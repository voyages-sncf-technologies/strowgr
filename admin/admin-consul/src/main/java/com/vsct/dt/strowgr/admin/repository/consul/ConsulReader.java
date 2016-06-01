package com.vsct.dt.strowgr.admin.repository.consul;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vsct.dt.strowgr.admin.core.configuration.EntryPoint;
import com.vsct.dt.strowgr.admin.repository.consul.mapping.json.CommittingConfigurationJson;
import com.vsct.dt.strowgr.admin.repository.consul.mapping.json.EntryPointMappingJson;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Set of methods for parsing HttpEntity content.
 */
class ConsulReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulReader.class);

    private final ObjectMapper mapper;

    ConsulReader(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    <T> Optional<T> parseHttpResponse(HttpResponse httpResponse, Function<HttpEntity, Optional<T>> method) throws ClientProtocolException {
        return parseHttpResponse(httpResponse, method, false);
    }

    <T> Optional<T> parseHttpResponseAccepting404(HttpResponse httpResponse, Function<HttpEntity, Optional<T>> method) throws ClientProtocolException {
        return parseHttpResponse(httpResponse, method, true);
    }

    /**
     * Read HttpResponse into HttpEntity and apply method with the result.
     * If result http status is not between 200 and 299, an exception is raised
     *
     * @param httpResponse response to read
     * @param method       method to apply to the read result
     * @param accept404    whether to return or not an empty result if a 404 occurrs
     * @param <T>          Type of the method application
     * @return the result of the method application
     * @throws ClientProtocolException thrown if the http status is not between 200 and 299 including
     */
    private <T> Optional<T> parseHttpResponse(HttpResponse httpResponse, Function<HttpEntity, Optional<T>> method, boolean accept404) throws ClientProtocolException {
        int status = httpResponse.getStatusLine().getStatusCode();
        HttpEntity entity = httpResponse.getEntity();
        Optional<T> result = Optional.empty();
        if (status >= 200 && status < 300) {
            result = method.apply(entity);
        } else if (status != 404 || !accept404){
            String content = Optional.ofNullable(entity).map(myEntity -> {
                try {
                    return EntityUtils.toString(myEntity);
                } catch (IOException e) {
                    LOGGER.error("can't parse content from consul", e);
                    return null;
                }
            }).orElse("no content");
            throw new ClientProtocolException("Unexpected response status: " + status + ": " + httpResponse.getStatusLine().getReasonPhrase() + ", entity is " + content);
        }
        return result;
    }


    Optional<ConsulRepository.Session> parseSessionFromHttpEntity(HttpEntity httpEntity) {
        Optional<ConsulRepository.Session> result = Optional.empty();
        try {
            result = Optional.of(mapper.readValue(httpEntity.getContent(), ConsulRepository.Session.class));
        } catch (IOException e) {
            LOGGER.error("can't read session", e);
        }
        return result;
    }

    Optional<Boolean> parseBooleanFromHttpEntity(HttpEntity httpEntity) {
        Optional<Boolean> result = Optional.empty();
        try {
            result = Optional.of(Boolean.parseBoolean(EntityUtils.toString(httpEntity)));
        } catch (IOException e) {
            LOGGER.error("can't read boolean", e);
        }
        return result;
    }

    Optional<Set<String>> parseKeysFromHttpEntity(HttpEntity httpEntity) {
        Optional<Set<String>> result = Optional.empty();

        try {
            result = Optional.of(mapper.readValue(httpEntity.getContent(), new TypeReference<Set<String>>() {
            }));
        } catch (IOException e) {
            LOGGER.error("can't read keys", e);
        }
        return result;
    }

    Optional<EntryPoint> parseEntryPointMappingJsonFromHttpEntity(HttpEntity httpEntity) {
        Optional<EntryPoint> result = Optional.empty();
        try {
            result = Optional.of(mapper.readValue(httpEntity.getContent(), EntryPointMappingJson.class));
        } catch (IOException e) {
            LOGGER.error("can't read keys", e);
        }
        return result;
    }

    Optional<ConsulItem<Map<String, Integer>>> parsePortsByHaproxyFromHttpEntity(HttpEntity httpEntity) {
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

    Optional<String> readRawContentFromHttpEntity(HttpEntity httpEntity) {
        Optional<String> result = Optional.empty();
        try {
            result = Optional.of(EntityUtils.toString(httpEntity));
        } catch (IOException e) {
            LOGGER.error("can't convert HttpEntity to string", e);
        }
        return result;
    }

    Optional<CommittingConfigurationJson> parseCommittingConfigurationJson(HttpEntity entity) {
        Optional<CommittingConfigurationJson> result = Optional.empty();
        try {
            result = Optional.of(mapper.readValue(entity.getContent(), CommittingConfigurationJson.class));
        } catch (IOException e) {
            LOGGER.error("can't convert HttpEntity to string", e);
        }
        return result;
    }

}