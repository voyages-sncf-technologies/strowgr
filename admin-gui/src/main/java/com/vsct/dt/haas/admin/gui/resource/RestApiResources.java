package com.vsct.dt.haas.admin.gui.resource;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vsct.dt.haas.admin.core.*;
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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    private final PortProvider portProvider;
    private Map<String, Waiter> callbacks = new ConcurrentHashMap<>();
    private ScheduledExecutorService timeoutExecutor = Executors.newSingleThreadScheduledExecutor();
    private final TemplateLocator templateLocator;
    private final TemplateGenerator templateGenerator;

    public RestApiResources(EventBus eventBus, EntryPointRepository repository, PortProvider portProvider, TemplateLocator templateLocator, TemplateGenerator templateGenerator) {
        this.eventBus = eventBus;
        this.repository = repository;
        this.portProvider = portProvider;
        this.templateLocator = templateLocator;
        this.templateGenerator = templateGenerator;
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

        return new EntryPointConfigurationJsonRepresentation(configuration.orElseThrow(NotFoundException::new));
    }

    @GET
    @Path("/entrypoint/{id : .+}/pending")
    public EntryPointConfigurationJsonRepresentation getPending(@PathParam("id") String id) throws JsonProcessingException {
        Optional<EntryPointConfiguration> configuration = repository.getPendingConfiguration(new EntryPointKeyDefaultImpl(id));

        return new EntryPointConfigurationJsonRepresentation(configuration.orElseThrow(NotFoundException::new));
    }

    @GET
    @Path("/entrypoint/{id : .+}/committing")
    public EntryPointConfigurationJsonRepresentation getCommitting(@PathParam("id") String id) throws JsonProcessingException {
        Optional<EntryPointConfiguration> configuration = repository.getCommittingConfiguration(new EntryPointKeyDefaultImpl(id));

        return new EntryPointConfigurationJsonRepresentation(configuration.orElseThrow(NotFoundException::new));
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

    @GET
    @Path("/ports/{id : .+}")
    public String getPort(@PathParam("id") String id) {
        Optional<Integer> port = portProvider.getPort(id);
        if (port.isPresent())
            return String.valueOf(port.get());
        else throw new NotFoundException();
    }

    @GET
    @Path("/ports")
    public Map<String, Integer> getPorts() {
        return portProvider.getPorts().orElseGet(HashMap::new);
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
        RegisterServerEvent event = new RegisterServerEvent(CorrelationId.newCorrelationId(), new EntryPointKeyDefaultImpl(id), backend, Sets.newHashSet(serverJson));
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

    @PUT
    @Path("/entrypoint/{id : .+}/newport")
    public String setPort(@PathParam("id") String id) {
        return String.valueOf(portProvider.newPort(id));
    }

    private WaiterBuilder waitEventWithId(String eventId) {
        return new WaiterBuilder(eventId);
    }

    @GET
    @Path("/entrypoint/{id : .+}/current/template/haproxy")
    @Produces(MediaType.TEXT_PLAIN)
    public String getHaproxyTemplate(@PathParam("id") String id) throws IOException {
        Optional<EntryPointConfiguration> currentConfiguration = repository.getCurrentConfiguration(new EntryPointKeyDefaultImpl(id));
        if (currentConfiguration.isPresent()) {
            return templateLocator.readTemplate(currentConfiguration.get());
        }
        return "can't find haproxy template uri for entrypoint " + id;
    }

    @GET
    @Path("/entrypoint/{id : .+}/current/configuration/haproxy")
    @Produces(MediaType.TEXT_PLAIN)
    public String getHaproxyConfiguration(@PathParam("id") String id) throws IOException {
        EntryPointKeyDefaultImpl key = new EntryPointKeyDefaultImpl(id);
        return repository
                .getCurrentConfiguration(key)
                .orElseThrow(() -> new IllegalStateException("can't get configurtion for id " + id))
                .generateHaproxyConfiguration(key, templateLocator, templateGenerator, portProvider);
    }

    @GET
    @Path("/haproxy/uri/{haproxyName : .+}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getHaproxyURI(@PathParam("haproxyName") String haproxyName) throws IOException {
        return repository.getHaproxy(haproxyName).orElseThrow(() -> new RuntimeException("can't get haproxy uri of " + haproxyName));
    }

    @Subscribe
    public void handle(CommitBeginEvent event) {
        Waiter w = callbacks.get(event.getCorrelationId());
        if (w != null) {
            try {
                w.handle(event);
                callbacks.remove(event.getCorrelationId());
            } catch (Exception e) {
                LOGGER.error("can't handle CommitBeginEvent " + event, e);
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
                LOGGER.error("can't handle CommitCompleteEvent " + event, e);
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
                LOGGER.error("can't handle EntryPointAddedEvent " + event, e);
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
                LOGGER.error("can't handle ServerRegisteredEvent " + event, e);
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
