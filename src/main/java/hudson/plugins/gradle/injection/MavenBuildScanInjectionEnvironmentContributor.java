package hudson.plugins.gradle.injection;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.EnvironmentContributor;
import hudson.model.Executor;
import hudson.model.InvisibleAction;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.gradle.GradleLogger;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Extension
public class MavenBuildScanInjectionEnvironmentContributor extends EnvironmentContributor {

    private static final Logger LOGGER = Logger.getLogger(MavenBuildScanInjectionEnvironmentContributor.class.getName());

    @Override
    public void buildEnvironmentFor(@Nonnull Run run, @Nonnull EnvVars envs, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        Executor executor = run.getExecutor();
        if (executor == null) {
            LOGGER.log(Level.FINE, "Executor is null");
            return;
        }

        MavenInjectionEnvironmentAction action = run.getAction(MavenInjectionEnvironmentAction.class);
        if (action != null) {
            EnvVars mavenInjectionEnvironment = action.getEnvVars();
            if (mavenInjectionEnvironment != null) {
                envs.putAll(mavenInjectionEnvironment);
            }
            return;
        }

        Computer computer = executor.getOwner();

        Node node = computer.getNode();
        if (node == null) {
            LOGGER.log(Level.FINE, "Node is null");
            return;
        }

        FilePath rootPath = node.getRootPath();
        if (rootPath == null) {
            LOGGER.log(Level.FINE, "Root path is null");
            return;
        }

        try {
            EnvVars mavenInjectionEnvironment = EnvFileUtil.read(rootPath);
            if (mavenInjectionEnvironment == null) {
                run.addAction(MavenInjectionEnvironmentAction.EMPTY);

                LOGGER.log(Level.FINE, "No environment to inject");
                return;
            }

            run.addAction(new MavenInjectionEnvironmentAction(mavenInjectionEnvironment));
            envs.putAll(mavenInjectionEnvironment);

            GradleLogger gradleLogger = new GradleLogger(listener);
            gradleLogger.info("The following environment variables were added to the run: " + asString(mavenInjectionEnvironment));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unable to read maven build scan injection .env file", e);
        }
    }

    private String asString(EnvVars envVars) {
        return envVars.entrySet()
            .stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining(", "));
    }

    public static class MavenInjectionEnvironmentAction extends InvisibleAction {

        public static final MavenInjectionEnvironmentAction EMPTY = new MavenInjectionEnvironmentAction(null);

        private final EnvVars envVars;

        public MavenInjectionEnvironmentAction(EnvVars envVars) {
            this.envVars = envVars;
        }

        @CheckForNull
        @Nullable
        public EnvVars getEnvVars() {
            return envVars;
        }
    }
}
