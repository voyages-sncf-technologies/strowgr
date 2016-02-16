package com.vsct.dt.haas.admin.gui.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.haas.admin.gui.configuration.ConsulRepositoryFactory;
import io.dropwizard.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class HaasConfiguration extends Configuration {

    @Valid
    @NotNull
    private ConsulRepositoryFactory consulRepositoryFactory;

    @Valid
    @NotNull
    private NSQLookupFactory nsqLookupfactory;

    @Valid
    @NotNull
    private NSQProducerFactory nsqProducerFactory;
    
    @Valid
    @NotNull
    private PeriodicSchedulerFactory periodicSchedulerFactory;

    @Min(1)
    private int threads;

    @JsonProperty("repository")
    public ConsulRepositoryFactory getConsulRepositoryFactory() {
        return consulRepositoryFactory;
    }

    @JsonProperty("repository")
    public void setConsulRepositoryFactory(ConsulRepositoryFactory consulRepositoryFactory) {
        this.consulRepositoryFactory = consulRepositoryFactory;
    }

    @JsonProperty("nsqLookup")
    public NSQLookupFactory getNsqLookupfactory() {
        return nsqLookupfactory;
    }

    @JsonProperty("nsqLookup")
    public void setNsqLookupfactory(NSQLookupFactory nsqLookupfactory) {
        this.nsqLookupfactory = nsqLookupfactory;
    }

    @JsonProperty("nsqProducer")
    public NSQProducerFactory getNsqProducerFactory() {
        return nsqProducerFactory;
    }

    @JsonProperty("nsqProducer")
    public void setNsqProducerFactory(NSQProducerFactory nsqProducerFactory) {
        this.nsqProducerFactory = nsqProducerFactory;
    }

    @JsonProperty("periodicScheduler")
    public PeriodicSchedulerFactory getPeriodicSchedulerFactory() {
        return periodicSchedulerFactory;
    }

    @JsonProperty("periodicScheduler")
    public void PeriodicSchedulerFactory(PeriodicSchedulerFactory periodicSchedulerFactory) {
        this.periodicSchedulerFactory = periodicSchedulerFactory;
    }

    @JsonProperty("threads")
    public int getThreads() {
        return threads;
    }

    @JsonProperty("threads")
    public void setThreads(int threads) {
        this.threads = threads;
    }
}
