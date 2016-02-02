package com.vsct.dt.haas.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.brainlag.nsq.NSQProducer;
import com.github.brainlag.nsq.exceptions.NSQException;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.common.eventbus.Subscribe;
import com.vsct.dt.haas.events.*;
import com.vsct.dt.haas.nsq.AddNewEntryPointPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

/**
 * Created by william_montaz on 02/02/2016.
 */
public class AdminState {

    Logger LOGGER = LoggerFactory.getLogger(AdminState.class);

    private static final String HAP_NAME = "default-name";
    ClassLoader classLoader = getClass().getClassLoader();
    File file = new File(classLoader.getResource("template.mustache").getFile());

    private Map<String, EntryPoint> entryPoints = new HashMap<>();
    private Map<String, EntryPoint> pendingEntryPoints = new HashMap<>();
    private Map<String, EntryPoint> commitingEntryPoints = new HashMap<>();

    NSQProducer producer = new NSQProducer().addAddress("floradora", 50150).start();

    ObjectMapper objectMapper = new ObjectMapper();

    public String generateTemplate(EntryPoint entryPoint) throws IOException {
        Writer writer = new StringWriter();
        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile(new FileReader(file), "no_cache");

        HashMap<String, Object> scope = entryPoint.toMustacheScope();

        mustache.execute(writer, scope);
        return writer.toString();
    }

    @Subscribe
    public void addNewEntryPoint(AddNewEntryPointEvent event) throws IOException, NSQException, TimeoutException {
        Optional<EntryPoint> entryPointOptional = this.getEntryPoint(event.getEntryPoint().getApplication(), event.getEntryPoint().getPlatform());

        if (!entryPointOptional.isPresent()) {
            this.putEntryPoint(event.getEntryPoint());

            String hapconf = generateTemplate(event.getEntryPoint());
            AddNewEntryPointPayload addNewEntryPointPayload = new AddNewEntryPointPayload();
            addNewEntryPointPayload.application = event.getEntryPoint().getApplication();
            addNewEntryPointPayload.platform = event.getEntryPoint().getPlatform();
            addNewEntryPointPayload.conf = Base64.getEncoder().encode(hapconf.getBytes());

            producer.produce("new_entrypoint_" + HAP_NAME, objectMapper.writeValueAsBytes(addNewEntryPointPayload));

        } else {
            /* TODO, if failed we allow to recreate it */
            LOGGER.error("AddNewEntryPoint - ERROR - Already an entrypoint for " + event.getEntryPoint().getApplication() + event.getEntryPoint().getPlatform());
        }
    }

    @Subscribe
    public void entryPointDeployed(EntryPointDeployedEvent event) {

        Optional<EntryPoint> entryPointOptional = this.getEntryPoint(event.getApplication(), event.getPlatform());

        if (entryPointOptional.isPresent()) {

            EntryPoint entryPoint = entryPointOptional.get();

            if (entryPoint.getStatus().equals(EntryPointStatus.DEPLOYED)) {
                LOGGER.error("EntryPointDeployed - ERROR - Entrypoint for " + event.getApplication() + event.getPlatform() + " is already deployed");
                return;
            } else if (entryPoint.getStatus().equals(EntryPointStatus.FAILED)) {
                LOGGER.error("EntryPointDeployed - ERROR - Entrypoint for " + event.getApplication() + event.getPlatform() + " was failed");
                return;
            }

            this.putEntryPoint(entryPoint.changeStatus(EntryPointStatus.DEPLOYED));

        } else {
            LOGGER.error("EntryPointDeployed - ERROR - There is no entrypoint for " + event.getApplication() + event.getPlatform());
        }

    }

    @Subscribe
    public void addNewServer(AddNewServerEvent event) {

        Optional<EntryPoint> entryPointOptional = this.getEntryPoint(event.getApplication(), event.getPlatform());
        if (entryPointOptional.isPresent()) {

            EntryPoint entryPoint = entryPointOptional.get();
            if (entryPoint.getStatus().equals(EntryPointStatus.DEPLOYED)) {

                EntryPoint pendingEntryPoint = getPendingEntryPoint(event.getApplication(), event.getPlatform())
                        .map(ep -> ep.addServer(event.getBackendName(), event.getServer())) /* TODO Implement merging strategy, for now, we just replace the server, not working with context provided by user */
                        .orElse(entryPoint);
                this.putPendingEntryPoint(pendingEntryPoint);


            } else {
                LOGGER.error("AddNewServer - ERROR - Entrypoint for " + event.getApplication() + event.getPlatform() + " is not deployed");
            }

        } else {
            LOGGER.error("AddNewServer - ERROR - There is no entrypoint for " + event.getApplication() + event.getPlatform());
        }

    }

    @Subscribe
    public void updateEntryPoint(UpdateEntryPointEvent event) throws IOException, NSQException, TimeoutException {

        Optional<EntryPoint> entryPointOptional = this.getEntryPoint(event.getApplication(), event.getPlatform());
        if (entryPointOptional.isPresent()) {

            EntryPoint entryPoint = entryPointOptional.get();
            if (entryPoint.getStatus().equals(EntryPointStatus.DEPLOYED)) {

                Optional<EntryPoint> pendingEntryPoint = getPendingEntryPoint(event.getApplication(), event.getPlatform());
                if (pendingEntryPoint.isPresent()) {

                    Optional<EntryPoint> commitingEntryPointOptional = getCommitingEntryPoint(event.getApplication(), event.getPlatform());
                    if (commitingEntryPointOptional.isPresent()) {
                        System.out.println("UpdateEntryPoint - INFO - Entrypoint for " + event.getApplication() + event.getPlatform() + " needs no deployment because it is already commiting");
                    } else {

                        EntryPoint commitingEntryPoint = pendingEntryPoint.get();
                        this.putCommitingEntryPoint(commitingEntryPoint);

                        this.removePendingEntryPoint(event.getApplication(), event.getPlatform());

                        /* TODO generate HAP conf and send addNewEntryPointPayload to NSQ */
                        String hapconf = generateTemplate(commitingEntryPoint);
                        AddNewEntryPointPayload addNewEntryPointPayload = new AddNewEntryPointPayload();
                        addNewEntryPointPayload.application = event.getApplication();
                        addNewEntryPointPayload.platform = event.getPlatform();
                        addNewEntryPointPayload.conf = Base64.getEncoder().encode(hapconf.getBytes());

                        producer.produce("try_update_" + HAP_NAME, objectMapper.writeValueAsBytes(addNewEntryPointPayload));

                    }

                } else {
                    LOGGER.info("UpdateEntryPoint - INFO - Entrypoint for " + event.getApplication() + event.getPlatform() + " needs no deployment because nothing is pending");
                }

            } else {
                LOGGER.error("UpdateEntryPoint - ERROR - Entrypoint for " + event.getApplication() + event.getPlatform() + " is not deployed");
            }

        } else {
            LOGGER.error("UpdateEntryPoint - ERROR - There is no entrypoint for " + event.getApplication() + event.getPlatform());
        }

    }

    @Subscribe public void commitedEntryPoint(CommitedEntryPointEvent event){

        Optional<EntryPoint> entryPointOptional = this.getEntryPoint(event.getApplication(), event.getPlatform());
        if (entryPointOptional.isPresent()) {

            EntryPoint entryPoint = entryPointOptional.get();
            if (entryPoint.getStatus().equals(EntryPointStatus.DEPLOYED)) {

                Optional<EntryPoint> commitingEntryPointOptional = getCommitingEntryPoint(event.getApplication(), event.getPlatform());
                if(commitingEntryPointOptional.isPresent()){

                    EntryPoint commitingEntryPoint = commitingEntryPointOptional.get();

                    this.putEntryPoint(commitingEntryPoint);

                    this.removeCommitingEntryPoint(event.getApplication(), event.getPlatform());

                } else {
                    LOGGER.error("CommitedEntryPoint - ERROR - There was no commiting entrypoint for " + event.getApplication() + event.getPlatform());
                }
            } else {
                LOGGER.error("CommitedEntryPoint - ERROR - Entrypoint for " + event.getApplication() + event.getPlatform() + " is not deployed");
            }
        } else {
            LOGGER.error("CommitedEntryPoint - ERROR - There is no entrypoint for " + event.getApplication() + event.getPlatform());
        }

    }

    public void putEntryPoint(EntryPoint entryPoint) {
        this.entryPoints.put(entryPoint.getApplication() + entryPoint.getPlatform(), entryPoint);
    }

    public void putPendingEntryPoint(EntryPoint pendingEntryPoint) {
        this.pendingEntryPoints.put(pendingEntryPoint.getApplication() + pendingEntryPoint.getPlatform(), pendingEntryPoint);
    }

    public void putCommitingEntryPoint(EntryPoint commitingEntryPoint) {
        this.commitingEntryPoints.put(commitingEntryPoint.getApplication() + commitingEntryPoint.getPlatform(), commitingEntryPoint);
    }

    public void removePendingEntryPoint(String application, String platform) {
        this.pendingEntryPoints.remove(application + platform);
    }

    public void removeCommitingEntryPoint(String application, String platform) {
        this.commitingEntryPoints.remove(application + platform);
    }

    public Optional<EntryPoint> getEntryPoint(String application, String platform) {
        return Optional.ofNullable(entryPoints.get(application + platform));
    }

    public Optional<EntryPoint> getPendingEntryPoint(String application, String platform) {
        return Optional.ofNullable(pendingEntryPoints.get(application + platform));
    }

    public Optional<EntryPoint> getCommitingEntryPoint(String application, String platform) {
        return Optional.ofNullable(commitingEntryPoints.get(application + platform));
    }
}
