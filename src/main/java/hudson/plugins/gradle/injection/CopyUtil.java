package hudson.plugins.gradle.injection;

import hudson.FilePath;
import hudson.ProxyConfiguration;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public final class CopyUtil {

    private static final String MAVEN_CENTRAL_URL_TEMPLATE = "https://repo1.maven.org/maven2/com/gradle/%1$s/%2$s/%1$s-%2$s.jar";

    private CopyUtil() {
    }

    public static void copyClasspathResourceToNode(FilePath nodePath, String resourceName) throws IOException, InterruptedException {
        try (InputStream is = CopyUtil.class.getResourceAsStream("/hudson/plugins/gradle/injection/" + resourceName)) {
            if (is == null) {
                throw new IllegalStateException("Could not find resource: " + resourceName);
            }
            nodePath.copyFrom(is);
        }
    }

    public static void copyMavenCentralResourceToNode(FilePath nodePath, String resourceName) throws IOException, InterruptedException {
        String artifact = StringUtils.substringBeforeLast(resourceName, "-");
        String version = StringUtils.substringAfterLast(StringUtils.removeEnd(resourceName, ".jar"), "-");

        String urlSpec = String.format(MAVEN_CENTRAL_URL_TEMPLATE, artifact, version);
        try (InputStream is = ProxyConfiguration.open(new URL(urlSpec)).getInputStream()) {
            if (is == null) {
                throw new IllegalStateException("Could not find resource: " + urlSpec);
            }
            nodePath.copyFrom(is);
        }
    }
}
