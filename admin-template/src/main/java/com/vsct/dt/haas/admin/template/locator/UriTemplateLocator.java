package com.vsct.dt.haas.admin.template.locator;

import com.vsct.dt.haas.admin.core.TemplateLocator;
import com.vsct.dt.haas.admin.core.configuration.EntryPointConfiguration;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

public class UriTemplateLocator implements TemplateLocator {

    private static final String uriField = "templateUri";
    private final CloseableHttpClient client;

    public UriTemplateLocator() {
        this.client = HttpClients.createDefault();
    }

    @Override
    public String readTemplate(EntryPointConfiguration configuration) {
        try {
            HttpGet getTemplate = new HttpGet(configuration.getContext().get(uriField));
            return client.execute(getTemplate, (response) -> {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();
                    return EntityUtils.toString(entity);
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
