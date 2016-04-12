package com.vsct.dt.haas.admin.template.locator;

import com.vsct.dt.haas.admin.core.TemplateLocator;
import com.vsct.dt.haas.admin.core.configuration.EntryPoint;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class UriTemplateLocator implements TemplateLocator {
    private static final Logger LOGGER = LoggerFactory.getLogger(UriTemplateLocator.class);

    public static final String URI_FIELD = "templateUri";
    private final CloseableHttpClient client;

    public UriTemplateLocator() {
        this.client = HttpClients.createDefault();
    }

    @Override
    public String readTemplate(EntryPoint configuration) {
        try {
            String uri = configuration.getContext().get(URI_FIELD);
            HttpGet getTemplate = new HttpGet(uri);
            LOGGER.debug("get template {}", uri);
            return client.execute(getTemplate, (response) -> {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();
                    String entitySer = EntityUtils.toString(entity);
                    if (entitySer == null) {
                        throw new IllegalStateException("template from " + uri + " has null content.");
                    } else {
                        LOGGER.info("template from " + uri + " starts with " + entitySer.substring(0, Math.max(20, entitySer.length())));
                    }
                    return entitySer;
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            });
        } catch (IOException e) {
            LOGGER.error("Can't retrieve template from ", e);
            return null;
        }
    }
}
