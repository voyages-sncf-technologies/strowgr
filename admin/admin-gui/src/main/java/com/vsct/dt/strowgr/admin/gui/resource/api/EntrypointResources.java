package com.vsct.dt.strowgr.admin.gui.resource.api;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vsct.dt.strowgr.admin.core.EntryPointKeyDefaultImpl;
import com.vsct.dt.strowgr.admin.core.configuration.EntryPoint;
import com.vsct.dt.strowgr.admin.core.event.CorrelationId;
import com.vsct.dt.strowgr.admin.core.event.in.*;
import com.vsct.dt.strowgr.admin.core.event.out.*;
import com.vsct.dt.strowgr.admin.gui.mapping.json.EntryPointMappingJson;
import com.vsct.dt.strowgr.admin.gui.mapping.json.UpdatedEntryPointMappingJson;
import com.vsct.dt.strowgr.admin.gui.resource.IncomingEntryPointBackendServerJsonRepresentation;
import com.vsct.dt.strowgr.admin.core.EntryPointRepository;
import io.dropwizard.jersey.PATCH;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Path("/entrypoints")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
@Consumes(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class EntrypointResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntrypointResources.class);

    private final EventBus             eventBus;
    private final EntryPointRepository repository;
    private Map<String, AsyncResponseCallback> callbacks       = new ConcurrentHashMap<>();
    private ScheduledExecutorService           timeoutExecutor = Executors.newSingleThreadScheduledExecutor();

    public EntrypointResources(EventBus eventBus, EntryPointRepository repository) {
        this.eventBus = eventBus;
        this.repository = repository;
    }

    @GET
    @Timed
    public Set<String> getEntryPoints() {
        return repository.getEntryPointsId();
    }

    @PUT
    @Path("/{id : .+}")
    @Timed
    public void addEntryPoint(@Suspended AsyncResponse asyncResponse, @PathParam("id") String id, @Valid EntryPointMappingJson configuration) {
        AddEntryPointEvent event = new AddEntryPointEvent(CorrelationId.newCorrelationId(), new EntryPointKeyDefaultImpl(id), configuration);

        new CallbackBuilder(event.getCorrelationId())
                .whenReceive(new AsyncResponseCallback<EntryPointAddedEvent>(asyncResponse) {
                    @Override
                    void handle(EntryPointAddedEvent event) throws Exception {
                        asyncResponse.resume(Response.status(Response.Status.CREATED).entity(event.getConfiguration().get()).build());
                    }
                })
                .timeoutAfter(10, TimeUnit.SECONDS);

        eventBus.post(event);
    }

    @PATCH
    @Path("/{id : .+}")
    @Timed
    public void updateEntryPoint(@Suspended AsyncResponse asyncResponse, @PathParam("id") String id, @Valid UpdatedEntryPointMappingJson updatedConfiguration) {
        UpdateEntryPointEvent event = new UpdateEntryPointEvent(CorrelationId.newCorrelationId(), new EntryPointKeyDefaultImpl(id), updatedConfiguration);

        new CallbackBuilder(event.getCorrelationId())
                .whenReceive(new AsyncResponseCallback<EntryPointUpdatedEvent>(asyncResponse) {
                    @Override
                    void handle(EntryPointUpdatedEvent event) throws Exception {
                        asyncResponse.resume(event.getConfiguration().get());
                    }
                })
                .timeoutAfter(10, TimeUnit.SECONDS);

        eventBus.post(event);
    }

    @GET
    @Path("/{id : .+}/current")
    @Timed
    public EntryPointMappingJson getCurrent(@PathParam("id") String id) throws JsonProcessingException {
        Optional<EntryPoint> configuration = repository.getCurrentConfiguration(new EntryPointKeyDefaultImpl(id));

        return new EntryPointMappingJson(configuration.orElseThrow(NotFoundException::new));
    }

    @GET
    @Path("/{id : .+}/pending")
    @Timed
    public EntryPointMappingJson getPending(@PathParam("id") String id) throws JsonProcessingException {
        Optional<EntryPoint> configuration = repository.getPendingConfiguration(new EntryPointKeyDefaultImpl(id));

        return new EntryPointMappingJson(configuration.orElseThrow(NotFoundException::new));
    }

    @GET
    @Path("/{id : .+}/committing")
    @Timed
    public EntryPointMappingJson getCommitting(@PathParam("id") String id) throws JsonProcessingException {
        Optional<EntryPoint> configuration = repository.getCommittingConfiguration(new EntryPointKeyDefaultImpl(id));

        return new EntryPointMappingJson(configuration.orElseThrow(NotFoundException::new));
    }

    /* DEBUGGING METHODS */
    @POST
    @Path("/{id : .+}/try-commit-current")
    @Produces(MediaType.TEXT_PLAIN)
    public String tryCommitCurrent(@PathParam("id") String id) {
        TryCommitCurrentConfigurationEvent event = new TryCommitCurrentConfigurationEvent(CorrelationId.newCorrelationId(), new EntryPointKeyDefaultImpl(id));
        eventBus.post(event);
        return "Request posted, look info to follow actions";
    }

    @POST
    @Path("/{id : .+}/try-commit-pending")
    @Produces(MediaType.TEXT_PLAIN)
    public void tryCommitPending(@Suspended AsyncResponse asyncResponse, @PathParam("id") String id) {
        TryCommitPendingConfigurationEvent event = new TryCommitPendingConfigurationEvent(CorrelationId.newCorrelationId(), new EntryPointKeyDefaultImpl(id));

        new CallbackBuilder(event.getCorrelationId()).whenReceive(
                new AsyncResponseCallback<CommitBeginEvent>(asyncResponse) {
                    @Override
                    void handle(CommitBeginEvent event) throws Exception {
                        asyncResponse.resume(event.getConfiguration());
                    }
                }).timeoutAfter(10, TimeUnit.SECONDS);

        eventBus.post(event);
    }

    @POST
    @Path("/{id : .+}/backend/{backend}/register-server")
    @Produces(MediaType.TEXT_PLAIN)
    public String registerServer(@PathParam("id") String id,
                                 @PathParam("backend") String backend,
                                 IncomingEntryPointBackendServerJsonRepresentation serverJson) {
        RegisterServerEvent event = new RegisterServerEvent(CorrelationId.newCorrelationId(), new EntryPointKeyDefaultImpl(id), backend, Sets.newHashSet(serverJson));
        LOGGER.debug("receive RegisterServerEvent {} for key {} and backend {}", event, id, backend);
        eventBus.post(event);
        return "Request posted, look info to follow actions";
    }

    @POST
    @Path("/{id : .+}/send-commit-success/{correlationId}")
    @Produces(MediaType.TEXT_PLAIN)
    public String sendCommitSuccess(@PathParam("id") String id, @PathParam("correlationId") String correlationId) {
        CommitSuccessEvent event = new CommitSuccessEvent(correlationId, new EntryPointKeyDefaultImpl(id));
        eventBus.post(event);
        return "Request posted, look info to follow actions";
    }

    @POST
    @Path("/{id : .+}/send-commit-failure/{correlationId}")
    @Produces(MediaType.TEXT_PLAIN)
    public String sendCommitFailure(@PathParam("id") String id, @PathParam("correlationId") String correlationId) {
        CommitFailureEvent event = new CommitFailureEvent(correlationId, new EntryPointKeyDefaultImpl(id));
        eventBus.post(event);
        return "Request posted, look info to follow actions";
    }

    private void handleWithCorrelationId(EntryPointEvent event) {
        try {
            LOGGER.trace("remove entrypoint {}", event);
            AsyncResponseCallback asyncResponseCallback = callbacks.remove(event.getCorrelationId());
            if (asyncResponseCallback != null) {
                asyncResponseCallback.handle(event);
            } else {
                LOGGER.debug("can't find callback for async response of the event {}", event);
            }
        } catch (Exception e) {
            LOGGER.error("can't handle EntryPointEvent " + event, e);
        }
    }

    @Subscribe
    public void handle(CommitCompleteEvent commitCompleteEvent) {
        handleWithCorrelationId(commitCompleteEvent);
    }

    @Subscribe
    public void handle(EntryPointAddedEvent entryPointAddedEvent) {
        handleWithCorrelationId(entryPointAddedEvent);
    }

    @Subscribe
    public void handle(EntryPointUpdatedEvent entryPointUpdatedEvent) {
        handleWithCorrelationId(entryPointUpdatedEvent);
    }

    @Subscribe
    public void handle(ServerRegisteredEvent serverRegisteredEvent) {
        handleWithCorrelationId(serverRegisteredEvent);
    }


    private abstract class AsyncResponseCallback<T> {
        private final AsyncResponse asyncResponse;

        protected AsyncResponseCallback(AsyncResponse asyncResponse) {
            this.asyncResponse = asyncResponse;
        }

        abstract void handle(T event) throws Exception;

        public void whenTimedOut() {
            asyncResponse.resume(Response.status(Response.Status.GATEWAY_TIMEOUT).build());
        }
    }

    private class CallbackBuilder {

        String                eventId;
        AsyncResponseCallback callback;

        CallbackBuilder(String eventId) {
            this.eventId = eventId;
        }

        CallbackBuilder whenReceive(AsyncResponseCallback callback) {
            this.callback = callback;
            return this;
        }

        public void timeoutAfter(long delay, TimeUnit unit) {
            timeoutExecutor.schedule(() -> {
                callback.whenTimedOut();
                callbacks.remove(eventId);
                LOGGER.trace("timeout reached, remove event {} from callbacks", eventId);
            }, delay, unit);

            callbacks.put(eventId, callback);
        }
    }

}
