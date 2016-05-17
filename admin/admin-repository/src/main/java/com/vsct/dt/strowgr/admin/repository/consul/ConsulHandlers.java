package com.vsct.dt.strowgr.admin.repository.consul;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsulHandlers {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulHandlers.class);

    private final ObjectMapper mapper = new ObjectMapper();

    ResponseHandler<ConsulRepository.Session> createSessionResponseHandler = response -> {
        int status = response.getStatusLine().getStatusCode();
        HttpEntity entity = response.getEntity();
        if (status >= 200 && status < 300) {
            return mapper.readValue(entity.getContent(), ConsulRepository.Session.class);
        } else {
            String content = "no content";
            if (entity != null) {
                content = EntityUtils.toString(entity);
            }
            throw new ClientProtocolException("Unexpected response status: " + status + ": " + response.getStatusLine().getReasonPhrase() + ", entity is " + content);
        }
    };

    ResponseHandler<Boolean> acquireEntryPointResponseHandler = response -> {
        int status = response.getStatusLine().getStatusCode();
        if (status >= 200 && status < 300) {
            HttpEntity entity = response.getEntity();
            boolean acquired = Boolean.parseBoolean(EntityUtils.toString(entity));
            LOGGER.trace("acquire for entrypoint: {}", acquired);
            return acquired;
        } else {
            throw new ClientProtocolException("Unexpected response status: " + status);
        }
    };
}
