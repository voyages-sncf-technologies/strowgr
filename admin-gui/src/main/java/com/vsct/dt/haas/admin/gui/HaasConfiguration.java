package com.vsct.dt.haas.admin.gui;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.haas.admin.gui.configuration.ConsulRepositoryFactory;
import io.dropwizard.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class HaasConfiguration extends Configuration {

    @Valid
    @NotNull
    private ConsulRepositoryFactory consulRepositoryFactory;

    @JsonProperty("repository")
    public ConsulRepositoryFactory getConsulRepositoryFactory() {
        return consulRepositoryFactory;
    }

    @JsonProperty("repository")
    public void setConsulRepositoryFactory(ConsulRepositoryFactory consulRepositoryFactory) {
        this.consulRepositoryFactory = consulRepositoryFactory;
    }
}
