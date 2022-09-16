package hudson.plugins.gradle.injection;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Node;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

class MavenOptsSetter {

    private static final String MAVEN_OPTS_VAR = "MAVEN_OPTS";
    private final Set<String> keys;

    public MavenOptsSetter(String... keys) {
        this.keys = new HashSet<>(Arrays.asList(keys));
    }

    void appendIfMissing(Node node, FilePath envFile, List<String> mavenOptsKeyValuePairs) throws IOException, InterruptedException {
        String mavenOpts = removeSystemProperties(getMavenOpts(node)) + " " + String.join(" ", mavenOptsKeyValuePairs);

        byte[] envFileContent =
            new StringBuilder()
                .append(MAVEN_OPTS_VAR).append("=").append(mavenOpts)
                .append(System.lineSeparator())
                .toString()
                .getBytes(StandardCharsets.UTF_8);

        envFile.copyFrom(new ByteArrayInputStream(envFileContent));
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
        return Arrays.stream(mavenOpts.split(" "))
            .filter(this::shouldBeKept)
            .collect(Collectors.joining(" "))
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
