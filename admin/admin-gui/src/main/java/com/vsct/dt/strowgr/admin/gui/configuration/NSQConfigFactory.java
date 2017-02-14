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

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.vsct.dt.nsq.NSQConfig;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.net.ssl.SSLException;
import javax.validation.Valid;

public class NSQConfigFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(NSQConfigFactory.class);

    @Nullable
    private String clientId;
    private boolean featureNegociation = true;
    @Nullable
    private Integer               heartbeatInterval;
    @Nullable
    private Integer               outputBufferSize;
    @Nullable
    private Integer               outputBufferTimeout;
    @Valid
    @Nullable
    private NSQCompressionFactory nsqCompressionFactory;
    @Nullable
    private Integer               sampleRate;
    @Nullable
    private String                userAgent;
    @Nullable
    private Integer               msgTimeout;
    private boolean useSSL = false;
    private SslContext        sslContext;
    @Nullable
    private Integer           eventLoopThreads;
    private NioEventLoopGroup eventLoopGroup;

    public NSQConfig build() throws SSLException {
        NSQConfig config = new NSQConfig();
        config.setClientId(getClientId());
        config.setFeatureNegotiation(isFeatureNegociation());
        config.setHeartbeatInterval(getHeartbeatInterval());
        config.setOutputBufferSize(getOutputBufferSize());
        config.setOutputBufferTimeout(getOutputBufferTimeout());
        config.setCompression(getCompression());
        config.setDeflateLevel(getDeflateLevel());
        config.setSampleRate(getSampleRate());
        config.setUserAgent(getUserAgent());
        config.setMsgTimeout(getMsgTimeout());
        //With sslcontext, NSQConfig makes a check, so we inject it only if it is not null
        if(getSslContext() != null) {
            config.setSslContext(getSslContext());
        }
        config.setEventLoopGroup(getEventLoopGroup());
        return config;
    }

    @JsonProperty("clientId")
    public String getClientId() {
        return clientId;
    }

    @JsonProperty("clientId")
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @JsonProperty("featureNegociation")
    public boolean isFeatureNegociation() {
        return featureNegociation;
    }

    @JsonProperty("featureNegociation")
    public void setFeatureNegociation(boolean featureNegociation) {
        this.featureNegociation = featureNegociation;
    }

    @JsonProperty("heartbeatInterval")
    public Integer getHeartbeatInterval() {
        return heartbeatInterval;
    }

    @JsonProperty("heartbeatInterval")
    public void setHeartbeatInterval(Integer heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    @JsonProperty("outputBufferSize")
    public Integer getOutputBufferSize() {
        return outputBufferSize;
    }

    @JsonProperty("outputBufferSize")
    public void setOutputBufferSize(Integer outputBufferSize) {
        this.outputBufferSize = outputBufferSize;
    }

    @JsonProperty("outputBufferTimeout")
    public Integer getOutputBufferTimeout() {
        return outputBufferTimeout;
    }

    @JsonProperty("outputBufferTimeout")
    public void setOutputBufferTimeout(Integer outputBufferTimeout) {
        this.outputBufferTimeout = outputBufferTimeout;
    }

    @JsonProperty("compression")
    public NSQCompressionFactory getNsqCompressionFactory() {
        return nsqCompressionFactory;
    }

    @JsonProperty("compression")
    public void setNsqCompressionFactory(NSQCompressionFactory nsqCompressionFactory) {
        this.nsqCompressionFactory = nsqCompressionFactory;
    }

    @JsonProperty("sampleRate")
    public Integer getSampleRate() {
        return sampleRate;
    }

    @JsonProperty("sampleRate")
    public void setSampleRate(Integer sampleRate) {
        this.sampleRate = sampleRate;
    }

    @JsonProperty("userAgent")
    public String getUserAgent() {
        return userAgent;
    }

    @JsonProperty("userAgent")
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    @JsonProperty("msgTimeout")
    public Integer getMsgTimeout() {
        return msgTimeout;
    }

    @JsonProperty("msgTimeout")
    public void setMsgTimeout(Integer msgTimeout) {
        this.msgTimeout = msgTimeout;
    }

    @JsonProperty("useSSL")
    public boolean isUseSSL() {
        return useSSL;
    }

    @JsonProperty("useSSL")
    public void setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }

    @JsonProperty("eventLoopThreads")
    public Integer getEventLoopThreads() {
        return eventLoopThreads;
    }

    @JsonProperty("eventLoopThreads")
    public void setEventLoopThreads(Integer eventLoopThreads) {
        this.eventLoopThreads = eventLoopThreads;
    }

    public NSQConfig.Compression getCompression() {
        if (nsqCompressionFactory == null) {
            return NSQConfig.Compression.NO_COMPRESSION;
        }
        else {
            return nsqCompressionFactory.getCompression();
        }
    }

    public Integer getDeflateLevel() {
        if (nsqCompressionFactory == null) {
            return null;
        }
        return nsqCompressionFactory.getDeflateLevel();
    }

    //TODO will need more on this if we want to use ssl
    public SslContext getSslContext() throws SSLException {
        if (useSSL && sslContext == null) {
            sslContext = SslContextBuilder.forClient().build();
        }
        return sslContext;
    }

    public EventLoopGroup getEventLoopGroup() {
        int nThreads = 0;
        if (eventLoopThreads != null) {
            if (eventLoopThreads > 1) {
                nThreads = eventLoopThreads;
            }
            else {
                LOGGER.warn("eventLoopThreads must be a positive integer. 0 means default number of threads for Netty (2xCPU). Defaulting to 0");
            }
        }
        if(eventLoopGroup == null){
            eventLoopGroup = new NioEventLoopGroup(nThreads);
        }
        return eventLoopGroup;
    }
}
