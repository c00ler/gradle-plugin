package hudson.plugins.gradle.injection;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import hudson.EnvVars;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import jenkins.model.Jenkins;

import java.util.Set;

import static hudson.plugins.gradle.injection.GlobalEnvironmentVariables.GRADLE_ENTERPRISE_EXTENSION_VERSION;
import static hudson.plugins.gradle.injection.GlobalEnvironmentVariables.GRADLE_ENTERPRISE_INJECTION;

public final class InjectionUtils {

    private static final String COMMA = ",";

    private InjectionUtils() {
    }

    public static boolean isMavenInjectionEnabledGlobally() {
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        if (jenkins == null) {
            return false;
        }

        EnvironmentVariablesNodeProperty envProperty =
            jenkins.getGlobalNodeProperties().get(EnvironmentVariablesNodeProperty.class);
        if (envProperty == null) {
            return false;
        }

        EnvVars envVars = envProperty.getEnvVars();
        return EnvUtil.isSet(envVars, GRADLE_ENTERPRISE_INJECTION)
            && EnvUtil.isSet(envVars, GRADLE_ENTERPRISE_EXTENSION_VERSION);
    }

    public static boolean isInjectionEnabledForNode(Set<String> labels, String disabledNodes, String enabledNodes) {
        return isNotDisabled(labels, disabledNodes) && isEnabled(labels, enabledNodes);
    }

    private static boolean isEnabled(Set<String> labels, String enabledNodes) {
        return Strings.isNullOrEmpty(enabledNodes) || isInEnabledNodes(enabledNodes, labels);
    }

    private static boolean isNotDisabled(Set<String> labels, String disabledNodes) {
        return Strings.isNullOrEmpty(disabledNodes) || isNotInDisabledNodes(disabledNodes, labels);
    }

    private static boolean isInEnabledNodes(String enabledNodes, Set<String> labels) {
        return Sets.newHashSet(enabledNodes.split(COMMA)).stream().anyMatch(labels::contains);
    }

    private static boolean isNotInDisabledNodes(String disabledNodes, Set<String> labels) {
        Set<String> labelsToDisable = Sets.newHashSet(disabledNodes.split(COMMA));
        return labels.stream().noneMatch(labelsToDisable::contains);
    }
}
