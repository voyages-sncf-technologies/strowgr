package com.vsct.dt.haas.admin.core.event;

import java.util.Random;

/**
 * Created by william_montaz on 10/02/2016.
 */
public class CorrelationId {

    private static Random random = new Random();

    public static String newCorrelationId() {
        return Long.toString(random.nextLong());
    }

}
