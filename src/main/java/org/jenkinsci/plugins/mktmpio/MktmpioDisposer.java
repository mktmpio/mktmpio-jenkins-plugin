package org.jenkinsci.plugins.mktmpio;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.tasks.SimpleBuildWrapper.Disposer;

import java.io.IOException;

public class MktmpioDisposer extends Disposer {
    private static final long serialVersionUID = 1L;
    private final MktmpioEnvironment env;

    public MktmpioDisposer(final MktmpioEnvironment env) {
        this.env = env;
    }

    @Override
    public void tearDown(final Run<?, ?> run, final FilePath workspace, final Launcher launcher, final TaskListener listener) throws IOException, InterruptedException {
        if (!env.shutdownWithBuild) {
            Mktmpio.shutdownAndCleanup(env, launcher, listener);
        }
    }
}
