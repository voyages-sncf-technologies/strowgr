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

    @Valid
    @NotNull
    private CommitMessageConsumerFactory commitMessageConsumerFactory;

    @Valid
    @NotNull
    private RegisterServerMessageConsumerFactory registerServerMessageConsumerFactory;

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
    public void setPeriodicSchedulerFactory(PeriodicSchedulerFactory periodicSchedulerFactory) {
        this.periodicSchedulerFactory = periodicSchedulerFactory;
    }

    @JsonProperty("commitMessageConsumer")
    public CommitMessageConsumerFactory getCommitMessageConsumerFactory() {
        return commitMessageConsumerFactory;
    }

    @JsonProperty("commitMessageConsumer")
    public void setCommitMessageConsumerFactory(CommitMessageConsumerFactory commitMessageConsumerFactory) {
        this.commitMessageConsumerFactory = commitMessageConsumerFactory;
    }

    @JsonProperty("registerServerMessageConsumer")
    public RegisterServerMessageConsumerFactory getRegisterServerMessageConsumerFactory() {
        return registerServerMessageConsumerFactory;
    }

    @JsonProperty("registerServerMessageConsumer")
    public void setRegisterServerMessageConsumerFactory(RegisterServerMessageConsumerFactory registerServerMessageConsumerFactory) {
        this.registerServerMessageConsumerFactory = registerServerMessageConsumerFactory;
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
