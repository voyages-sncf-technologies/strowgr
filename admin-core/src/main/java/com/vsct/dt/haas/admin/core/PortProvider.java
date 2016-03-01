package com.vsct.dt.haas.admin.core;

import java.util.Map;
import java.util.Optional;

/**
 * Created by william_montaz on 26/02/2016.
 */
public interface PortProvider {

    Optional<Map<String, Integer>> getPorts();

    public Optional<Integer> getPort(String key);

    Integer newPort(String key);
}
