package hudson.plugins.gradle.injection;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.plugins.gradle.injection.MavenExtensionsHandler.MavenExtension;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static hudson.plugins.gradle.injection.GlobalEnvironmentVariables.GRADLE_ENTERPRISE_ALLOW_UNTRUSTED_SERVER;
import static hudson.plugins.gradle.injection.GlobalEnvironmentVariables.GRADLE_ENTERPRISE_CCUD_EXTENSION_VERSION;
import static hudson.plugins.gradle.injection.GlobalEnvironmentVariables.GRADLE_ENTERPRISE_EXTENSION_VERSION;
import static hudson.plugins.gradle.injection.GlobalEnvironmentVariables.GRADLE_ENTERPRISE_URL;
import static hudson.plugins.gradle.injection.GlobalEnvironmentVariables.MAVEN_INJECTION_DISABLED_NODES;
import static hudson.plugins.gradle.injection.GlobalEnvironmentVariables.MAVEN_INJECTION_ENABLED_NODES;

public class MavenBuildScanInjection implements BuildScanInjection {

    private static final Logger LOGGER = Logger.getLogger(MavenBuildScanInjection.class.getName());

    // Maven system properties passed on the CLI to a Maven build
    private static final String GRADLE_ENTERPRISE_URL_PROPERTY_KEY = "gradle.enterprise.url";
    private static final String GRADLE_ENTERPRISE_ALLOW_UNTRUSTED_SERVER_PROPERTY_KEY = "gradle.enterprise.allowUntrustedServer";
    private static final String GRADLE_SCAN_UPLOAD_IN_BACKGROUND_PROPERTY_KEY = "gradle.scan.uploadInBackground";
    private static final String MAVEN_EXT_CLASS_PATH_PROPERTY_KEY = "maven.ext.class.path";

    private static final MavenOptsSetter MAVEN_OPTS_SETTER =
        new MavenOptsSetter(
            MAVEN_EXT_CLASS_PATH_PROPERTY_KEY,
            GRADLE_SCAN_UPLOAD_IN_BACKGROUND_PROPERTY_KEY,
            GRADLE_ENTERPRISE_ALLOW_UNTRUSTED_SERVER_PROPERTY_KEY,
            GRADLE_ENTERPRISE_URL_PROPERTY_KEY);

    private final MavenExtensionsHandler extensionsHandler = new MavenExtensionsHandler();

    @Override
    public String getActivationEnvironmentVariableName() {
        return GRADLE_ENTERPRISE_EXTENSION_VERSION;
    }

    @Override
    public void inject(Node node, EnvVars envGlobal, EnvVars envComputer) {
        try {
            if (node == null) {
                return;
            }

            FilePath nodeRootPath = node.getRootPath();
            if (nodeRootPath == null) {
                return;
            }

            if (injectionEnabledForNode(node, envGlobal)) {
                injectMavenExtensions(node, nodeRootPath);
            } else {
                removeMavenExtensions(node, nodeRootPath);
            }
        } catch (IllegalStateException e) {
            if (injectionEnabledForNode(node, envGlobal)) {
                LOGGER.log(Level.WARNING, "Unexpected exception while injecting build scans for Maven", e);
            }
        }
    }

    @Override
    public String getEnabledNodesEnvironmentVariableName() {
        return MAVEN_INJECTION_ENABLED_NODES;
    }

    @Override
    public String getDisabledNodesEnvironmentVariableName() {
        return MAVEN_INJECTION_DISABLED_NODES;
    }

    private void injectMavenExtensions(Node node, FilePath nodeRootPath) {
        try {
            LOGGER.info("Injecting Maven extensions " + nodeRootPath);
            List<FilePath> libs = new LinkedList<>();

            libs.add(extensionsHandler.copyExtensionToAgent(MavenExtension.GRADLE_ENTERPRISE, nodeRootPath));
            if (getGlobalEnvVar(GRADLE_ENTERPRISE_CCUD_EXTENSION_VERSION) != null) {
                libs.add(extensionsHandler.copyExtensionToAgent(MavenExtension.CCUD, nodeRootPath));
            } else {
                extensionsHandler.deleteExtensionFromAgent(MavenExtension.CCUD, nodeRootPath);
            }

            String cp = constructExtClasspath(libs, isUnix(node));
            List<String> mavenOptsKeyValuePairs = new ArrayList<>();
            mavenOptsKeyValuePairs.add(asSystemProperty(MAVEN_EXT_CLASS_PATH_PROPERTY_KEY, cp));
            mavenOptsKeyValuePairs.add(asSystemProperty(GRADLE_SCAN_UPLOAD_IN_BACKGROUND_PROPERTY_KEY, "false"));

            if (getGlobalEnvVar(GRADLE_ENTERPRISE_ALLOW_UNTRUSTED_SERVER) != null) {
                mavenOptsKeyValuePairs.add(asSystemProperty(GRADLE_ENTERPRISE_ALLOW_UNTRUSTED_SERVER_PROPERTY_KEY, getGlobalEnvVar(GRADLE_ENTERPRISE_ALLOW_UNTRUSTED_SERVER)));
            }
            if (getGlobalEnvVar(GRADLE_ENTERPRISE_URL) != null) {
                mavenOptsKeyValuePairs.add(asSystemProperty(GRADLE_ENTERPRISE_URL_PROPERTY_KEY, getGlobalEnvVar(GRADLE_ENTERPRISE_URL)));
            }

            MAVEN_OPTS_SETTER.writeMavenOptsToFile(node, nodeRootPath, mavenOptsKeyValuePairs);
            MAVEN_OPTS_SETTER.removeLegacyMavenOptsValueFromNodeProperties(node);
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private void removeMavenExtensions(Node node, FilePath rootPath) {
        try {
            extensionsHandler.deleteAllExtensionsFromAgent(rootPath);
            MAVEN_OPTS_SETTER.removeLegacyMavenOptsValueFromNodeProperties(node);
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private String constructExtClasspath(List<FilePath> libs, boolean isUnix) throws IOException, InterruptedException {
        return libs.stream().map(FilePath::getRemote).collect(Collectors.joining(getDelimiter(isUnix)));
    }

    private String getDelimiter(boolean isUnix) {
        return isUnix ? ":" : ";";
    }

    private static boolean isUnix(Node node) {
        Computer computer = node.toComputer();
        return computer == null || Boolean.TRUE.equals(computer.isUnix());
    }

    private static String getGlobalEnvVar(String varName) {
        EnvironmentVariablesNodeProperty envProperty =
            Jenkins.get().getGlobalNodeProperties().get(EnvironmentVariablesNodeProperty.class);
        return envProperty.getEnvVars().get(varName);
    }

    private static String asSystemProperty(String sysProp, String value) {
        return "-D" + sysProp + "=" + value;
    }
}
