package com.vsct.dt.haas.nsq;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by william_montaz on 02/02/2016.
 */
public class EntryPointDeployedPayload extends Payload {

    @JsonProperty
    public String application;

    @JsonProperty
    public String platform;

}
