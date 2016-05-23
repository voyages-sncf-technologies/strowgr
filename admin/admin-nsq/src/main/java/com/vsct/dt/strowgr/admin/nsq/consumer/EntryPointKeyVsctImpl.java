package com.vsct.dt.strowgr.admin.nsq.consumer;

import com.vsct.dt.strowgr.admin.core.EntryPointKey;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An entry point is defined by application and platform
 */
public class EntryPointKeyVsctImpl implements EntryPointKey {

    private final String application;
    private final String platform;

    public EntryPointKeyVsctImpl(String application, String platform) {
        checkNotNull(application);
        checkNotNull(platform);
        checkArgument(application.length() > 0, "Application should not be empty");
        checkArgument(platform.length() > 0, "Platform should not be empty");
        this.application = application;
        this.platform = platform;
    }

    public String getApplication() {
        return application;
    }

    public String getPlatform() {
        return platform;
    }

    @Override
    public String getID() {
        return application + "/" + platform;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EntryPointKeyVsctImpl that = (EntryPointKeyVsctImpl) o;

        boolean applicationEq = (application != null ?
                application.equals(that.application) :
                that.application == null);
        boolean platformEq = (platform != null ?
                platform.equals(that.platform) :
                that.platform == null);

        return applicationEq && platformEq;

    }

    @Override
    public int hashCode() {
        int result = application != null ? application.hashCode() : 0;
        result = 31 * result + (platform != null ? platform.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return getID();
    }
}
