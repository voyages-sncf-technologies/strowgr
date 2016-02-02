package com.vsct.dt.haas.events;

/**
 * Created by william_montaz on 02/02/2016.
 */
public class AddNewServerEvent {
    private final String application;
    private final String platform;
    private final String backendName;
    private final String serverName;
    private final String ip;
    private final String port;

    public AddNewServerEvent(String application, String platform, String backendName, String serverName, String ip, String port) {
        this.application = application;
        this.platform = platform;
        this.backendName = backendName;
        this.serverName = serverName;
        this.ip = ip;
        this.port = port;
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

    public String getServerName() {
        return serverName;
    }

    public String getIp() {
        return ip;
    }

    public String getPort() {
        return port;
    }
}
