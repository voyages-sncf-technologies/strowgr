package com.vsct.dt.haas.admin.gui.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import org.hibernate.validator.constraints.NotEmpty;

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
    private CommitCompletedConsumerFactory commitCompletedConsumerFactory;

    @Valid
    @NotNull
    private CommitFailedConsumerFactory commitFailedConsumerFactory;

    @Valid
    @NotNull
    private RegisterServerMessageConsumerFactory registerServerMessageConsumerFactory;

    @NotEmpty
    private String defaultHAPName;

    @Min(1)
    private int threads;

    @Min(10)
    private int commitTimeout;

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

    @JsonProperty("commitCompletedConsumer")
    public CommitCompletedConsumerFactory getCommitCompletedConsumerFactory() {
        return commitCompletedConsumerFactory;
    }

    @JsonProperty("commitCompletedConsumer")
    public void setCommitCompletedConsumerFactory(CommitCompletedConsumerFactory commitCompletedConsumerFactory) {
        this.commitCompletedConsumerFactory = commitCompletedConsumerFactory;
    }

    @JsonProperty("commitFailedConsumer")
    public CommitFailedConsumerFactory getCommitFailedConsumerFactory() {
        return commitFailedConsumerFactory;
    }

    @JsonProperty("commitFailedConsumer")
    public void setCommitFailedConsumerFactory(CommitFailedConsumerFactory commitFailedConsumerFactory) {
        this.commitFailedConsumerFactory = commitFailedConsumerFactory;
    }

    @JsonProperty("registerServerMessageConsumer")
    public RegisterServerMessageConsumerFactory getRegisterServerMessageConsumerFactory() {
        return registerServerMessageConsumerFactory;
    }

    @JsonProperty("registerServerMessageConsumer")
    public void setRegisterServerMessageConsumerFactory(RegisterServerMessageConsumerFactory registerServerMessageConsumerFactory) {
        this.registerServerMessageConsumerFactory = registerServerMessageConsumerFactory;
    }

    @JsonProperty("haproxyName")
    public String getDefaultHAPName() {
        return defaultHAPName;
    }

    @JsonProperty("haproxyName")
    public void setDefaultHAPName(String defaultHAPName) {
        this.defaultHAPName = defaultHAPName;
    }

    @JsonProperty("threads")
    public int getThreads() {
        return threads;
    }

    @JsonProperty("threads")
    public void setThreads(int threads) {
        this.threads = threads;
    }

    @JsonProperty("commitTimeout")
    public int getCommitTimeout() {
        return commitTimeout;
    }

    @JsonProperty("commitTimeout")
    public void setCommitTimeout(int commitTimeout) {
        this.commitTimeout = commitTimeout;
    }
}
