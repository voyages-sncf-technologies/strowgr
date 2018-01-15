/*
 * Copyright (C) 2016 VSCT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vsct.dt.strowgr.admin.gui.configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.cache.CacheBuilderSpec;
import com.vsct.dt.strowgr.admin.core.security.model.User;
import com.vsct.dt.strowgr.admin.gui.configuration.scheduler.PeriodicSchedulerFactory;
import com.vsct.dt.strowgr.admin.gui.security.LDAPAuthenticator;
import com.vsct.dt.strowgr.admin.gui.security.LdapConfiguration;
import com.vsct.dt.strowgr.admin.gui.security.NoProdAuthenticator;
import com.vsct.dt.strowgr.admin.gui.security.ProdAuthenticator;

import io.dropwizard.Configuration;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentials;
import io.dropwizard.client.HttpClientConfiguration;

import java.util.Optional;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StrowgrConfiguration extends Configuration {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(StrowgrConfiguration.class);

    public StrowgrConfiguration() {
        super();
    }

    @Min(1)
    private long handledHaproxyRefreshPeriodSecond = 20;

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
    private int threads = 200;

    @Min(10)
    private int commitTimeout = 13;

    @Valid
    @NotNull
    private HttpClientConfiguration httpClient = new HttpClientConfiguration();

    @Valid
    @Nullable
    private NSQConfigFactory nsqConsumerConfigFactory;

    @Valid
    @Nullable
    private NSQConfigFactory nsqProducerConfigFactory;

    @Nullable
    private String nsqChannel = "admin";
    
    @Valid
    @NotEmpty
    private String authenticatorType;

    @Valid
    @NotEmpty
    private String useDefaultUserWhenAuthentFails;
    
    @Valid
    @JsonProperty
    private LdapConfiguration ldapConfiguration;
    
    @Valid
    @NotNull
    @JsonProperty
    private CacheBuilderSpec authenticationCachePolicy;
    
    // the value for haproxy.platform attribute to filter (SRE-81)
    @Valid
    @NotEmpty
    private String platformValue;

    
    @JsonIgnore
    public Optional<Authenticator<BasicCredentials, User>> getAuthenticator() {
    	
        final String authType = authenticatorType.toLowerCase();
        
        LOGGER.trace("authenticatorType={}", authType);

        if (authType.equals("none")) {
            return Optional.empty();
        } else if (authType.equals("prod_mock")) {
            return Optional.of(new ProdAuthenticator(platformValue));
        } else if (authType.equals("noprod_mock")) {
            return Optional.of(new NoProdAuthenticator(platformValue));
        } else if (authType.equals("ldap")) {
            if (ldapConfiguration == null) {
                throw new IllegalArgumentException("Authenticator type is set to 'ldap' but ldap configuration is empty.");
            }

            return Optional.of(new LDAPAuthenticator(platformValue,ldapConfiguration));
        } else {
            throw new IllegalArgumentException("Authenticator " + authenticatorType + " is unknow. Use one of ['none', 'simple', 'ldap']");
        }
        
    }
    

    @JsonProperty("nsqChannel")
    public String getNsqChannel() {
        return nsqChannel;
    }

    @JsonProperty("nsqChannel")
    public void setNsqChannel(String nsqChannel) {
        this.nsqChannel = nsqChannel;
    }

    @JsonProperty("nsqConsumerConfigFactory")
    public NSQConfigFactory getNsqConsumerConfigFactory() {
        if (nsqProducerConfigFactory == null) {
            return new NSQConfigFactory();
        }
        return nsqConsumerConfigFactory;
    }

    @JsonProperty("nsqConsumerConfigFactory")
    public void setNsqConsumerConfigFactory(NSQConfigFactory nsqConsumerConfigFactory) {
        this.nsqConsumerConfigFactory = nsqConsumerConfigFactory;
    }

    @JsonProperty("nsqProducerConfigFactory")
    public NSQConfigFactory getNsqProducerConfigFactory() {
        if (nsqProducerConfigFactory == null) {
            return new NSQConfigFactory();
        }
        return nsqProducerConfigFactory;
    }

    @JsonProperty("nsqProducerConfigFactory")
    public void setNsqProducerConfigFactory(NSQConfigFactory nsqProducerConfigFactory) {
        this.nsqProducerConfigFactory = nsqProducerConfigFactory;
    }

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


    @JsonProperty("httpClient")
    public HttpClientConfiguration getHttpClientConfiguration() {
        return httpClient;
    }

    @JsonProperty("httpClient")
    public void setHttpClientConfiguration(HttpClientConfiguration httpClient) {
        this.httpClient = httpClient;
    }

    public long getHandledHaproxyRefreshPeriodSecond() {
        return handledHaproxyRefreshPeriodSecond;
    }

    public void setHandledHaproxyRefreshPeriodSecond(long handledHaproxyRefreshPeriodSecond) {
        this.handledHaproxyRefreshPeriodSecond = handledHaproxyRefreshPeriodSecond;
    }


	public String getAuthenticatorType() {
		return authenticatorType;
	}

	@JsonProperty
	public void setAuthenticatorType(String authenticatorType) {
		this.authenticatorType = authenticatorType;
	}

    public boolean useDefaultUserWhenAuthentFails() {
        return useDefaultUserWhenAuthentFails.equals("true");
    }

    public void setUseDefaultUserWhenAuthentFails(String useDefaultUserWhenAuthentFails) {
        this.useDefaultUserWhenAuthentFails = useDefaultUserWhenAuthentFails;
    }

	public LdapConfiguration getLdapConfiguration() {
		return ldapConfiguration;
	}


	public void setLdapConfiguration(LdapConfiguration ldapConfiguration) {
		this.ldapConfiguration = ldapConfiguration;
	}


	public CacheBuilderSpec getAuthenticationCachePolicy() {
		return authenticationCachePolicy;
	}


	public void setAuthenticationCachePolicy(CacheBuilderSpec authenticationCachePolicy) {
		this.authenticationCachePolicy = authenticationCachePolicy;
	}


	public void setPlatformValue(String platformValue) {
		this.platformValue = platformValue;
	}
	
    
}
