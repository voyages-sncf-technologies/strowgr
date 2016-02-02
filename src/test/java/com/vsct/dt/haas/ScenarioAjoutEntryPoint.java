package com.vsct.dt.haas;

import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vsct.dt.haas.events.AddNewEntryPointEvent;
import com.vsct.dt.haas.events.EntryPointDeployedEvent;
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
public class ScenarioAjoutEntryPoint {

    EventBus eventBus = new EventBus();
    AdminState adminState;

    @Before
    public void setUp(){
        adminState = new AdminState();
        eventBus.register(adminState);
    }

    @Test
    public void scenario_add_new_entry_point_when_not_existing(){
        Haproxy haproxy = new Haproxy("ip_master", "ip_slave");

        AddNewEntryPointEvent addNewEntryPointEvent = new AddNewEntryPointEvent(new EntryPoint(haproxy, "OCE", "REC1", "hapocer1", "54250", ImmutableSet.<Frontend>of(), ImmutableSet.<Backend>of()));

        assertThat(adminState.getEntryPoint("OCE", "REC1").isPresent()).isFalse();

        eventBus.post(addNewEntryPointEvent);

        Optional<EntryPoint> ep = adminState.getEntryPoint("OCE", "REC1");
        assertThat(ep.isPresent()).isTrue();
        assertThat(ep.get().getStatus().equals(EntryPointStatus.DEPLOYING));
    }

    @Test
    public void scenario_receive_ep_deployed_event(){
        Haproxy haproxy = new Haproxy("ip_master", "ip_slave");
        EntryPoint entryPoint = new EntryPoint(haproxy, "OCE", "REC1", "hapocer1", "54250", EntryPointStatus.DEPLOYING);

        adminState.putEntryPoint(entryPoint);

        EntryPointDeployedEvent entryPointDeployedEvent = new EntryPointDeployedEvent("OCE", "REC1");
        eventBus.post(entryPointDeployedEvent);

        Optional<EntryPoint> ep = adminState.getEntryPoint("OCE", "REC1");
        assertThat(ep.isPresent()).isTrue();
        assertThat(ep.get().getStatus()).isEqualTo(EntryPointStatus.DEPLOYED);
    }



}
