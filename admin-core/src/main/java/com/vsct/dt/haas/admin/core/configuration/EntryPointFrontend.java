package com.vsct.dt.haas.admin.core.configuration;

import com.vsct.dt.haas.admin.Preconditions;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class EntryPointFrontend {

    private final String id;
    private final HashMap<String, String> context;

    public EntryPointFrontend(String id, Map<String, String> context) {
        Preconditions.checkStringNotEmpty(id, "Frontend should have an id");
        checkNotNull(context);
        this.id = id;
        this.context = new HashMap<>(context);
    }

    public String getId() {
        return id;
    }

    public Map<String, String> getContext() {
        return new HashMap<>(context);
    }

    public String portId() {
        return this.id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EntryPointFrontend that = (EntryPointFrontend) o;

        if (context != null ? !context.equals(that.context) : that.context != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (context != null ? context.hashCode() : 0);
        return result;
    }

}
