package com.vsct.dt.haas;

import com.google.common.eventbus.EventBus;
import com.vsct.dt.haas.events.AddNewEntryPointEvent;
import com.vsct.dt.haas.events.AddNewServerEvent;
import com.vsct.dt.haas.events.CommitedEntryPointEvent;
import com.vsct.dt.haas.events.UpdateEntryPointEvent;
import com.vsct.dt.haas.state.*;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Created by william_montaz on 02/02/2016.
 */
public class ScenarioAddServer {


    EventBus eventBus = new EventBus();
    AdminState adminState;

    @Before
    public void setUp(){
        adminState = new AdminState();
        eventBus.register(adminState);
    }

    @Test
    public void scenario_add_new_server_should_add_pending_ep_when_ep_is_deployed(){

        EntryPoint entryPoint = new EntryPoint("default-name", "OCE", "REC1", "hapocer1", "54250", EntryPointStatus.DEPLOYED);
        adminState.putEntryPoint(entryPoint);

        Server server = new Server("instance_name", "server_name", "ip", "port");

        AddNewServerEvent addNewServerEvent = new AddNewServerEvent("OCE", "REC1", "BACKEND", server);

        eventBus.post(addNewServerEvent);

        Optional<EntryPoint> ep = adminState.getPendingEntryPoint("OCE", "REC1");
        assertThat(ep.isPresent()).isTrue();
    }

    @Test
    public void scenario_add_new_server_should_do_nothing_when_ep_is_not_deployed(){

        EntryPoint entryPoint = new EntryPoint("default-name", "OCE", "REC1", "hapocer1", "54250", EntryPointStatus.DEPLOYING);
        adminState.putEntryPoint(entryPoint);

        Server server = new Server("instance_name", "server_name", "ip", "port");

        AddNewServerEvent addNewServerEvent = new AddNewServerEvent("OCE", "REC1", "BACKEND", server);

        eventBus.post(addNewServerEvent);

        Optional<EntryPoint> ep = adminState.getPendingEntryPoint("OCE", "REC1");
        assertThat(ep.isPresent()).isFalse();
    }

    @Test
    public void scenario_update_entry_point_should_put_pending_in_commiting(){

        EntryPoint entryPoint = new EntryPoint("default-name", "OCE", "REC1", "hapocer1", "54250", EntryPointStatus.DEPLOYED);
        adminState.putEntryPoint(entryPoint);
        adminState.putPendingEntryPoint(entryPoint);

        UpdateEntryPointEvent updateEntryPointEvent = new UpdateEntryPointEvent("OCE", "REC1");

        eventBus.post(updateEntryPointEvent);

        Optional<EntryPoint> ep = adminState.getPendingEntryPoint("OCE", "REC1");
        assertThat(ep.isPresent()).isFalse();

        ep = adminState.getCommitingEntryPoint("OCE", "REC1");
        assertThat(ep.isPresent()).isTrue();

    }

    @Test
    public void scenario_updated_entry_point_should_remove_from_commiting_and_replace_actual_by_commited(){
        EntryPoint entryPoint = new EntryPoint("default-name", "OCE", "REC1", "hapocer1", "54250", EntryPointStatus.DEPLOYED);
        adminState.putEntryPoint(entryPoint);
        adminState.putCommitingEntryPoint(entryPoint);

        CommitedEntryPointEvent commitedEntryPointEvent = new CommitedEntryPointEvent("OCE", "REC1");

        eventBus.post(commitedEntryPointEvent);

        Optional<EntryPoint> ep = adminState.getCommitingEntryPoint("OCE", "REC1");
        assertThat(ep.isPresent()).isFalse();

    }



}
