/*
 *  Copyright (C) 2016 VSCT
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.vsct.dt.strowgr.admin.repository.consul;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.vsct.dt.strowgr.admin.core.EntryPointKey;
import com.vsct.dt.strowgr.admin.core.EntryPointRepository;
import com.vsct.dt.strowgr.admin.core.PortProvider;
import com.vsct.dt.strowgr.admin.core.configuration.EntryPoint;
import com.vsct.dt.strowgr.admin.repository.consul.mapping.json.CommittingConfigurationJson;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ConsulRepository implements EntryPointRepository, PortProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulRepository.class);

    /**
     * Enum on different behavior on a session.
     * RELEASE removes only the session when the TTL is reached or after an explicit release.
     * DELETE removes the key/value which acquires this session too.
     */
    private enum CONSUL_BEHAVIOR {
        RELEASE("release"), DELETE("delete");
        private String value;

        CONSUL_BEHAVIOR(String value) {
            this.value = value;
        }
    }

    private final String host;
    private final int port;
    private int minGeneratedPort;
    private int maxGeneratedPort;

    private final CloseableHttpClient client;
    private final ObjectMapper mapper;
    private final ConsulReader consulReader;

    private ThreadLocal<String> sessionLocal = new ThreadLocal<>();

    private Random random = new Random(System.nanoTime());

    public ConsulRepository(String host, int port, int minGeneratedPort, int maxGeneratedPort) {
        this.host = host;
        this.port = port;
        this.minGeneratedPort = minGeneratedPort;
        this.maxGeneratedPort = maxGeneratedPort;
        this.client = HttpClients.createDefault();
        mapper = new ObjectMapper();
        consulReader = new ConsulReader(mapper);
    }

    ConsulRepository(String host, int port, int minGeneratedPort, int maxGeneratedPort, ObjectMapper mapper, ConsulReader consulReader, CloseableHttpClient client) {
        this.host = host;
        this.port = port;
        this.minGeneratedPort = minGeneratedPort;
        this.maxGeneratedPort = maxGeneratedPort;
        this.client = client;
        this.mapper = mapper;
        this.consulReader = consulReader;
    }


    @Override
    public boolean lock(EntryPointKey entryPointKey) {
        boolean locked = false;
        String sessionId = null;
        try {
            if (sessionLocal.get() == null) {
                sessionId = createSession(entryPointKey).orElseThrow(IllegalStateException::new).ID;
                sessionLocal.set(sessionId);
            } else {
                LOGGER.warn("reuse session for key {}");
            }

            LOGGER.debug("attempt to acquire lock for key {} on session {}", entryPointKey, sessionId);
            HttpPut acquireEntryPointKeyURI = new HttpPut("http://" + host + ":" + port + "/v1/kv/admin/" + entryPointKey + "/lock?acquire=" + sessionId);

            /* TODO, implement wait with a blocking query */
            int count = 0;
            while (!locked && (count++ < 10)) {
                locked = client.execute(acquireEntryPointKeyURI, httpResponse -> consulReader.parseHttpResponse(httpResponse, consulReader::parseBooleanFromHttpEntity)).orElse(Boolean.FALSE);
                if (!locked) {
                    /* Avoid crazy spinning*/
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        LOGGER.error("error in consul repository for session " + sessionId + " and key " + entryPointKey, e);
                    }
                } else {
                    LOGGER.debug("lock acquired for key {} on session {}", entryPointKey, sessionId);
                }
            }

        } catch (IOException e) {
            LOGGER.error("error in consul repository for session " + sessionId + " and key " + entryPointKey, e);
        }
        return locked;
    }

    Optional<Session> createSession(EntryPointKey entryPointKey) throws IOException {
        return createSession(entryPointKey, 10, CONSUL_BEHAVIOR.RELEASE);
    }

    private Optional<Session> createSession(EntryPointKey entryPointKey, Integer ttlInSec, CONSUL_BEHAVIOR behavior) throws IOException {
        HttpPut createSessionURI = new HttpPut("http://" + host + ":" + port + "/v1/session/create");
        if (ttlInSec != null) {
            String payload = "{\"Behavior\":\"" + behavior.value + "\",\"TTL\":\"" + ttlInSec + "s\", \"Name\":\"" + entryPointKey.getID() + "\"}";
            LOGGER.trace("create a consul session with theses options: {} ", payload);
            createSessionURI.setEntity(new StringEntity(payload));
        }
        Optional<Session> session = client.execute(createSessionURI, response -> consulReader.parseHttpResponse(response, consulReader::parseSessionFromHttpEntity));
        session.ifPresent(s -> LOGGER.debug("get session {} for key {}", s.ID, entryPointKey));
        return session;
    }

    @Override
    public void release(EntryPointKey key) {
        try {
            LOGGER.debug("attempt to release lock for key " + key + " on session " + sessionLocal.get());
            HttpPut releaseEntryPointKeyURI = new HttpPut("http://" + host + ":" + port + "/v1/kv/admin/" + key.getID() + "/lock?release=" + sessionLocal.get());
            client.execute(releaseEntryPointKeyURI, httpResponse -> consulReader.parseHttpResponse(httpResponse, consulReader::parseBooleanFromHttpEntity));
            LOGGER.debug("lock released for key " + key + " on session " + sessionLocal.get());
        } catch (IOException e) {
            LOGGER.error("error in consul repository", e);
        } finally {
            sessionLocal.remove();
        }
    }

    @Override
    public Optional<EntryPoint> getCurrentConfiguration(EntryPointKey key) {
        try {
            LOGGER.trace("attempt to get the current configuration for key " + key);
            HttpGet getCurrentURI = new HttpGet("http://" + host + ":" + port + "/v1/kv/admin/" + key.getID() + "/current?raw");
            return client.execute(getCurrentURI, httpResponse -> consulReader.parseHttpResponseAccepting404(httpResponse, consulReader::parseEntryPointMappingJsonFromHttpEntity));
        } catch (IOException e) {
            LOGGER.error("error in consul repository", e);
            return Optional.empty();
        }
    }

    @Override
    public Set<String> getEntryPointsId() {
        try {
            HttpGet listKeysURI = new HttpGet("http://" + host + ":" + port + "/v1/kv/admin?keys");
            Optional<Set<String>> allKeys = client.execute(listKeysURI, httpResponse -> consulReader.parseHttpResponse(httpResponse, consulReader::parseKeysFromHttpEntity));
            return allKeys.orElse(new HashSet<>()).stream()
                    .filter(s -> !s.contains("lock"))
                    .map(s -> s.replace("admin/", ""))
                    .map(s -> s.replace("/lock", ""))
                    .map(s -> s.replace("/current", ""))
                    .map(s -> s.replace("/pending", ""))
                    .map(s -> s.replace("/committing", ""))
                    .distinct()
                    .collect(Collectors.toSet());

        } catch (IOException e) {
            LOGGER.error("error in consul repository", e);
            return new HashSet<>();
        }
    }

    @Override
    public Optional<EntryPoint> getPendingConfiguration(EntryPointKey key) {
        try {
            HttpGet getPendingURI = new HttpGet("http://" + host + ":" + port + "/v1/kv/admin/" + key.getID() + "/pending?raw");
            return client.execute(getPendingURI, httpResponse -> consulReader.parseHttpResponseAccepting404(httpResponse, consulReader::parseEntryPointMappingJsonFromHttpEntity));
        } catch (IOException e) {
            LOGGER.error("error in consul repository", e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<EntryPoint> getCommittingConfiguration(EntryPointKey key) {
        return getCommittingConfigurationWithCorrelationId(key).map(committingConfigurationJson -> new EntryPoint(committingConfigurationJson.getHaproxy(), committingConfigurationJson.getHapUser(), committingConfigurationJson.getFrontends(), committingConfigurationJson.getBackends(), committingConfigurationJson.getContext()));
    }

    private Optional<CommittingConfigurationJson> getCommittingConfigurationWithCorrelationId(EntryPointKey key) {
        try {
            HttpGet getCommittingURI = new HttpGet("http://" + host + ":" + port + "/v1/kv/admin/" + key.getID() + "/committing?raw");
            return client.execute(getCommittingURI, response -> consulReader.parseHttpResponseAccepting404(response, consulReader::parseCommittingConfigurationJson));
        } catch (IOException e) {
            LOGGER.error("error in consul repository", e);
            return Optional.empty();
        }
    }

    @Override
    public void setPendingConfiguration(EntryPointKey key, EntryPoint configuration) {
        try {
            HttpPut setPendingURI = new HttpPut("http://" + host + ":" + port + "/v1/kv/admin/" + key.getID() + "/pending");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            mapper.writeValue(out, configuration);

            setPendingURI.setEntity(new ByteArrayEntity(out.toByteArray()));

            client.execute(setPendingURI, httpResponse -> consulReader.parseHttpResponse(httpResponse, consulReader::parseBooleanFromHttpEntity));
        } catch (IOException e) {
            LOGGER.error("error in consul repository", e);
        }
    }

    @Override
    public void removePendingConfiguration(EntryPointKey key) {
        try {
            HttpDelete deletePendingURI = new HttpDelete("http://" + host + ":" + port + "/v1/kv/admin/" + key.getID() + "/pending");
            client.execute(deletePendingURI, httpResponse -> consulReader.parseHttpResponse(httpResponse, consulReader::parseBooleanFromHttpEntity));
        } catch (IOException e) {
            LOGGER.error("error in consul repository", e);
        }
    }

    @Override
    public void setCommittingConfiguration(String correlationId, EntryPointKey entryPointKey, EntryPoint configuration, int ttl) {
        try {
            /* Use Consul session to use TTL feature
               This implies that when the consul node holding the session is lost,
               the session and thus the committing config will also be lost,
               TTL cannot be honored in that corner case.
             */
            String sessionId = createSession(entryPointKey, ttl, CONSUL_BEHAVIOR.DELETE).orElseThrow(IllegalStateException::new).ID;

            HttpPut setCommittingURI = new HttpPut("http://" + host + ":" + port + "/v1/kv/admin/" + entryPointKey.getID() + "/committing?acquire=" + sessionId);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            mapper.writeValue(out, new CommittingConfigurationJson(correlationId, configuration));

            setCommittingURI.setEntity(new ByteArrayEntity(out.toByteArray()));

            client.execute(setCommittingURI, httpResponse -> consulReader.parseHttpResponse(httpResponse, consulReader::parseBooleanFromHttpEntity));
        } catch (IOException e) {
            LOGGER.error("error in consul repository", e);
        }
    }

    @Override
    public void removeCommittingConfiguration(EntryPointKey key) {
        try {
            HttpDelete deleteCommittingURI = new HttpDelete("http://" + host + ":" + port + "/v1/kv/admin/" + key.getID() + "/committing");
            client.execute(deleteCommittingURI, httpResponse -> consulReader.parseHttpResponse(httpResponse, consulReader::parseBooleanFromHttpEntity));
        } catch (IOException e) {
            LOGGER.error("error in consul repository", e);
        }
    }

    @Override
    public void setCurrentConfiguration(EntryPointKey key, EntryPoint configuration) {
        try {
            HttpPut setCurrentURI = new HttpPut("http://" + host + ":" + port + "/v1/kv/admin/" + key.getID() + "/current");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            mapper.writeValue(out, configuration);

            setCurrentURI.setEntity(new ByteArrayEntity(out.toByteArray()));

            client.execute(setCurrentURI, httpResponse -> consulReader.parseHttpResponse(httpResponse, consulReader::parseBooleanFromHttpEntity));
        } catch (IOException e) {
            LOGGER.error("error in consul repository", e);
        }
    }

    @Override
    public Optional<Map<String, Integer>> getPorts() {
        try {
            HttpGet getPortsById = new HttpGet("http://" + host + ":" + port + "/v1/kv/ports");
            Optional<ConsulItem<Map<String, Integer>>> result = client.execute(getPortsById, httpResponse -> consulReader.parseHttpResponse(httpResponse, consulReader::parsePortsByHaproxyFromHttpEntity));
            if (result.isPresent()) {
                return Optional.of(result.get().value(mapper));
            } else {
                return Optional.empty();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Integer> getPort(String key) {
        try {
            // TODO should use ?raw with a different handler
            HttpGet getPortsById = new HttpGet("http://" + host + ":" + port + "/v1/kv/ports");
            Optional<ConsulItem<Map<String, Integer>>> portsByEntrypoint = client.execute(getPortsById, httpResponse -> consulReader.parseHttpResponse(httpResponse, consulReader::parsePortsByHaproxyFromHttpEntity));
            if (portsByEntrypoint.isPresent()) {
                Map<String, Integer> portsByEntrypointRaw = portsByEntrypoint.get().value(mapper);
                if (portsByEntrypointRaw.containsKey(key)) {
                    return Optional.of(portsByEntrypointRaw.get(key));
                }
            }
            return Optional.empty();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Integer newPort(String key) {
        int newPort = -1;
        try {
            boolean failToPutNewPort = true;
            while (failToPutNewPort) {
                HttpGet getPortById = new HttpGet("http://" + host + ":" + port + "/v1/kv/ports");
                Optional<ConsulItem<Map<String, Integer>>> portsByEntrypoint = client.execute(getPortById, httpResponse -> consulReader.parseHttpResponse(httpResponse, consulReader::parsePortsByHaproxyFromHttpEntity));
                HttpPut putPortById;
                if (portsByEntrypoint.isPresent()) {
                    // Ports map has been already initialized
                    Map<String, Integer> rawPortsByEntrypoint = portsByEntrypoint.get().value(mapper);
                    if (rawPortsByEntrypoint.containsKey(key)) {
                        throw new IllegalStateException("Port for key " + key + " is already setted. It's port " + rawPortsByEntrypoint.get(key));
                    }

                    boolean portAlreadyUsed = true;
                    while (portAlreadyUsed) {
                        newPort = random.nextInt(maxGeneratedPort - minGeneratedPort) + minGeneratedPort;
                        portAlreadyUsed = rawPortsByEntrypoint.values().contains(newPort);
                    }

                    rawPortsByEntrypoint.put(key, newPort);

                    putPortById = new HttpPut("http://" + host + ":" + port + "/v1/kv/ports?cas=" + portsByEntrypoint.get().getModifyIndex());
                    String encodedJson = encodeJson(rawPortsByEntrypoint);
                    putPortById.setEntity(new StringEntity(encodedJson));
                } else {
                    // First initialization
                    newPort = random.nextInt(maxGeneratedPort - minGeneratedPort) + minGeneratedPort;
                    Map<String, Integer> rawPortsByEntrypoint = new HashMap<>();
                    rawPortsByEntrypoint.put(key, newPort);
                    putPortById = new HttpPut("http://" + host + ":" + port + "/v1/kv/ports?cas=0");
                    String encodedJson = encodeJson(rawPortsByEntrypoint);
                    putPortById.setEntity(new StringEntity(encodedJson));
                }
                failToPutNewPort = !client.execute(putPortById, httpResponse -> consulReader.parseHttpResponse(httpResponse, consulReader::parseBooleanFromHttpEntity))
                        .orElseThrow((Supplier<RuntimeException>) () -> new IllegalStateException("can't parse boolean value"));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return newPort;
    }

    @Override
    public Optional<String> getHaproxyVip(String haproxyName) {
        try {
            HttpGet getHaproxyURI = new HttpGet("http://" + host + ":" + port + "/v1/kv/haproxy/" + haproxyName + "/vip?raw");
            return client.execute(getHaproxyURI, httpResponse -> consulReader.parseHttpResponseAccepting404(httpResponse, consulReader::readRawContentFromHttpEntity));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<String> getCommitCorrelationId(EntryPointKey key) {
        return getCommittingConfigurationWithCorrelationId(key).map(CommittingConfigurationJson::getCorrelationId);
    }

    private String encodeJson(Map<String, Integer> portsByEntrypoint) throws IOException {
        ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
        return ow.writeValueAsString(portsByEntrypoint);
    }

    public void shutdown() {
        try {
            this.client.close();
        } catch (IOException e) {
            LOGGER.error("error in consul repository", e);
        }
    }

    static class Session {
        private String ID;

        @JsonCreator
        Session(@JsonProperty("ID") String ID) {
            this.ID = ID;
        }
    }

}
