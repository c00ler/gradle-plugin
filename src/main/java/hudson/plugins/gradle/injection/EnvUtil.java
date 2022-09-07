package hudson.plugins.gradle.injection;

import hudson.EnvVars;

public final class EnvUtil {

    private EnvUtil() {
    }

    public static String getEnv(EnvVars env, String key) {
        return env != null ? env.get(key) : null;
    }
}
