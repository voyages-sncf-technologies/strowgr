package com.vsct.dt.haas;

import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vsct.dt.haas.events.AddNewEntryPointEvent;
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
    public void add_new_entry_point_when_not_existing(){
        AddNewEntryPointEvent addNewEntryPointEvent = new AddNewEntryPointEvent(new EntryPoint("default-name", "OCE", "REC1", "hapocer1", "54250", ImmutableSet.<Frontend>of(), ImmutableSet.<Backend>of()));

        assertThat(adminState.getEntryPoint("OCE", "REC1").isPresent()).isFalse();

        eventBus.post(addNewEntryPointEvent);

        Optional<EntryPoint> ep = adminState.getPendingEntryPoint("OCE", "REC1");
        assertThat(ep.isPresent()).isTrue();
    }

    @Test
    public void add_new_entry_point_logs_when_existing(){
        EntryPoint entryPoint = new EntryPoint("default-name", "OCE", "REC1", "hapocer1", "54250");
        adminState.putEntryPoint(entryPoint);

        AddNewEntryPointEvent addNewEntryPointEvent = new AddNewEntryPointEvent(new EntryPoint("default-name", "OCE", "REC1", "hapocer1", "54250", ImmutableSet.<Frontend>of(), ImmutableSet.<Backend>of()));

        eventBus.post(addNewEntryPointEvent);

        Optional<EntryPoint> ep = adminState.getPendingEntryPoint("OCE", "REC1");
        assertThat(ep.isPresent()).isFalse();
    }

}
