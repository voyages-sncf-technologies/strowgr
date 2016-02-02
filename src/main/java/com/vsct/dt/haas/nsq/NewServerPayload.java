package com.vsct.dt.haas.nsq;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by william_montaz on 02/02/2016.
 */
public class NewServerPayload extends Payload {

    @JsonProperty
    public String application;

    @JsonProperty
    public String platform;

    @JsonProperty
    public String backend;

    @JsonProperty
    public String instanceName;

    @JsonProperty
    public String name;

    @JsonProperty
    public String ip;

    @JsonProperty
    public String port;

}
