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
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MavenBuildScanInjection implements BuildScanInjection {

    private static final Logger LOGGER = Logger.getLogger(MavenBuildScanInjection.class.getName());


    // Maven system properties passed on the CLI to a Maven build
    private static final String GRADLE_ENTERPRISE_URL_PROPERTY_KEY = "gradle.enterprise.url";
    private static final String GRADLE_ENTERPRISE_ALLOW_UNTRUSTED_SERVER_PROPERTY_KEY = "gradle.enterprise.allowUntrustedServer";
    // Environment variables set in Jenkins Global configuration
    private static final String GRADLE_SCAN_UPLOAD_IN_BACKGROUND_PROPERTY_KEY = "gradle.scan.uploadInBackground";
    private static final String MAVEN_EXT_CLASS_PATH_PROPERTY_KEY = "maven.ext.class.path";
    private static final MavenOptsSetter MAVEN_OPTS_SETTER = new MavenOptsSetter(
        MAVEN_EXT_CLASS_PATH_PROPERTY_KEY,
        GRADLE_SCAN_UPLOAD_IN_BACKGROUND_PROPERTY_KEY,
        GRADLE_ENTERPRISE_ALLOW_UNTRUSTED_SERVER_PROPERTY_KEY,
        GRADLE_ENTERPRISE_URL_PROPERTY_KEY
    );
    private static final String GE_ALLOW_UNTRUSTED_VAR = "JENKINSGRADLEPLUGIN_GRADLE_ENTERPRISE_ALLOW_UNTRUSTED_SERVER";
    private static final String GE_URL_VAR = "JENKINSGRADLEPLUGIN_GRADLE_ENTERPRISE_URL";
    private static final String GE_CCUD_VERSION_VAR = "JENKINSGRADLEPLUGIN_CCUD_EXTENSION_VERSION";

    private final MavenExtensionsHandler extensionsHandler = new MavenExtensionsHandler();

    @Override
    public String getActivationEnvironmentVariableName() {
        return "JENKINSGRADLEPLUGIN_GRADLE_ENTERPRISE_EXTENSION_VERSION";
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

            if (injectionEnabled(envGlobal)) {
                injectMavenExtensions(node, nodeRootPath);
            } else {
                removeMavenExtensions(node, nodeRootPath);
            }
        } catch (IllegalStateException e) {
            if (injectionEnabled(envGlobal)) {
                LOGGER.warning("Error: " + e.getMessage());
            }
        }
    }

    private void injectMavenExtensions(Node node, FilePath nodeRootPath) {
        try {
            LOGGER.info("Injecting Maven extensions " + nodeRootPath);
            List<FilePath> libs = new LinkedList<>();

            extensionsHandler.copyGradleEnterpriseExtensionToAgent(nodeRootPath);
            libs.add(extensionsHandler.getGradleEnterpriseExtensionPath(nodeRootPath));

            if (getGlobalEnvVar(GE_CCUD_VERSION_VAR) != null) {
                extensionsHandler.copyCCUDExtensionToAgent(nodeRootPath);
                libs.add(extensionsHandler.getCCUDExtensionPath(nodeRootPath));
            }

            String cp = constructExtClasspath(libs, isUnix(node));
            List<String> mavenOptsKeyValuePairs = new ArrayList<>();
            mavenOptsKeyValuePairs.add(asSystemProperty(MAVEN_EXT_CLASS_PATH_PROPERTY_KEY, cp));
            mavenOptsKeyValuePairs.add(asSystemProperty(GRADLE_SCAN_UPLOAD_IN_BACKGROUND_PROPERTY_KEY, "false"));

            if (getGlobalEnvVar(GE_ALLOW_UNTRUSTED_VAR) != null) {
                mavenOptsKeyValuePairs.add(asSystemProperty(GRADLE_ENTERPRISE_ALLOW_UNTRUSTED_SERVER_PROPERTY_KEY, getGlobalEnvVar(GE_ALLOW_UNTRUSTED_VAR)));
            }
            if (getGlobalEnvVar(GE_URL_VAR) != null) {
                mavenOptsKeyValuePairs.add(asSystemProperty(GRADLE_ENTERPRISE_URL_PROPERTY_KEY, getGlobalEnvVar(GE_URL_VAR)));
            }
            MAVEN_OPTS_SETTER.appendIfMissing(node, mavenOptsKeyValuePairs);
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private boolean isUnix(Node node) {
        Computer computer = node.toComputer();
        return computer == null || Boolean.TRUE.equals(computer.isUnix());
    }

    private void removeMavenExtensions(Node node, FilePath rootPath) {
        try {
            MAVEN_OPTS_SETTER.remove(node);
            extensionsHandler.deleteAllExtensionsFromAgent(rootPath);
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

    private String getGlobalEnvVar(String varName) {
        EnvironmentVariablesNodeProperty envProperty = Jenkins.get().getGlobalNodeProperties()
            .get(EnvironmentVariablesNodeProperty.class);
        return envProperty.getEnvVars().get(varName);
    }

    private String asSystemProperty(String sysProp, String value) {
        return "-D" + sysProp + "=" + value;
    }
}
