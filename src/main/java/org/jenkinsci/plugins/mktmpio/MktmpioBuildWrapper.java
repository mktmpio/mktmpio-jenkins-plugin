package org.jenkinsci.plugins.mktmpio;

import hudson.*;
import hudson.console.ConsoleNote;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildWrapper;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;

public class MktmpioBuildWrapper extends SimpleBuildWrapper {

    // Job config
    private String instanceType;
    private boolean shutdownWithBuild = false;

    @DataBoundConstructor
    public MktmpioBuildWrapper(String instanceType) {
        this.instanceType = instanceType;
    }

    static void shutdownAndCleanup(final MktmpioEnvironment env, final Launcher launcher, final TaskListener listener) throws IOException, InterruptedException {
        final String instanceID = env.id;
        MktmpioInstance instance = new MktmpioInstance(env);
        instance.destroy();
        listener.getLogger().printf("mktmpio instance shutdown. type: %s, host: %s, port: %d\n", env.type, env.host, env.port);
    }

    public String getInstanceType() {
        return instanceType;
    }

    @DataBoundSetter
    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    public boolean isShutdownWithBuild() {
        return shutdownWithBuild;
    }

    @DataBoundSetter
    public void setShutdownWithBuild(boolean shutdownWithBuild) {
        this.shutdownWithBuild = shutdownWithBuild;
    }

    @Override
    public MktmpioBuildWrapperDescriptor getDescriptor() {
        return (MktmpioBuildWrapperDescriptor) super.getDescriptor();
    }

    @Override
    public void setUp(SimpleBuildWrapper.Context context,
                      Run<?, ?> build,
                      FilePath workspace,
                      Launcher launcher,
                      TaskListener listener,
                      EnvVars initialEnvironment)
            throws IOException, InterruptedException {
        final String token =  getDescriptor().getToken();
        final String baseUrl = getDescriptor().getMktmpioServer();
        final String type = getInstanceType();
        final MktmpioInstance instance;
        try {
            listener.getLogger().printf("Attempting to create instance (server: %s, token: %s, type: %s)",
                                        baseUrl, token.replaceAll(".", "*"), type);
            instance = MktmpioInstance.create(baseUrl, token, type, isShutdownWithBuild());
        } catch (IOException ex) {
            listener.fatalError("mktmpio: " + ex.getMessage());
            throw new InterruptedException(ex.getMessage());
        }
        final MktmpioEnvironment env = instance.getEnv();
        listener.hyperlink("https://mktmp.io/i/" + env.id, env.type + " instance " + env.id);
        listener.getLogger().printf("mktmpio instance created. type: %s, host: %s, port: %d\n", env.type, env.host, env.port);
        build.addAction(env);
        context.env("MKTMPIO_HOST", env.host);
        context.env("MKTMPIO_PORT", String.valueOf(env.port));
        context.setDisposer(new MktmpioDisposer(env));
    }

    @Extension
    public static final class MktmpioBuildWrapperDescriptor extends BuildWrapperDescriptor {

        @CopyOnWrite
        private String token;
        @CopyOnWrite
        private String mktmpioServer = "https://mktmp.io";

        public MktmpioBuildWrapperDescriptor() {
            load();
        }

        @Override
        public boolean configure(final StaplerRequest req, final JSONObject formData) {
            token = formData.getString("token");
            mktmpioServer = formData.optString("mktmpioServer", "https://mktmp.io");
            save();
            return true;
        }

        @Override
        public BuildWrapper newInstance(final StaplerRequest req, final JSONObject formData) throws hudson.model.Descriptor.FormException {
            return req.bindJSON(MktmpioBuildWrapper.class, formData);
        }

        public String getToken() {
            return token;
        }

        @DataBoundSetter
        public void setToken(String token) {
            this.token = token;
        }

        public String getMktmpioServer() {
            return mktmpioServer;
        }

        @DataBoundSetter
        public void setMktmpioServer(String mktmpioServer) {
            this.mktmpioServer = mktmpioServer;
        }

        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        public String getDisplayName() {
            return "Temporary databases by mktmpio";
        }

        public ListBoxModel doFillInstanceTypeItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("MySQL", "mysql");
            items.add("PostgreSQL-9.4", "postgres");
            items.add("PostgreSQL-9.5", "postgres-9.5");
            items.add("Redis", "redis");
            items.add("MongoDB", "mongodb");
            return items;
        }
    }
}
