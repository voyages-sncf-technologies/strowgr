package com.vsct.dt.haas.nsq;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by william_montaz on 02/02/2016.
 */
public class Payload {

    @JsonProperty
    public long timestamp = System.currentTimeMillis();

    @JsonProperty
    public long correlationid = System.currentTimeMillis();

}
