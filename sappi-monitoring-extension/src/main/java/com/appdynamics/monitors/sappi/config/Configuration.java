package com.appdynamics.monitors.sappi.config;

public class Configuration {

    private String protocol;
    private String host;
    private int port;
    private String username;
    private String password;
    private String component;
    private String view;
    private String metricPathPrefix;
    
    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getComponent()
    {
    	return component;
    }
    
    public void setComponent(String component)
    {
    	this.component= component;
    }

    public String getView()
    {
    	return view;
    }
    
    public void setView(String view)
    {
    	this.view= view;
    }

    public String getMetricPathPrefix() {
        return metricPathPrefix;
    }

    public void setMetricPathPrefix(String metricPathPrefix) {
        this.metricPathPrefix = metricPathPrefix;
    }

}
