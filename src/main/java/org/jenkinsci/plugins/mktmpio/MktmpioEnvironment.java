package org.jenkinsci.plugins.mktmpio;

import hudson.model.InvisibleAction;

import java.io.Serializable;

public class MktmpioEnvironment extends InvisibleAction implements Serializable {
    private static final long serialVersionUID = 1L;
    public final String token;
    public final String id;
    public final String host;
    public final short port;
    public final String username;
    public final String password;
    public final String type;
    public final boolean shutdownWithBuild;

    public MktmpioEnvironment(final String token, final String id, final String host, final short port, final String username, final String password, final String type, final boolean shutdownWithBuild) {
        this.token = token;
        this.id = id;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.type = type;
        this.shutdownWithBuild = shutdownWithBuild;
    }
}
