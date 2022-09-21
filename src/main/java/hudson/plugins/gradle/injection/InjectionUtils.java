package hudson.plugins.gradle.injection;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import java.util.Set;

public final class InjectionUtils {

    private static final String COMMA = ",";

    private InjectionUtils() {
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
