package hudson.plugins.gradle.injection;

import hudson.EnvVars;
import hudson.model.Node;
import hudson.model.labels.LabelAtom;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public interface BuildScanInjection {

    default boolean injectionEnabled(EnvVars env) {
        return EnvUtil.getEnv(env, getActivationEnvironmentVariableName()) != null;
    }

    String getActivationEnvironmentVariableName();

    void inject(Node node, EnvVars envGlobal, EnvVars envComputer);

    default boolean isInjectionEnabledForNode(Node node, EnvVars envGlobal) {
        Set<LabelAtom> labelAtoms = (node.getAssignedLabels() != null ? node.getAssignedLabels() : new HashSet<>());
        Set<String> labels = labelAtoms.stream().map(LabelAtom::getName).collect(Collectors.toSet());

        String disabledNodes = EnvUtil.getEnv(envGlobal, getDisabledNodesEnvironmentVariableName());
        String enabledNodes = EnvUtil.getEnv(envGlobal, getEnabledNodesEnvironmentVariableName());

        return InjectionUtils.isInjectionEnabledForNode(labels, disabledNodes, enabledNodes);
    }

    String getEnabledNodesEnvironmentVariableName();

    String getDisabledNodesEnvironmentVariableName();
}
