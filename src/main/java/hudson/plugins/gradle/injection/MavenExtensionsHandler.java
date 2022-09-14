package hudson.plugins.gradle.injection;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import hudson.FilePath;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static hudson.plugins.gradle.injection.CopyUtil.copyResourceToNode;

public class MavenExtensionsHandler {

    static final String LIB_DIR_PATH = "jenkins-gradle-plugin/lib";

    private final Map<MavenExtension, MavenExtensionFileHandler> fileHandlers =
        Arrays.stream(MavenExtension.values())
            .map(MavenExtensionFileHandler::new)
            .collect(Collectors.toMap(h -> h.extension, Function.identity()));

    public void copyExtensionToAgent(MavenExtension extension, FilePath rootPath) throws IOException, InterruptedException {
        fileHandlers.get(extension).copyExtensionToAgent(rootPath);
    }

    public FilePath getAgentExtensionPath(MavenExtension extension, FilePath rootPath) throws IOException {
        return fileHandlers.get(extension).getAgentExtensionPath(rootPath);
    }

    public void deleteAllExtensionsFromAgent(FilePath rootPath) throws IOException, InterruptedException {
        rootPath.child(LIB_DIR_PATH).deleteContents();
    }

    private static final class MavenExtensionFileHandler {
        private final MavenExtension extension;

        MavenExtensionFileHandler(MavenExtension extension) {
            this.extension = extension;
        }

        public void copyExtensionToAgent(FilePath rootPath) throws IOException, InterruptedException {
            copyResourceToNode(rootPath.child(LIB_DIR_PATH).child(extension.getJarName()), extension.getJarName());
        }

        public FilePath getAgentExtensionPath(FilePath rootPath) {
            return rootPath.child(LIB_DIR_PATH).child(extension.getJarName());
        }
    }

    public enum MavenExtension {
        GRADLE_ENTERPRISE("gradle-enterprise-maven-extension"),
        CCUD("common-custom-user-data-maven-extension"),
        CONFIGURATION("configuration-maven-extension", "1.0.0");

        final String name;
        final Supplier<String> version;

        MavenExtension(String name) {
            this(name, null);
        }

        MavenExtension(String name, String fixedVersion) {
            this.name = name;
            this.version =
                fixedVersion != null
                    ? Suppliers.ofInstance(fixedVersion)
                    : Suppliers.memoize(this::getExtensionVersion);
        }

        public String getJarName() {
            return name + "-" + version.get() + ".jar";
        }

        private String getExtensionVersion() {
            try {
                String resourceName = name + "-version.txt";
                try (InputStream version = MavenBuildScanInjection.class.getResourceAsStream("/versions/" + resourceName)) {
                    if (version == null) {
                        throw new IllegalStateException("Could not find resource: " + resourceName);
                    }
                    return IOUtils.toString(version, StandardCharsets.UTF_8);
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
