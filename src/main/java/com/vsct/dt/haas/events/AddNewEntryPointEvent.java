package com.vsct.dt.haas.events;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.vsct.dt.haas.state.Backend;
import com.vsct.dt.haas.state.EntryPoint;
import com.vsct.dt.haas.state.Frontend;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by william_montaz on 02/02/2016.
 */
public class AddNewEntryPointEvent {

    private final EntryPoint entryPoint;

    public AddNewEntryPointEvent(EntryPoint entryPoint) {
        this.entryPoint = entryPoint;
    }

    public EntryPoint getEntryPoint() {
        return entryPoint;
    }
}
