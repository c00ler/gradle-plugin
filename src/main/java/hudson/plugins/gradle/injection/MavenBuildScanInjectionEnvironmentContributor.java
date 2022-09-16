package hudson.plugins.gradle.injection;

import com.google.common.base.Splitter;
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
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Extension
public class MavenBuildScanInjectionEnvironmentContributor extends EnvironmentContributor {

    private static final Logger LOGGER = Logger.getLogger(MavenBuildScanInjectionEnvironmentContributor.class.getName());

    private static final Splitter LINE_SPLITTER = Splitter.on(System.lineSeparator()).omitEmptyStrings().trimResults();

    @Override
    public void buildEnvironmentFor(@Nonnull Run r, @Nonnull EnvVars envs, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        Executor executor = r.getExecutor();
        if (executor == null) {
            LOGGER.log(Level.FINE, "Executor is null");
            return;
        }

        MavenInjectionEnvironmentAction action = r.getAction(MavenInjectionEnvironmentAction.class);
        if (action != null) {
            EnvVars mavenInjectionEnvironment = action.getEnvVars();
            if (mavenInjectionEnvironment == null) {
                return; // nothing needs to be done
            }

            envs.putAll(mavenInjectionEnvironment);
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
            FilePath envFile = rootPath.child(MavenExtensionsHandler.LIB_DIR_PATH).child(MavenExtensionsHandler.ENV_FILE);
            if (!envFile.exists()) {
                r.addAction(MavenInjectionEnvironmentAction.EMPTY);

                LOGGER.log(Level.FINE, "No environment to inject");
                return;
            }

            String envFileContent = envFile.readToString();
            EnvVars mavenInjectionEnvironment = parseEnvFile(envFileContent);

            r.addAction(new MavenInjectionEnvironmentAction(mavenInjectionEnvironment));
            envs.putAll(mavenInjectionEnvironment);

            GradleLogger gradleLogger = new GradleLogger(listener);
            gradleLogger.info("The following environment variables were added to the run: " + asString(mavenInjectionEnvironment));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unable to read maven build scan injection .env file", e);
        }
    }

    private EnvVars parseEnvFile(String envFileContent) {
        EnvVars envVars = new EnvVars();

        LINE_SPLITTER.split(envFileContent).forEach(envVars::addLine);

        return envVars;
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
        public EnvVars getEnvVars() {
            return envVars;
        }
    }
}
