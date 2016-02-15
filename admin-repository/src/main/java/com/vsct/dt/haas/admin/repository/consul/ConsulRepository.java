package com.vsct.dt.haas.admin.repository.consul;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vsct.dt.haas.admin.core.EntryPointKey;
import com.vsct.dt.haas.admin.core.EntryPointRepository;
import com.vsct.dt.haas.admin.core.configuration.EntryPointConfiguration;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ConsulRepository implements EntryPointRepository {

    private final String host;
    private final int port;

    private final CloseableHttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    private ThreadLocal<String> sessionLocal = new ThreadLocal<>();

    public ConsulRepository(String host, int port) {
        this.host = host;
        this.port = port;
        this.client = HttpClients.createDefault();
    }

    ResponseHandler<Session> createSessionResponseHandler = response -> {
        int status = response.getStatusLine().getStatusCode();
        if (status >= 200 && status < 300) {
            HttpEntity entity = response.getEntity();
            return mapper.readValue(entity.getContent(), Session.class);
        } else {
            throw new ClientProtocolException("Unexpected response status: " + status);
        }
    };
    ResponseHandler<Boolean> destroySessionResponseHandler = response -> {
        int status = response.getStatusLine().getStatusCode();
        if (status >= 200 && status < 300) {
            HttpEntity entity = response.getEntity();
            return Boolean.parseBoolean(EntityUtils.toString(entity));
        } else {
            throw new ClientProtocolException("Unexpected response status: " + status);
        }
    };
    ResponseHandler<Boolean> acquireEntryPointResponseHandler = response -> {
        int status = response.getStatusLine().getStatusCode();
        if (status >= 200 && status < 300) {
            HttpEntity entity = response.getEntity();
            return Boolean.parseBoolean(EntityUtils.toString(entity));
        } else {
            throw new ClientProtocolException("Unexpected response status: " + status);
        }
    };
    ResponseHandler<Boolean> releaseEntryPointResponseHandler = response -> {
        int status = response.getStatusLine().getStatusCode();
        if (status >= 200 && status < 300) {
            HttpEntity entity = response.getEntity();
            return Boolean.parseBoolean(EntityUtils.toString(entity));
        } else {
            throw new ClientProtocolException("Unexpected response status: " + status);
        }
    };
    ResponseHandler<Optional<EntryPointConfiguration>> getConfigurationResponseHandler = response -> {
        int status = response.getStatusLine().getStatusCode();
        if (status == 404) return Optional.empty();
        if (status >= 200 && status < 300) {
            HttpEntity entity = response.getEntity();
            return Optional.of(mapper.readValue(entity.getContent(), EntryPointConfigurationJsonRepresentation.class));
        } else {
            throw new ClientProtocolException("Unexpected response status: " + status);
        }
    };
    ResponseHandler<Boolean> setConfigurationResponseHandler = response -> {
        int status = response.getStatusLine().getStatusCode();
        if (status >= 200 && status < 300) {
            HttpEntity entity = response.getEntity();
            return Boolean.parseBoolean(EntityUtils.toString(entity));
        } else {
            throw new ClientProtocolException("Unexpected response status: " + status);
        }
    };
    ResponseHandler<Boolean> deleteConfigurationResponseHandler = response -> {
        int status = response.getStatusLine().getStatusCode();
        if (status >= 200 && status < 300) {
            HttpEntity entity = response.getEntity();
            return Boolean.parseBoolean(EntityUtils.toString(entity));
        } else {
            throw new ClientProtocolException("Unexpected response status: " + status);
        }
    };
    ResponseHandler<Set<String>> listKeysResponseHandler = response -> {
        int status = response.getStatusLine().getStatusCode();
        if (status >= 200 && status < 300) {
            HttpEntity entity = response.getEntity();
            return mapper.readValue(entity.getContent(), new TypeReference<Set<String>>() {
            });
        } else {
            throw new ClientProtocolException("Unexpected response status: " + status);
        }
    };

    @Override
    public void lock(EntryPointKey key) {
        try {
            HttpPut createSessionURI = new HttpPut("http://" + host + ":" + port + "/v1/session/create");
            Session session = client.execute(createSessionURI, createSessionResponseHandler);
            sessionLocal.set(session.ID);

            HttpPut acquireEntryPointKeyURI = new HttpPut("http://" + host + ":" + port + "/v1/kv/admin/" + key.getID() + "/lock?acquire=" + sessionLocal.get());

            /* TODO, implement wait with a blocking query */
            boolean locked = false;
            while (!locked) {
                locked = client.execute(acquireEntryPointKeyURI, acquireEntryPointResponseHandler);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void release(EntryPointKey key) {
        try {
            HttpPut releaseEntryPointKeyURI = new HttpPut("http://" + host + ":" + port + "/v1/kv/admin/" + key.getID() + "/lock?release=" + sessionLocal.get());
            client.execute(releaseEntryPointKeyURI, releaseEntryPointResponseHandler);

            HttpPut destroySessionURI = new HttpPut("http://" + host + ":" + port + "/v1/session/destroy/" + sessionLocal.get());
            client.execute(destroySessionURI, destroySessionResponseHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Optional<EntryPointConfiguration> getCurrentConfiguration(EntryPointKey key) {
        try {
            HttpGet getCurrentURI = new HttpGet("http://" + host + ":" + port + "/v1/kv/admin/" + key.getID() + "/current?raw");
            return client.execute(getCurrentURI, getConfigurationResponseHandler);
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public Set<String> getEntryPointsId() {
        try {
            HttpGet listKeysURI = new HttpGet("http://" + host + ":" + port + "/v1/kv/admin?keys");
            Set<String> allKeys = client.execute(listKeysURI, listKeysResponseHandler);
            return allKeys.stream()
                    .filter(s -> !s.contains("lock"))
                    .map(s -> s.replace("admin/", ""))
                    .map(s -> s.replace("/lock", ""))
                    .map(s -> s.replace("/current", ""))
                    .map(s -> s.replace("/pending", ""))
                    .map(s -> s.replace("/committing", ""))
                    .distinct()
                    .collect(Collectors.toSet());

        } catch (IOException e) {
            e.printStackTrace();
            return new HashSet<>();
        }
    }

    @Override
    public Optional<EntryPointConfiguration> getPendingConfiguration(EntryPointKey key) {
        try {
            HttpGet getPendingURI = new HttpGet("http://" + host + ":" + port + "/v1/kv/admin/" + key.getID() + "/pending?raw");
            return client.execute(getPendingURI, getConfigurationResponseHandler);
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public Optional<EntryPointConfiguration> getCommittingConfiguration(EntryPointKey key) {
        try {
            HttpGet getCommittingURI = new HttpGet("http://" + host + ":" + port + "/v1/kv/admin/" + key.getID() + "/committing?raw");
            return client.execute(getCommittingURI, getConfigurationResponseHandler);
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public void setPendingConfiguration(EntryPointKey key, EntryPointConfiguration configuration) {
        try {
            HttpPut setPendingURI = new HttpPut("http://" + host + ":" + port + "/v1/kv/admin/" + key.getID() + "/pending");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            mapper.writeValue(out, configuration);

            setPendingURI.setEntity(new ByteArrayEntity(out.toByteArray()));

            client.execute(setPendingURI, setConfigurationResponseHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removePendingConfiguration(EntryPointKey key) {
        try {
            HttpDelete deletePendingURI = new HttpDelete("http://" + host + ":" + port + "/v1/kv/admin/" + key.getID() + "/pending");
            client.execute(deletePendingURI, deleteConfigurationResponseHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setCommittingConfiguration(EntryPointKey key, EntryPointConfiguration configuration) {
        try {
            HttpPut setCommittingURI = new HttpPut("http://" + host + ":" + port + "/v1/kv/admin/" + key.getID() + "/committing");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            mapper.writeValue(out, configuration);

            setCommittingURI.setEntity(new ByteArrayEntity(out.toByteArray()));

            client.execute(setCommittingURI, setConfigurationResponseHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeCommittingConfiguration(EntryPointKey key) {
        try {
            HttpDelete deleteCommittingURI = new HttpDelete("http://" + host + ":" + port + "/v1/kv/admin/" + key.getID() + "/committing");
            client.execute(deleteCommittingURI, deleteConfigurationResponseHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setCurrentConfiguration(EntryPointKey key, EntryPointConfiguration configuration) {
        try {
            HttpPut setCurrentURI = new HttpPut("http://" + host + ":" + port + "/v1/kv/admin/" + key.getID() + "/current");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            mapper.writeValue(out, configuration);

            setCurrentURI.setEntity(new ByteArrayEntity(out.toByteArray()));

            client.execute(setCurrentURI, setConfigurationResponseHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void shutdown(){
        try {
            this.client.close();
        } catch (IOException e) {
            e.printStackTrace();
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
