package com.vsct.dt.haas.events;

/**
 * Created by william_montaz on 02/02/2016.
 */
public class UpdateEntryPointEvent {
    private final String application;
    private final String platform;

    public UpdateEntryPointEvent(String application, String platform) {
        this.application = application;
        this.platform = platform;
    }

    public String getApplication() {
        return application;
    }

    public String getPlatform() {
        return platform;
    }
}
