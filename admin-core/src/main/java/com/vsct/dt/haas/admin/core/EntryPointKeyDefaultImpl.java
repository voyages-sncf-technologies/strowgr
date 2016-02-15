package com.vsct.dt.haas.admin.core;

/**
 * Created by william_montaz on 09/02/2016.
 */
public class EntryPointKeyDefaultImpl implements EntryPointKey {

    private final String id;

    public EntryPointKeyDefaultImpl(String id) {
        this.id = id;
    }

    @Override
    public String getID() {
        return this.id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EntryPointKeyDefaultImpl that = (EntryPointKeyDefaultImpl) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
