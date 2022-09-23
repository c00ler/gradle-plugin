package hudson.plugins.gradle.injection;

import com.google.common.collect.Iterables;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Node;
import hudson.slaves.EnvironmentVariablesNodeProperty;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class MavenOptsSetter {

    private static final Logger LOGGER = Logger.getLogger(MavenOptsSetter.class.getName());

    private static final String SPACE = " ";
    private static final String MAVEN_OPTS_VAR = "MAVEN_OPTS";
    private static final Pattern MAVEN_OPTS_WITH_GE_EXTENSION =
        Pattern.compile(".*-Dmaven\\.ext\\.class\\.path=.*gradle-enterprise-maven-extension(-\\d+(\\.\\d+(\\.\\d+)?)?)?\\.jar.*");

    private final Set<String> keys;

    public MavenOptsSetter(String... keys) {
        this.keys = new HashSet<>(Arrays.asList(keys));
    }

    void writeMavenOptsToFile(Node node, FilePath nodeRootPath, List<String> mavenOptsKeyValuePairs) throws IOException, InterruptedException {
        String mavenOpts = removeSystemProperties(getMavenOpts(node)) + SPACE + String.join(SPACE, mavenOptsKeyValuePairs);
        EnvFileUtil.write(nodeRootPath, mavenOpts);
    }

    public void removeLegacyMavenOptsValueFromNodeProperties(Node node) {
        List<EnvironmentVariablesNodeProperty> all =
            node.getNodeProperties().getAll(EnvironmentVariablesNodeProperty.class);
        if (all.isEmpty()) {
            return;
        }

        EnvironmentVariablesNodeProperty last = Iterables.getLast(all);
        EnvVars envVars = last.getEnvVars();
        String mavenOpts = envVars.get(MAVEN_OPTS_VAR);
        if (mavenOpts == null || mavenOpts.isEmpty()) {
            return;
        }

        if (MAVEN_OPTS_WITH_GE_EXTENSION.matcher(mavenOpts).matches()) {
            envVars.remove(MAVEN_OPTS_VAR);
            LOGGER.log(
                Level.INFO,
                "MAVEN_OPTS environment variable has been removed from the node properties: {0}",
                mavenOpts);
        }
    }

    private String getMavenOpts(Node node) throws IOException, InterruptedException {
        EnvVars nodeEnvVars = EnvVars.getRemote(node.getChannel());
        return nodeEnvVars.get(MAVEN_OPTS_VAR);
    }

    private String removeSystemProperties(String mavenOpts) throws RuntimeException {
        return Optional.ofNullable(mavenOpts)
            .map(this::filterMavenOpts)
            .orElse("");
    }

    /**
     * Splits MAVEN_OPTS at each space and then removes all key value pairs that contain
     * any of the keys we want to remove.
     */
    private String filterMavenOpts(String mavenOpts) {
        return Arrays.stream(mavenOpts.split(SPACE))
            .filter(this::shouldBeKept)
            .collect(Collectors.joining(SPACE))
            .trim();
    }

    /**
     * Checks for a MAVEN_OPTS key value pair whether it contains none of the keys we're looking for.
     * In other words if this segment none of the keys, this method returns true.
     */
    private boolean shouldBeKept(String seg) {
        return keys.stream().noneMatch(seg::contains);
    }
}
