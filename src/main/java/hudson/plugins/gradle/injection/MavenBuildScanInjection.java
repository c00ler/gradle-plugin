package hudson.plugins.gradle.injection;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MavenBuildScanInjection implements BuildScanInjection {

    private static final Logger LOGGER = Logger.getLogger(MavenBuildScanInjection.class.getName());

    // Maven system properties passed on the CLI to a Maven build
    private static final String GRADLE_ENTERPRISE_URL_PROPERTY_KEY = "gradle.enterprise.url";
    private static final String GRADLE_ENTERPRISE_ALLOW_UNTRUSTED_SERVER_PROPERTY_KEY = "gradle.enterprise.allowUntrustedServer";
    private static final String GRADLE_SCAN_UPLOAD_IN_BACKGROUND_PROPERTY_KEY = "gradle.scan.uploadInBackground";
    private static final String MAVEN_EXT_CLASS_PATH_PROPERTY_KEY = "maven.ext.class.path";

    private static final MavenOptsSetter MAVEN_OPTS_SETTER = new MavenOptsSetter(
        MAVEN_EXT_CLASS_PATH_PROPERTY_KEY,
        GRADLE_SCAN_UPLOAD_IN_BACKGROUND_PROPERTY_KEY,
        GRADLE_ENTERPRISE_ALLOW_UNTRUSTED_SERVER_PROPERTY_KEY,
        GRADLE_ENTERPRISE_URL_PROPERTY_KEY
    );

    // Environment variables set in Jenkins Global configuration
    private static final String GE_ALLOW_UNTRUSTED_VAR = "JENKINSGRADLEPLUGIN_GRADLE_ENTERPRISE_ALLOW_UNTRUSTED_SERVER";
    private static final String GE_URL_VAR = "JENKINSGRADLEPLUGIN_GRADLE_ENTERPRISE_URL";
    private static final String GE_CCUD_VERSION_VAR = "JENKINSGRADLEPLUGIN_CCUD_EXTENSION_VERSION";
    private static final String GE_EXTENSION_VERSION_VAR = "JENKINSGRADLEPLUGIN_GRADLE_ENTERPRISE_EXTENSION_VERSION";

    public static final String GE_EXTENSION_CLASSPATH_VAR = "JENKINSGRADLEPLUGIN_GRADLE_ENTERPRISE_EXTENSION_CLASSPATH";

    static final String FEATURE_TOGGLE_DISABLED_NODES = "JENKINSGRADLEPLUGIN_MAVEN_INJECTION_DISABLED_NODES";
    static final String FEATURE_TOGGLE_ENABLED_NODES = "JENKINSGRADLEPLUGIN_MAVEN_INJECTION_ENABLED_NODES";

    private final MavenExtensionsHandler extensionsHandler = new MavenExtensionsHandler();

    @Override
    public String getActivationEnvironmentVariableName() {
        return GE_EXTENSION_VERSION_VAR;
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

            removeMavenExtensions(node, nodeRootPath);
            if (injectionEnabledForNode(node, envGlobal)) {
                injectMavenExtensions(node, nodeRootPath);
            }
        } catch (IllegalStateException e) {
            if (injectionEnabled(envGlobal)) {
                LOGGER.log(
                    Level.WARNING, "Unexpected exception while injecting build scans for Maven", e);
            }
        }
    }

    @Override
    public String getEnabledNodesEnvironmentVariableName() {
        return FEATURE_TOGGLE_ENABLED_NODES;
    }

    @Override
    public String getDisabledNodesEnvironmentVariableName() {
        return FEATURE_TOGGLE_DISABLED_NODES;
    }

    private void injectMavenExtensions(Node node, FilePath nodeRootPath) {
        try {
            LOGGER.info("Injecting Maven extensions " + nodeRootPath);
            List<FilePath> libs = new LinkedList<>();

            extensionsHandler.copyExtensionToAgent(MavenExtensionsHandler.MavenExtension.GRADLE_ENTERPRISE, nodeRootPath);
            libs.add(extensionsHandler.getAgentExtensionPath(MavenExtensionsHandler.MavenExtension.GRADLE_ENTERPRISE, nodeRootPath));

            if (getGlobalEnvVar(GE_CCUD_VERSION_VAR) != null) {
                extensionsHandler.copyExtensionToAgent(MavenExtensionsHandler.MavenExtension.CCUD, nodeRootPath);
                libs.add(extensionsHandler.getAgentExtensionPath(MavenExtensionsHandler.MavenExtension.CCUD, nodeRootPath));
            }

            boolean isUnix = isUnix(node);

            List<String> mavenOptsKeyValuePairs = new ArrayList<>();
            mavenOptsKeyValuePairs.add(asSystemProperty(MAVEN_EXT_CLASS_PATH_PROPERTY_KEY, constructExtClasspath(libs, isUnix)));
            mavenOptsKeyValuePairs.add(asSystemProperty(GRADLE_SCAN_UPLOAD_IN_BACKGROUND_PROPERTY_KEY, "false"));

            if (getGlobalEnvVar(GE_ALLOW_UNTRUSTED_VAR) != null) {
                mavenOptsKeyValuePairs.add(asSystemProperty(GRADLE_ENTERPRISE_ALLOW_UNTRUSTED_SERVER_PROPERTY_KEY, getGlobalEnvVar(GE_ALLOW_UNTRUSTED_VAR)));
            }
            if (getGlobalEnvVar(GE_URL_VAR) != null) {
                mavenOptsKeyValuePairs.add(asSystemProperty(GRADLE_ENTERPRISE_URL_PROPERTY_KEY, getGlobalEnvVar(GE_URL_VAR)));
            }
            MAVEN_OPTS_SETTER.appendIfMissing(node, mavenOptsKeyValuePairs);

            // Configuration extension should not be added to MAVEN_OPTS
            extensionsHandler.copyExtensionToAgent(MavenExtensionsHandler.MavenExtension.CONFIGURATION, nodeRootPath);
            libs.add(extensionsHandler.getAgentExtensionPath(MavenExtensionsHandler.MavenExtension.CONFIGURATION, nodeRootPath));
            EnvUtil.setEnvVar(node, GE_EXTENSION_CLASSPATH_VAR, constructExtClasspath(libs, isUnix));
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private void removeMavenExtensions(Node node, FilePath rootPath) {
        try {
            MAVEN_OPTS_SETTER.remove(node);
            EnvUtil.setEnvVar(node, GE_EXTENSION_CLASSPATH_VAR, "");
            extensionsHandler.deleteAllExtensionsFromAgent(rootPath);
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String constructExtClasspath(List<FilePath> libs, boolean isUnix) throws IOException, InterruptedException {
        return libs
            .stream()
            .map(FilePath::getRemote)
            .collect(Collectors.joining(getDelimiter(isUnix)));
    }

    private static String getGlobalEnvVar(String varName) {
        EnvironmentVariablesNodeProperty envProperty =
            Jenkins.get().getGlobalNodeProperties().get(EnvironmentVariablesNodeProperty.class);
        return envProperty.getEnvVars().get(varName);
    }

    private static boolean isUnix(Node node) {
        Computer computer = node.toComputer();
        return computer == null || Boolean.TRUE.equals(computer.isUnix());
    }

    private static String getDelimiter(boolean isUnix) {
        return isUnix ? ":" : ";";
    }

    private static String asSystemProperty(String sysProp, String value) {
        return "-D" + sysProp + "=" + value;
    }
}
