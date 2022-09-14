package hudson.plugins.gradle.injection;

import hudson.EnvVars;
import hudson.model.Node;
import hudson.slaves.EnvironmentVariablesNodeProperty;

public final class EnvUtil {

    private EnvUtil() {
    }

    public static boolean isSet(EnvVars env, String key) {
        return getEnv(env, key) != null;
    }

    public static String getEnv(EnvVars env, String key) {
        return env != null ? env.get(key) : null;
    }

    public static void setEnvVar(Node node, String key, String value) {
        EnvironmentVariablesNodeProperty environmentVariablesNodeProperty =
            new EnvironmentVariablesNodeProperty(new EnvironmentVariablesNodeProperty.Entry(key, value));

        node.getNodeProperties().add(environmentVariablesNodeProperty);
    }
}
