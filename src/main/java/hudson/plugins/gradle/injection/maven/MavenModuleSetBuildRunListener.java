package hudson.plugins.gradle.injection.maven;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.plugins.gradle.GradleLogger;

@Extension(optional = true)
public class MavenModuleSetBuildRunListener extends RunListener<MavenModuleSetBuild> {

    @Override
    public void onStarted(MavenModuleSetBuild build, TaskListener listener) {
        GradleLogger logger = GradleLogger.of(listener);

        MavenModuleSet project = build.getProject();

        Node node = build.getBuiltOn();
        if (node == null) {
            logger.error("Node is null");
        }

        logger.info("Build on: " + node.getNodeName());

        String originalMavenOpts = project.getMavenOpts();

        if (originalMavenOpts.contains("gradle.enterprise.url")) {
            logger.info("Already exists");
        } else {
            project.setMavenOpts(originalMavenOpts + " -Dgradle.enterprise.url=https://ge-unstable.grdev.net");
            logger.info("Added 'gradle.enterprise.url'");
        }


    }

    @Override
    public void onCompleted(MavenModuleSetBuild build, @NonNull TaskListener listener) {
        GradleLogger logger = GradleLogger.of(listener);
    }
}
