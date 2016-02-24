package com.vsct.dt.haas.admin.gui.resource;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vsct.dt.haas.admin.core.EntryPointKeyDefaultImpl;
import com.vsct.dt.haas.admin.core.EntryPointRepository;
import com.vsct.dt.haas.admin.core.configuration.EntryPointBackendServer;
import com.vsct.dt.haas.admin.core.configuration.EntryPointConfiguration;
import com.vsct.dt.haas.admin.core.event.CorrelationId;
import com.vsct.dt.haas.admin.core.event.in.*;
import com.vsct.dt.haas.admin.core.event.out.CommitBeginEvent;
import com.vsct.dt.haas.admin.core.event.out.CommitCompleteEvent;
import com.vsct.dt.haas.admin.core.event.out.EntryPointAddedEvent;
import com.vsct.dt.haas.admin.core.event.out.ServerRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RestApiResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestApiResources.class);

    private final EventBus eventBus;
    private final EntryPointRepository repository;
    private Map<String, Waiter> callbacks = new ConcurrentHashMap<>();
    private ScheduledExecutorService timeoutExecutor = Executors.newSingleThreadScheduledExecutor();

    public RestApiResources(EventBus eventBus, EntryPointRepository repository) {
        this.eventBus = eventBus;
        this.repository = repository;
    }

    @POST
    @Path("/entrypoint/{id : .+}")
    @Timed
    public void addEntryPoint(@Suspended AsyncResponse asyncResponse, @PathParam("id") String id, @Valid EntryPointConfigurationJsonRepresentation configuration) {
        LOGGER.info("Get all criteria");

        AddEntryPointEvent event = new AddEntryPointEvent(CorrelationId.newCorrelationId(), new EntryPointKeyDefaultImpl(id), configuration);

        waitEventWithId(event.getCorrelationId()).whenReceive(new WaiterCallBack() {
            @Override
            void handle(EntryPointAddedEvent event) {
                asyncResponse.resume(event.getConfiguration());
            }

            @Override
            void whenTimedOut() {
                asyncResponse.resume("Timeout");
            }
        }).timeoutAfter(10, TimeUnit.SECONDS);

        eventBus.post(event);
    }

    @GET
    @Path("/entrypoint/{id : .+}/current")
    public EntryPointConfigurationJsonRepresentation getCurrent(@PathParam("id") String id) throws JsonProcessingException {
        Optional<EntryPointConfiguration> configuration = repository.getCurrentConfiguration(new EntryPointKeyDefaultImpl(id));

        EntryPointConfigurationJsonRepresentation restEntity = new EntryPointConfigurationJsonRepresentation(configuration.orElseThrow(() -> new NotFoundException()));

        return restEntity;
    }

    @GET
    @Path("/entrypoint/{id : .+}/pending")
    public EntryPointConfigurationJsonRepresentation getPending(@PathParam("id") String id) throws JsonProcessingException {
        Optional<EntryPointConfiguration> configuration = repository.getPendingConfiguration(new EntryPointKeyDefaultImpl(id));

        EntryPointConfigurationJsonRepresentation restEntity = new EntryPointConfigurationJsonRepresentation(configuration.orElseThrow(() -> new NotFoundException()));

        return restEntity;
    }

    @GET
    @Path("/entrypoint/{id : .+}/committing")
    public EntryPointConfigurationJsonRepresentation getCommitting(@PathParam("id") String id) throws JsonProcessingException {
        Optional<EntryPointConfiguration> configuration = repository.getCommittingConfiguration(new EntryPointKeyDefaultImpl(id));

        EntryPointConfigurationJsonRepresentation restEntity = new EntryPointConfigurationJsonRepresentation(configuration.orElseThrow(() -> new NotFoundException()));

        return restEntity;
    }

    @GET
    @Path("/entrypoints")
    public Set<String> getEntryPoints() {
        return repository.getEntryPointsId();
    }

    @POST
    @Path("/entrypoint/{id : .+}/try-commit-pending")
    public void tryCommitPending(@Suspended AsyncResponse asyncResponse, @PathParam("id") String id) {
        TryCommitPendingConfigurationEvent event = new TryCommitPendingConfigurationEvent(CorrelationId.newCorrelationId(), new EntryPointKeyDefaultImpl(id));

        waitEventWithId(event.getCorrelationId()).whenReceive(
                new WaiterCallBack() {
                    @Override
                    void handle(CommitCompleteEvent event) {
                        asyncResponse.resume(event.getConfiguration());
                    }

                    @Override
                    void whenTimedOut() {
                        asyncResponse.resume("Timeout");
                    }
                }
        ).timeoutAfter(10, TimeUnit.SECONDS);

        eventBus.post(event);
    }

    /* DEBUGGING METHODS */
    @POST
    @Path("/entrypoint/{id : .+}/try-commit-current")
    public String tryCommitCurrent(@PathParam("id") String id) {
        TryCommitCurrentConfigurationEvent event = new TryCommitCurrentConfigurationEvent(CorrelationId.newCorrelationId(), new EntryPointKeyDefaultImpl(id));
        eventBus.post(event);
        return "Request posted, look info to follow actions";
    }

    @POST
    @Path("/entrypoint/{id : .+}/backend/{backend}/register-server")
    public String registerServer(@PathParam("id") String id,
                                 @PathParam("backend") String backend,
                                 EntryPointBackendServerJsonRepresentation serverJson) {
        EntryPointBackendServer server = (EntryPointBackendServer) serverJson;
        RegisterServerEvent event = new RegisterServerEvent(CorrelationId.newCorrelationId(), new EntryPointKeyDefaultImpl(id), backend, Sets.newHashSet(server));
        eventBus.post(event);
        return "Request posted, look info to follow actions";
    }

    @POST
    @Path("/entrypoint/{id : .+}/send-commit-success/{correlationId}")
    public String sendCommitSuccess(@PathParam("id") String id, @PathParam("correlationId") String correlationId) {
        CommitSuccessEvent event = new CommitSuccessEvent(correlationId, new EntryPointKeyDefaultImpl(id));
        eventBus.post(event);
        return "Request posted, look info to follow actions";
    }

    @POST
    @Path("/entrypoint/{id : .+}/send-commit-failure/{correlationId}")
    public String sendCommitFailure(@PathParam("id") String id, @PathParam("correlationId") String correlationId) {
        CommitFailureEvent event = new CommitFailureEvent(correlationId, new EntryPointKeyDefaultImpl(id));
        eventBus.post(event);
        return "Request posted, look info to follow actions";
    }

    @GET
    @Path("/entrypoint/{id : .+}/port")
    public String getPort(@PathParam("id") String id) {
        Optional<Integer> port = repository.getPort(id);
        if (port.isPresent())
            return String.valueOf(port.get());
        else return "port not found for entry point " + id;
    }

    @GET
    @Path("/ports")
    public Map<String, Integer> getPorts() {
        return repository.getPorts().orElseGet(HashMap::new);
    }

    @PUT
    @Path("/entrypoint/{id : .+}/newport")
    public String setPort(@PathParam("id") String id) {
        return String.valueOf(repository.newPort(id));
    }

    private <T> WaiterBuilder waitEventWithId(String eventId) {
        return new WaiterBuilder(eventId);
    }

    @Subscribe
    public void handle(CommitBeginEvent event) {
        Waiter w = callbacks.get(event.getCorrelationId());
        if (w != null) {
            try {
                w.handle(event);
                callbacks.remove(event.getCorrelationId());
            } catch (Exception e) {

            }
        }
    }

    @Subscribe
    public void handle(CommitCompleteEvent event) {
        Waiter w = callbacks.get(event.getCorrelationId());
        if (w != null) {
            try {
                w.handle(event);
                callbacks.remove(event.getCorrelationId());
            } catch (Exception e) {

            }
        }
    }

    @Subscribe
    public void handle(EntryPointAddedEvent event) {
        Waiter w = callbacks.get(event.getCorrelationId());
        if (w != null) {
            try {
                w.handle(event);
                callbacks.remove(event.getCorrelationId());
            } catch (Exception e) {

            }
        }
    }

    @Subscribe
    public void handle(ServerRegisteredEvent event) {
        Waiter w = callbacks.get(event.getCorrelationId());
        if (w != null) {
            try {
                w.handle(event);
                callbacks.remove(event.getCorrelationId());
            } catch (Exception e) {

            }
        }
    }

    private interface WaiterBuilderITimeout {
        void timeoutAfter(long delay, TimeUnit unit);
    }

    private static abstract class WaiterCallBack {

        Waiter waiter;

        void handle(CommitBeginEvent event) throws Exception {
            throw new Exception();
        }

        void handle(CommitCompleteEvent event) throws Exception {
            throw new Exception();
        }

        void handle(EntryPointAddedEvent event) throws Exception {
            throw new Exception();
        }

        void handle(ServerRegisteredEvent event) throws Exception {
            throw new Exception();
        }

        abstract void whenTimedOut();
    }

    private class WaiterBuilder implements WaiterBuilderITimeout {

        String eventId;
        WaiterCallBack callback;

        WaiterBuilder(String eventId) {
            this.eventId = eventId;
        }

        WaiterBuilderITimeout whenReceive(WaiterCallBack callback) {
            this.callback = callback;
            return this;
        }

        public void timeoutAfter(long delay, TimeUnit unit) {
            timeoutExecutor.schedule(() -> {
                callback.whenTimedOut();
                callbacks.remove(eventId);
            }, delay, unit);

            Waiter w = new Waiter(eventId, callback);
            callbacks.put(eventId, w);
        }
    }

    private class Waiter {

        String eventId;
        WaiterCallBack callback;

        Waiter(String eventId, WaiterCallBack callback) {
            this.eventId = eventId;
            this.callback = callback;
            this.callback.waiter = this;
        }

        void handle(CommitBeginEvent event) throws Exception {
            callback.handle(event);
        }

        void handle(CommitCompleteEvent event) throws Exception {
            callback.handle(event);
        }

        void handle(EntryPointAddedEvent event) throws Exception {
            callback.handle(event);
        }

        void handle(ServerRegisteredEvent event) throws Exception {
            callback.handle(event);
        }
    }

}
