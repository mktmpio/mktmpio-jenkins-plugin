package org.jenkinsci.plugins.mktmpio;

import org.jenkinsci.plugins.workflow.JenkinsRuleExt;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.CoreWrapperStep;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.jenkinsci.plugins.workflow.test.steps.SemaphoreStep;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.model.Statement;
import org.jvnet.hudson.test.RestartableJenkinsRule;

public class MktmpioWorkflowTest extends MktmpioBaseTest {

    @Rule
    public RestartableJenkinsRule restartableSystem = new RestartableJenkinsRule();

    @Before
    public void configureMock() {
        prepareFakeInstance("totally-legit-token", "redis");
    }

    public void configureMktmpio() {
        getConfig().setToken("totally-legit-token");
        getConfig().setServer(mockedServer());
    }

    @Test
    public void configurationShouldRoundTrip() {
        restartableSystem.addStep(new Statement() {

            @Override
            public void evaluate() throws Throwable {
                final Mktmpio mktmpio = new Mktmpio("redis");

                final CoreWrapperStep wrapperStep = new CoreWrapperStep(mktmpio);

                final CoreWrapperStep testerStep = new StepConfigTester(restartableSystem.j)
                        .configRoundTrip(wrapperStep);

                restartableSystem.j.assertEqualDataBoundBeans(mktmpio, testerStep.getDelegate());
            }
        });
    }

    @Test
    public void shouldAllowWorkflowRestarts() {
        restartableSystem.addStep(new Statement() {

            @Override
            public void evaluate() throws Throwable {
                configureMktmpio();
                final WorkflowJob workflowJob = restartableSystem.j.jenkins.createProject(WorkflowJob.class,
                        "shouldAllowWorkflowRestarts");

                workflowJob.setDefinition(new CpsFlowDefinition(""
                        + "node {\n"
                        + "  wrap([$class: 'Mktmpio', dbType: 'redis']) {\n"
                        + "    semaphore 'shouldAllowWorkflowRestarts'\n"
                        + "    sh 'echo REDIS_HOST=$MKTMPIO_HOST'\n"
                        + "  }\n"
                        + "}", true));

                final WorkflowRun workflowRun = workflowJob.scheduleBuild2(0).waitForStart();

                SemaphoreStep.waitForStart("shouldAllowWorkflowRestarts/1", workflowRun);
            }

        });

        restartableSystem.addStep(new Statement() {

            @Override
            public void evaluate() throws Throwable {

                SemaphoreStep.success("shouldAllowWorkflowRestarts/1", null);

                final WorkflowJob workflowProject = restartableSystem.j.jenkins.getItemByFullName(
                        "shouldAllowWorkflowRestarts", WorkflowJob.class);

                final WorkflowRun workflowRun = workflowProject.getBuildByNumber(1);

                restartableSystem.j.assertBuildStatusSuccess(JenkinsRuleExt.waitForCompletion(workflowRun));

                restartableSystem.j.assertLogContains("REDIS_HOST=12.34.56.78", workflowRun);
            }

        });
    }

    @Test
    public void shouldSupportWorkflow() {
        restartableSystem.addStep(new Statement() {

            @Override
            public void evaluate() throws Throwable {
                configureMktmpio();

                final WorkflowJob workflowJob = restartableSystem.j.jenkins.createProject(WorkflowJob.class,
                        "shouldSupportWorkflow");

                workflowJob.setDefinition(new CpsFlowDefinition(""
                        + "node {\n"
                        + "  wrap([$class: 'Mktmpio', dbType: 'redis']) {\n"
                        + "    sh 'echo REDIS_HOST=$MKTMPIO_HOST'\n"
                        + "  }\n"
                        + "}", true));

                final WorkflowRun workflowRun = workflowJob.scheduleBuild2(0).waitForStart();

                restartableSystem.j.assertBuildStatusSuccess(JenkinsRuleExt.waitForCompletion(workflowRun));

                restartableSystem.j.assertLogContains("REDIS_HOST=12.34.56.78", workflowRun);
            }

        });
    }

    private Mktmpio.MktmpioDescriptor getConfig() {
        return (Mktmpio.MktmpioDescriptor) restartableSystem.j.jenkins.getDescriptor(Mktmpio.class);
    }
}
