/*
 * Copyright (C) 2016 VSCT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vsct.dt.strowgr.admin.gui.resource.api;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Sets;
import com.vsct.dt.strowgr.admin.core.EntryPointKey;
import com.vsct.dt.strowgr.admin.core.EntryPointKeyDefaultImpl;
import com.vsct.dt.strowgr.admin.core.configuration.EntryPoint;
import com.vsct.dt.strowgr.admin.core.entrypoint.*;
import com.vsct.dt.strowgr.admin.core.event.CorrelationId;
import com.vsct.dt.strowgr.admin.core.event.in.*;
import com.vsct.dt.strowgr.admin.core.event.out.DeleteEntryPointEvent;
import com.vsct.dt.strowgr.admin.core.repository.EntryPointRepository;
import com.vsct.dt.strowgr.admin.core.security.model.User;
import com.vsct.dt.strowgr.admin.gui.mapping.json.EntryPointMappingJson;
import com.vsct.dt.strowgr.admin.gui.mapping.json.UpdatedEntryPointMappingJson;
import com.vsct.dt.strowgr.admin.gui.resource.IncomingEntryPointBackendServerJsonRepresentation;
import com.vsct.dt.strowgr.admin.nsq.consumer.EntryPointKeyVsctImpl;

import io.dropwizard.auth.Auth;
import io.dropwizard.jersey.PATCH;
import org.reactivestreams.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static javax.ws.rs.core.Response.Status.*;
import static javax.ws.rs.core.Response.*;

@Path("/entrypoints")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
@Consumes(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class EntryPointResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntryPointResources.class);

    private final EntryPointRepository repository;

    private final Subscriber<AutoReloadConfigEvent> autoReloadConfigSubscriber;

    private final Subscriber<AddEntryPointEvent> addEntryPointSubscriber;

    private final Subscriber<UpdateEntryPointEvent> updateEntryPointSubscriber;

    private final Subscriber<DeleteEntryPointEvent> deleteEntryPointSubscriber;

    private final Subscriber<TryCommitPendingConfigurationEvent> tryCommitPendingConfigurationSubscriber;

    private final Subscriber<TryCommitCurrentConfigurationEvent> tryCommitCurrentConfigurationSubscriber;

    private final Subscriber<RegisterServerEvent> registerServerSubscriber;

    private final Subscriber<CommitCompletedEvent> commitSuccessSubscriber;

    private final Subscriber<CommitFailedEvent> commitFailureSubscriber;

    public EntryPointResources(EntryPointRepository repository,
                               Subscriber<AutoReloadConfigEvent> autoReloadConfigSubscriber,
                               Subscriber<AddEntryPointEvent> addEntryPointSubscriber,
                               Subscriber<UpdateEntryPointEvent> updateEntryPointSubscriber,
                               Subscriber<DeleteEntryPointEvent> deleteEntryPointSubscriber,
                               Subscriber<TryCommitPendingConfigurationEvent> tryCommitPendingConfigurationSubscriber,
                               Subscriber<TryCommitCurrentConfigurationEvent> tryCommitCurrentConfigurationSubscriber,
                               Subscriber<RegisterServerEvent> registerServerSubscriber,
                               Subscriber<CommitCompletedEvent> commitSuccessSubscriber,
                               Subscriber<CommitFailedEvent> commitFailureSubscriber) {
        this.repository = repository;
        this.autoReloadConfigSubscriber = autoReloadConfigSubscriber;
        this.addEntryPointSubscriber = addEntryPointSubscriber;
        this.updateEntryPointSubscriber = updateEntryPointSubscriber;
        this.deleteEntryPointSubscriber = deleteEntryPointSubscriber;
        this.tryCommitPendingConfigurationSubscriber = tryCommitPendingConfigurationSubscriber;
        this.tryCommitCurrentConfigurationSubscriber = tryCommitCurrentConfigurationSubscriber;
        this.registerServerSubscriber = registerServerSubscriber;
        this.commitSuccessSubscriber = commitSuccessSubscriber;
        this.commitFailureSubscriber = commitFailureSubscriber;
    }

    @GET
    @Timed
    public Set<String> getEntryPoints(@Auth final User user) {
        return repository.getEntryPointsId();
    }

    @GET
    @Path("/{id : .+}/autoreload")
    public Boolean isAutoreloaded(@Auth final User user, @PathParam("id") String entryPointKey) {
        return repository.isAutoreloaded(new EntryPointKeyDefaultImpl(entryPointKey));
    }

    @PATCH
    @Path("/{id : .+}/autoreload/swap")
    public void swapAutoReload(@Auth final User user, @Suspended AsyncResponse asyncResponse, @PathParam("id") String key) {

        EntryPointKey entryPointKey = new EntryPointKeyDefaultImpl(key);

        autoReloadConfigSubscriber.onNext(new AutoReloadConfigEvent(CorrelationId.newCorrelationId(), entryPointKey) {
            @Override
            public void onSuccess(AutoReloadConfigResponse autoReloadResponse) {
                asyncResponse.resume(status(PARTIAL_CONTENT).build());
            }

            @Override
            public void onError(Throwable throwable) {
                asyncResponse.resume(throwable);
            }
        });

    }

    @PUT
    @Path("/{id : .+}")
    @Timed
    public void addEntryPoint(@Auth final User user, @Suspended AsyncResponse asyncResponse, @PathParam("id") String key, @Valid EntryPointMappingJson configuration) {

        EntryPointKey entryPointKey = new EntryPointKeyDefaultImpl(key);

        addEntryPointSubscriber.onNext(new AddEntryPointEvent(CorrelationId.newCorrelationId(), entryPointKey, configuration) {
            @Override
            public void onSuccess(AddEntryPointResponse addEntryPointResponse) {
                asyncResponse.resume(status(CREATED).entity(addEntryPointResponse.getConfiguration()).build());
            }

            @Override
            public void onError(Throwable throwable) {
                asyncResponse.resume(throwable);
            }
        });

    }

    /**
     * Update an entrypoint.
     *
     * @param asyncResponse        response asynchronously
     * @param id                   of the entrypoint (ex. PAO/REL1)
     * @param updatedConfiguration deserialized entrypoint configuration with new values
     */
    @PATCH
    @Path("/{id : .+}")
    @Timed
    public void updateEntryPoint(@Auth final User user, @Suspended AsyncResponse asyncResponse, @PathParam("id") String id, @Valid UpdatedEntryPointMappingJson updatedConfiguration) {

        updateEntryPointSubscriber.onNext(new UpdateEntryPointEvent(CorrelationId.newCorrelationId(), new EntryPointKeyDefaultImpl(id), updatedConfiguration) {
            @Override
            public void onSuccess(UpdateEntryPointResponse updateEntryPointResponse) {
                asyncResponse.resume(updateEntryPointResponse.getConfiguration());
            }

            @Override
            public void onError(Throwable throwable) {
                asyncResponse.resume(throwable);
            }
        });

    }

    @GET
    @Path("/{id : .+}/current")
    @Timed
    public EntryPointMappingJson getCurrent(@Auth final User user, @PathParam("id") String id) throws JsonProcessingException {
        Optional<EntryPoint> configuration = repository.getCurrentConfiguration(new EntryPointKeyDefaultImpl(id));

        return new EntryPointMappingJson(configuration.orElseThrow(NotFoundException::new));
    }


    /**
     * Delete an entrypoint in the repository and, after a success confirmation, forwards the event to other strowgr component through the event bus.
     *
     * @param id of the entrypoint
     * @return {@link javax.ws.rs.core.Response.Status#OK} if success, {@link javax.ws.rs.core.Response.Status#NOT_FOUND} if entrypoint doesn't exists anymore, {@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} otherwise. HTTP response payload contains the updated entrypoint set.
     */
    @DELETE
    @Path("/{id : .+}")
    @Timed
    public Response deleteEntrypoint(@Auth final User user, @PathParam("id") String id) {
        final Response response;

        // first get the current entrypoint configuration, if exists
        EntryPointKeyVsctImpl entryPointKey = EntryPointKeyVsctImpl.fromID(id);
        Optional<EntryPoint> configuration = repository.getCurrentConfiguration(entryPointKey);

        if (configuration.isPresent()) {
            // secondly remove the entrypoint from repository
            Optional<Boolean> removed = repository.removeEntrypoint(entryPointKey);
            if (removed.isPresent()) {
                if (removed.get()) {
                    Set<String> entryPointsId = repository.getEntryPointsId();
                    // entrypoint removed without problem
                    response = ok().entity(entryPointsId).build();
                    // thirdly, propagate the deleted event to other strowgr components
                    deleteEntryPointSubscriber.onNext(new DeleteEntryPointEvent(UUID.randomUUID().toString(), entryPointKey, configuration.get().getHaproxy(), entryPointKey.getApplication(), entryPointKey.getPlatform()));
                } else {
                    Set<String> entryPointsId = repository.getEntryPointsId();
                    LOGGER.warn("can't removed an entrypoint though its configuration has just been found. May be there are concurrency problem, admin or/and repository are overloaded.");
                    response = status(NOT_FOUND).entity(entryPointsId).build();
                }
            } else {
                Set<String> entryPointsId = repository.getEntryPointsId();
                response = serverError().entity(entryPointsId).build();
            }
        } else {
            Set<String> entryPointsId = repository.getEntryPointsId();
            LOGGER.warn("can't find entrypoint {} from repository", entryPointKey);
            response = status(NOT_FOUND).entity(entryPointsId).build();
        }
        return response;
    }

    @GET
    @Path("/{id : .+}/pending")
    @Timed
    public EntryPointMappingJson getPending(@Auth final User user, @PathParam("id") String id) throws JsonProcessingException {
        Optional<EntryPoint> configuration = repository.getPendingConfiguration(new EntryPointKeyDefaultImpl(id));

        return new EntryPointMappingJson(configuration.orElseThrow(NotFoundException::new));
    }

    @GET
    @Path("/{id : .+}/committing")
    @Timed
    public EntryPointMappingJson getCommitting(@Auth final User user, @PathParam("id") String id) throws JsonProcessingException {
        Optional<EntryPoint> configuration = repository.getCommittingConfiguration(new EntryPointKeyDefaultImpl(id));

        return new EntryPointMappingJson(configuration.orElseThrow(NotFoundException::new));
    }

    /* DEBUGGING METHODS */
    @POST
    @Path("/{id : .+}/try-commit-current")
    @Produces(MediaType.TEXT_PLAIN)
    public String tryCommitCurrent(@Auth final User user, @PathParam("id") String id) {
        TryCommitCurrentConfigurationEvent event = new TryCommitCurrentConfigurationEvent(CorrelationId.newCorrelationId(), new EntryPointKeyDefaultImpl(id));

        tryCommitCurrentConfigurationSubscriber.onNext(event);

        return "Request posted, look info to follow actions";
    }

    @POST
    @Path("/{id : .+}/try-commit-pending")
    @Produces(MediaType.TEXT_PLAIN)
    public String tryCommitPending(@Auth final User user, @PathParam("id") String id) {
        TryCommitPendingConfigurationEvent event = new TryCommitPendingConfigurationEvent(CorrelationId.newCorrelationId(), new EntryPointKeyDefaultImpl(id));

        tryCommitPendingConfigurationSubscriber.onNext(event);

        return "Request posted, look info to follow actions";
    }

    @POST
    @Path("/{id : .+}/backend/{backend}/register-server")
    @Produces(MediaType.TEXT_PLAIN)
    public String registerServer( @Auth final User user, 
    							@PathParam("id") String id,
                                 @PathParam("backend") String backend,
                                 IncomingEntryPointBackendServerJsonRepresentation serverJson) {
        RegisterServerEvent event = new RegisterServerEvent(CorrelationId.newCorrelationId(), new EntryPointKeyDefaultImpl(id), backend, Sets.newHashSet(serverJson));
        LOGGER.debug("receive RegisterServerEvent {} for key {} and backend {}", event, id, backend);
        registerServerSubscriber.onNext(event);
        return "Request posted, look info to follow actions";
    }

    @POST
    @Path("/{id : .+}/send-commit-success/{correlationId}")
    @Produces(MediaType.TEXT_PLAIN)
    public String sendCommitSuccess(@Auth final User user, 
    									@PathParam("id") String id, @PathParam("correlationId") String correlationId) {
        CommitCompletedEvent event = new CommitCompletedEvent(correlationId, new EntryPointKeyDefaultImpl(id));
        commitSuccessSubscriber.onNext(event);
        return "Request posted, look info to follow actions";
    }

    @POST
    @Path("/{id : .+}/send-commit-failure/{correlationId}")
    @Produces(MediaType.TEXT_PLAIN)
    public String sendCommitFailure(@Auth final User user,
    									@PathParam("id") String id, @PathParam("correlationId") String correlationId) {
        CommitFailedEvent event = new CommitFailedEvent(correlationId, new EntryPointKeyDefaultImpl(id));
        commitFailureSubscriber.onNext(event);
        return "Request posted, look info to follow actions";
    }

}
