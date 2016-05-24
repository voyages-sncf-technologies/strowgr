package com.vsct.dt.strowgr.admin.repository.consul;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vsct.dt.strowgr.admin.core.configuration.EntryPoint;
import com.vsct.dt.strowgr.admin.repository.consul.mapping.json.CommittingConfigurationJson;
import com.vsct.dt.strowgr.admin.repository.consul.mapping.json.EntryPointMappingJson;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Set of methods for parsing HttpEntity content.
 */
public class HttpEntityParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpEntityParser.class);

    private final ObjectMapper mapper;

    public HttpEntityParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }


    protected Optional<ConsulRepository.Session> parseSessionFromHttpEntity(HttpEntity httpEntity) {
        Optional<ConsulRepository.Session> result = Optional.empty();
        try {
            result = Optional.of(mapper.readValue(httpEntity.getContent(), ConsulRepository.Session.class));
        } catch (IOException e) {
            LOGGER.error("can't read session", e);
        }
        return result;
    }

    protected Optional<Boolean> parseBooleanFromHttpEntity(HttpEntity httpEntity) {
        Optional<Boolean> result = Optional.empty();
        try {
            result = Optional.of(Boolean.parseBoolean(EntityUtils.toString(httpEntity)));
        } catch (IOException e) {
            LOGGER.error("can't read boolean", e);
        }
        return result;
    }

    protected Optional<Set<String>> parseKeysFromHttpEntity(HttpEntity httpEntity) {
        Optional<Set<String>> result = Optional.empty();

        try {
            result = Optional.of(mapper.readValue(httpEntity.getContent(), new TypeReference<Set<String>>() {
            }));
        } catch (IOException e) {
            LOGGER.error("can't read keys", e);
        }
        return result;
    }

    protected Optional<EntryPoint> parseEntryPointMappingJsonFromHttpEntity(HttpEntity httpEntity) {
        Optional<EntryPoint> result = Optional.empty();
        try {
            result = Optional.of(mapper.readValue(httpEntity.getContent(), EntryPointMappingJson.class));
        } catch (IOException e) {
            LOGGER.error("can't read keys", e);
        }
        return result;
    }

    protected Optional<ConsulItem<Map<String, Integer>>> parsePortsByHaproxyFromHttpEntity(HttpEntity httpEntity) {
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

    protected Optional<String> readRawContentFromHttpEntity(HttpEntity httpEntity) {
        Optional<String> result = Optional.empty();
        try {
            result = Optional.of(EntityUtils.toString(httpEntity));
        } catch (IOException e) {
            LOGGER.error("can't convert HttpEntity to string", e);
        }
        return result;
    }

    protected Optional<CommittingConfigurationJson> parseCommittingConfigurationJson(HttpEntity entity) {
        Optional<CommittingConfigurationJson> result = Optional.empty();
        try {
            result = Optional.of(mapper.readValue(entity.getContent(), CommittingConfigurationJson.class));
        } catch (IOException e) {
            LOGGER.error("can't convert HttpEntity to string", e);
        }
        return result;
    }

}
