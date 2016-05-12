package com.vsct.dt.haas.admin.core.event;

import java.util.Random;
import java.util.UUID;

/**
 * Created by william_montaz on 10/02/2016.
 */
public class CorrelationId {

    public static String newCorrelationId() {
        return UUID.randomUUID().toString();
    }

}
