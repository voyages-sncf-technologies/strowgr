package com.vsct.dt.haas.admin.core;

import java.util.Map;
import java.util.Optional;

/**
 * Created by william_montaz on 26/02/2016.
 */
public interface PortProvider {

    Optional<Map<String, Integer>> getPorts();

    Optional<Integer> getPort(String key);

    default Optional<Integer> getPort(EntryPointKey key, String portId){
        return getPort(PortProvider.getPortKey(key, portId));
    }

    Integer newPort(String key);

    default Integer newPort(EntryPointKey key, String portId){
        return newPort(PortProvider.getPortKey(key, portId));
    }

    static String getPortKey(EntryPointKey key, String portId) {
        return key.getID() + '-' + portId;
    }

}
