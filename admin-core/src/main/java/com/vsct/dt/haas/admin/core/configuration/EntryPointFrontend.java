package com.vsct.dt.haas.admin.core.configuration;

import com.vsct.dt.haas.admin.Preconditions;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class EntryPointFrontend {

    private final String id;
    private final String port;

    private final HashMap<String, String> context;

    public EntryPointFrontend(String id, String port, Map<String, String> context) {
        Preconditions.checkStringNotEmpty(id, "Frontend should have an id");
        Preconditions.checkStringNotEmpty(port, "Frontend should have a port");
        checkNotNull(context);
        this.id = id;
        this.port = port;
        this.context = new HashMap<>(context);
    }

    public String getId() {
        return id;
    }

    public String getPort() {
        return port;
    }

    public Map<String, String> getContext() {
        return new HashMap<>(context);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EntryPointFrontend that = (EntryPointFrontend) o;

        if (context != null ? !context.equals(that.context) : that.context != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (port != null ? !port.equals(that.port) : that.port != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (port != null ? port.hashCode() : 0);
        result = 31 * result + (context != null ? context.hashCode() : 0);
        return result;
    }
}
