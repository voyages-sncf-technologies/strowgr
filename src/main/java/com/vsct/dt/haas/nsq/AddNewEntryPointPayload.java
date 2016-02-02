package com.vsct.dt.haas.nsq;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.haas.nsq.Payload;

/**
 * Created by william_montaz on 02/02/2016.
 */
public class AddNewEntryPointPayload extends Payload {

    @JsonProperty
    public String application;

    @JsonProperty
    public String platform;

    @JsonProperty
    public byte[] conf;
}
