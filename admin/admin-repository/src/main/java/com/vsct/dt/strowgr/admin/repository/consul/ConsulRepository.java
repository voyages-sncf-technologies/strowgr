package com.vsct.dt.strowgr.admin.repository.consul;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.vsct.dt.strowgr.admin.core.EntryPointKey;
import com.vsct.dt.strowgr.admin.core.EntryPointRepository;
import com.vsct.dt.strowgr.admin.core.PortProvider;
import com.vsct.dt.strowgr.admin.core.configuration.EntryPoint;
import com.vsct.dt.strowgr.admin.repository.consul.mapping.json.CommittingConfigurationJson;
import com.vsct.dt.strowgr.admin.repository.consul.mapping.json.EntryPointMappingJson;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
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
    private final ObjectMapper mapper = new ObjectMapper();

    private ThreadLocal<String> sessionLocal = new ThreadLocal<>();

    private Random random = new Random(System.nanoTime());

    public ConsulRepository(String host, int port, int minGeneratedPort, int maxGeneratedPort) {
        this.host = host;
        this.port = port;
        this.minGeneratedPort = minGeneratedPort;
        this.maxGeneratedPort = maxGeneratedPort;
        this.client = HttpClients.createDefault();
    }

    protected <T> Optional<T> parseHttpResponse(HttpResponse httpResponse, Function<HttpEntity, Optional<T>> method) throws ClientProtocolException {
        int status = httpResponse.getStatusLine().getStatusCode();
        HttpEntity entity = httpResponse.getEntity();
        if (status >= 200 && status < 300) {
            return method.apply(entity);
        } else {
            String content = "no content";
            if (entity != null) {
                try {
                    content = EntityUtils.toString(entity);
                } catch (IOException e) {
                    LOGGER.error("can't parse content from consul", e);
                }
            }
            throw new ClientProtocolException("Unexpected response status: " + status + ": " + httpResponse.getStatusLine().getReasonPhrase() + ", entity is " + content);
        }
    }

    protected <T> Optional<T> parseHttpResponseAccepting404(HttpResponse httpResponse, Function<HttpEntity, Optional<T>> method) throws ClientProtocolException {
        int status = httpResponse.getStatusLine().getStatusCode();
        HttpEntity entity = httpResponse.getEntity();
        if (status == 404) {
            return Optional.<T>empty();
        } else if (status >= 200 && status < 300) {
            return method.apply(entity);
        } else {
            String content = "no content";
            if (entity != null) {
                try {
                    content = EntityUtils.toString(entity);
                } catch (IOException e) {
                    LOGGER.error("can't parse content from consul", e);
                }
            }
            throw new ClientProtocolException("Unexpected response status: " + status + ": " + httpResponse.getStatusLine().getReasonPhrase() + ", entity is " + content);
        }
    }

    protected Optional<Session> readSessionFromHttpEntity(HttpEntity httpEntity) {
        Optional<Session> result = Optional.empty();
        try {
            result = Optional.of(mapper.readValue(httpEntity.getContent(), Session.class));
        } catch (IOException e) {
            LOGGER.error("can't read session", e);
        }
        return result;
    }

    protected Optional<Boolean> readBooleanFromHttpEntity(HttpEntity httpEntity) {
        Optional<Boolean> result = Optional.empty();
        try {
            result = Optional.of(Boolean.parseBoolean(EntityUtils.toString(httpEntity)));
        } catch (IOException e) {
            LOGGER.error("can't read boolean", e);
        }
        return result;
    }

    protected Optional<Set<String>> readKeysFromHttpEntity(HttpEntity httpEntity) {
        Optional<Set<String>> result = Optional.empty();

        try {
            result = Optional.of(mapper.readValue(httpEntity.getContent(), new TypeReference<Set<String>>() {
            }));
        } catch (IOException e) {
            LOGGER.error("can't read keys", e);
        }
        return result;
    }

    protected Optional<EntryPoint> readEntryPointMappingJsonFromHttpEntity(HttpEntity httpEntity) {
        Optional<EntryPoint> result = Optional.empty();
        try {
            result = Optional.of(mapper.readValue(httpEntity.getContent(), EntryPointMappingJson.class));
        } catch (IOException e) {
            LOGGER.error("can't read keys", e);
        }
        return result;
    }

    protected Optional<ConsulItem<Map<String, Integer>>> readPortsByHaproxyFromHttpEntity(HttpEntity httpEntity) {
        Optional<ConsulItem<Map<String, Integer>>> result = Optional.empty();
        try {
            List<ConsulItem<Map<String, Integer>>> consulItems = mapper.readValue(httpEntity.getContent(), new TypeReference<List<ConsulItem<Map<String, Integer>>>>() {
            });
            if (consulItems.size() > 1) {
                throw new IllegalStateException("get too many ports mapping");
            } else {
                result = Optional.of(consulItems.get(0));
            }
        } catch (IOException e) {
            LOGGER.error("can't read ports by haproxy", e);
        }
        return result;
    }

    protected Optional<String> readRawContentFromHttpEntity(HttpEntity entity) {
        Optional<String> result = Optional.empty();
        try {
            result = Optional.of(EntityUtils.toString(entity));
        } catch (IOException e) {
            LOGGER.error("can't convert HttpEntity to string", e);
        }
        return result;
    }

    @Override
    public void lock(EntryPointKey entryPointKey) {
        try {
            Session session;
            if (sessionLocal.get() == null) {
                session = createSession(entryPointKey);
                sessionLocal.set(session.ID);
            } else {
                LOGGER.warn("reuse session for key {}, session {}", sessionLocal.get());
            }

            LOGGER.debug("attempt to acquire lock for key {} on session {}", entryPointKey, sessionLocal.get());
            HttpPut acquireEntryPointKeyURI = new HttpPut("http://" + host + ":" + port + "/v1/kv/admin/" + entryPointKey + "/lock?acquire=" + sessionLocal.get());

            /* TODO, implement wait with a blocking query */
            Optional<Boolean> locked = Optional.of(Boolean.TRUE);
            int count = 0;
            while (!locked.get() && (count++ < 10)) {
                locked = client.execute(acquireEntryPointKeyURI, httpResponse -> parseHttpResponse(httpResponse, this::readBooleanFromHttpEntity));
                if (!locked.orElseThrow((Supplier<RuntimeException>) () -> new IllegalStateException("can't read boolean from consul"))) {
                    /* Avoid crazy spinning*/
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        LOGGER.error("error in consul repository for session " + sessionLocal.get() + " and key " + entryPointKey, e);
                    }
                } else {
                    LOGGER.debug("lock acquired for key {} on session {}", entryPointKey, sessionLocal.get());
                }
            }

        } catch (IOException e) {
            LOGGER.error("error in consul repository for session " + sessionLocal.get() + " and key " + entryPointKey, e);
        }
    }

    private Session createSession(EntryPointKey entryPointKey) throws IOException {
        return createSession(entryPointKey, 10, CONSUL_BEHAVIOR.RELEASE);
    }

    private Session createSession(EntryPointKey entryPointKey, Integer ttlInSec, CONSUL_BEHAVIOR behavior) throws IOException {
        HttpPut createSessionURI = new HttpPut("http://" + host + ":" + port + "/v1/session/create");
        if (ttlInSec != null) {
            String payload = "{\"Behavior\":\"" + behavior.value + "\",\"TTL\":\"" + ttlInSec + "s\", \"Name\":\"" + entryPointKey.getID() + "\"}";
            LOGGER.trace("create a consul session with theses options: {} ", payload);
            createSessionURI.setEntity(new StringEntity(payload));
        }
        Optional<Session> session = client.execute(createSessionURI, response -> parseHttpResponse(response, this::readSessionFromHttpEntity));
        if (session.isPresent()) {
            LOGGER.debug("get session {} for key {}", session.get().ID, entryPointKey);
        }
        return session.orElseThrow(() -> new IllegalStateException("can't get session"));
    }

    @Override
    public void release(EntryPointKey key) {
        try {
            LOGGER.debug("attempt to release lock for key " + key + " on session " + sessionLocal.get());
            HttpPut releaseEntryPointKeyURI = new HttpPut("http://" + host + ":" + port + "/v1/kv/admin/" + key.getID() + "/lock?release=" + sessionLocal.get());
            client.execute(releaseEntryPointKeyURI, httpResponse -> parseHttpResponse(httpResponse, this::readBooleanFromHttpEntity));
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
            return client.execute(getCurrentURI, httpResponse -> parseHttpResponseAccepting404(httpResponse, this::readEntryPointMappingJsonFromHttpEntity));
        } catch (IOException e) {
            LOGGER.error("error in consul repository", e);
            return Optional.empty();
        }
    }

    @Override
    public Set<String> getEntryPointsId() {
        try {
            HttpGet listKeysURI = new HttpGet("http://" + host + ":" + port + "/v1/kv/admin?keys");
            Optional<Set<String>> allKeys = client.execute(listKeysURI, httpResponse -> parseHttpResponse(httpResponse, this::readKeysFromHttpEntity));
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
            return client.execute(getPendingURI, httpResponse -> parseHttpResponseAccepting404(httpResponse, this::readEntryPointMappingJsonFromHttpEntity));
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
            return client.execute(getCommittingURI, response -> {
                Optional<CommittingConfigurationJson> result = Optional.empty();
                int status = response.getStatusLine().getStatusCode();
                if (status == 404) {
                    LOGGER.debug("configuration not found. Response is: " + EntityUtils.toString(response.getEntity()));
                } else if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();
                    result = Optional.of(mapper.readValue(entity.getContent(), CommittingConfigurationJson.class));
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
                return result;
            });
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

            client.execute(setPendingURI, httpResponse -> parseHttpResponse(httpResponse, this::readBooleanFromHttpEntity));
        } catch (IOException e) {
            LOGGER.error("error in consul repository", e);
        }
    }

    @Override
    public void removePendingConfiguration(EntryPointKey key) {
        try {
            HttpDelete deletePendingURI = new HttpDelete("http://" + host + ":" + port + "/v1/kv/admin/" + key.getID() + "/pending");
            client.execute(deletePendingURI, httpResponse -> parseHttpResponse(httpResponse, this::readBooleanFromHttpEntity));
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
            Session session = createSession(entryPointKey, ttl, CONSUL_BEHAVIOR.DELETE);

            HttpPut setCommittingURI = new HttpPut("http://" + host + ":" + port + "/v1/kv/admin/" + entryPointKey.getID() + "/committing?acquire=" + session.ID);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            mapper.writeValue(out, new CommittingConfigurationJson(correlationId, configuration));

            setCommittingURI.setEntity(new ByteArrayEntity(out.toByteArray()));

            client.execute(setCommittingURI, httpResponse -> parseHttpResponse(httpResponse, this::readBooleanFromHttpEntity));
        } catch (IOException e) {
            LOGGER.error("error in consul repository", e);
        }
    }

    @Override
    public void removeCommittingConfiguration(EntryPointKey key) {
        try {
            HttpDelete deleteCommittingURI = new HttpDelete("http://" + host + ":" + port + "/v1/kv/admin/" + key.getID() + "/committing");
            client.execute(deleteCommittingURI, httpResponse -> parseHttpResponse(httpResponse, this::readBooleanFromHttpEntity));
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

            client.execute(setCurrentURI, httpResponse -> parseHttpResponse(httpResponse, this::readBooleanFromHttpEntity));
        } catch (IOException e) {
            LOGGER.error("error in consul repository", e);
        }
    }

    @Override
    public Optional<Map<String, Integer>> getPorts() {
        try {
            HttpGet getPortsById = new HttpGet("http://" + host + ":" + port + "/v1/kv/ports");
            Optional<ConsulItem<Map<String, Integer>>> result = client.execute(getPortsById, httpResponse -> parseHttpResponse(httpResponse, this::readPortsByHaproxyFromHttpEntity));
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
            Optional<ConsulItem<Map<String, Integer>>> portsByEntrypoint = client.execute(getPortsById, httpResponse -> parseHttpResponse(httpResponse, this::readPortsByHaproxyFromHttpEntity));
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
                Optional<ConsulItem<Map<String, Integer>>> portsByEntrypoint = client.execute(getPortById, httpResponse -> parseHttpResponse(httpResponse, this::readPortsByHaproxyFromHttpEntity));
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
                failToPutNewPort = !client.execute(putPortById, httpResponse -> parseHttpResponse(httpResponse, this::readBooleanFromHttpEntity))
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
            return client.execute(getHaproxyURI, httpResponse -> parseHttpResponseAccepting404(httpResponse, this::readRawContentFromHttpEntity));
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
        public Session(@JsonProperty("ID") String ID) {
            this.ID = ID;
        }
    }

}