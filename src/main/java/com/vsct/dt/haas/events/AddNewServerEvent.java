package com.vsct.dt.haas.events;

import com.vsct.dt.haas.state.Server;

/**
 * Created by william_montaz on 02/02/2016.
 */
public class AddNewServerEvent {
    private final String application;
    private final String platform;
    private final String backendName;
    private final Server server;

    public AddNewServerEvent(String application, String platform, String backendName, Server server) {
        this.application = application;
        this.platform = platform;
        this.backendName = backendName;
        this.server = server;
    }

    public String getApplication() {
        return application;
    }

    public String getPlatform() {
        return platform;
    }

    public String getBackendName() {
        return backendName;
    }

    public Server getServer() {
        return server;
    }
}
