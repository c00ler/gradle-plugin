package hudson.plugins.gradle.injection;

import hudson.FilePath;
import hudson.Util;

import java.io.IOException;
import java.io.InputStream;

public final class CopyUtil {

    private CopyUtil() {
    }

    public static void copyResourceToNode(FilePath nodePath, String resourceName) throws IOException, InterruptedException {
        doWithResource(resourceName, is -> {
            nodePath.copyFrom(is);
            return null;
        });
    }

    public static String resourceDigest(String resourceName) throws IOException, InterruptedException {
        return doWithResource(resourceName, Util::getDigestOf);
    }

    private static <T> T doWithResource(String resourceName, RemoteFunction<InputStream, T> action) throws IOException, InterruptedException {
        try (InputStream is = CopyUtil.class.getResourceAsStream("/hudson/plugins/gradle/injection/" + resourceName)) {
            if (is == null) {
                throw new IllegalStateException("Could not find resource: " + resourceName);
            }
            return action.apply(is);
        }
    }

    @FunctionalInterface
    private interface RemoteFunction<T, R> {

        R apply(T t) throws IOException, InterruptedException;
    }
}
