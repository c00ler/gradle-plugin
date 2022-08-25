package hudson.plugins.gradle.injection;

import hudson.EnvVars;

public class EnvUtil {

    public static String getEnv(EnvVars env, String key) {
        return env != null ? env.get(key) : null;
    }

}
