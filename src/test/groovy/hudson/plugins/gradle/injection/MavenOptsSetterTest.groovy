package hudson.plugins.gradle.injection

import hudson.model.Slave
import hudson.slaves.DumbSlave
import hudson.slaves.EnvironmentVariablesNodeProperty
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class MavenOptsSetterTest extends Specification {

    private static final String MAVEN_OPTS = "MAVEN_OPTS"

    Slave node = new DumbSlave("test", "/tmp", null)

    @Subject
    MavenOptsSetter mavenOptsSetter = new MavenOptsSetter("maven.ext.class.path")

    def "doesn't remove MAVEN_OPTS from the first node property"() {
        given:
        node.getNodeProperties().add(new EnvironmentVariablesNodeProperty(new EnvironmentVariablesNodeProperty.Entry(MAVEN_OPTS, "foo")))
        node.getNodeProperties().add(new EnvironmentVariablesNodeProperty(new EnvironmentVariablesNodeProperty.Entry("TEST", "bar")))

        when:
        mavenOptsSetter.removeLegacyMavenOptsValueFromNodeProperties(node)

        then:
        node.getNodeProperties().get(EnvironmentVariablesNodeProperty.class).getEnvVars().get(MAVEN_OPTS) == "foo"
    }

    def "does nothing if no node properties"() {
        when:
        mavenOptsSetter.removeLegacyMavenOptsValueFromNodeProperties(node)

        then:
        noExceptionThrown()
    }

    def "does nothing if no MAVEN_OPTS"() {
        given:
        node.getNodeProperties().add(new EnvironmentVariablesNodeProperty(new EnvironmentVariablesNodeProperty.Entry("FOO", "bar")))

        when:
        mavenOptsSetter.removeLegacyMavenOptsValueFromNodeProperties(node)

        then:
        node.getNodeProperties().get(EnvironmentVariablesNodeProperty.class).getEnvVars().get("FOO") == "bar"
    }

    @Unroll
    def "removes MAVEN_OPTS only if it matches the pattern"() {
        given:
        node.getNodeProperties().add(new EnvironmentVariablesNodeProperty(new EnvironmentVariablesNodeProperty.Entry("FOO", "bar")))
        node.getNodeProperties().add(new EnvironmentVariablesNodeProperty(new EnvironmentVariablesNodeProperty.Entry(MAVEN_OPTS, mavenOptsValue)))

        when:
        mavenOptsSetter.removeLegacyMavenOptsValueFromNodeProperties(node)

        then:
        def last = node.getNodeProperties().getAll(EnvironmentVariablesNodeProperty.class).last()
        def result = last.getEnvVars().get(MAVEN_OPTS)
        if (removed) {
            result == null
        } else {
            result == mavenOptsValue
        }

        where:
        mavenOptsValue                                                                                                                                         || removed
        "-Dfoo=bar"                                                                                                                                            || false
        "-Dfoo=bar -Dmaven.ext.class.path=/tmp/gradle-enterprise-maven-extension.jar:/tmp/common-custom-user-data-maven-extension.jar -Dtest=true"             || true
        "-Dmaven.ext.class.path=/tmp/gradle-enterprise-maven-extension.jar"                                                                                    || true
        "-Dmaven.ext.class.path=/tmp/gradle-enterprise-maven-extension-1.0.0.jar"                                                                              || true
        "-Dmaven.ext.class.path=/tmp/gradle-enterprise-maven-extension-1.jar"                                                                                  || false
        "-Dfoo=bar -Dmaven.ext.class.path=/tmp/gradle-enterprise-maven-extension-1.0.jar:/tmp/common-custom-user-data-maven-extension-1.0.jar -Dtest=true"     || true
        "-Dfoo=bar -Dmaven.ext.class.path=/tmp/gradle-enterprise-maven-extension-1.0.0.jar:/tmp/common-custom-user-data-maven-extension-1.0.0.jar -Dtest=true" || true
        "-Dfoo=bar -Dext.class.path=/tmp/gradle-enterprise-maven-extension-1.0.0.jar:/tmp/common-custom-user-data-maven-extension-1.0.0.jar -Dtest=true"       || false
        "-Dfoo=bar -Dmaven.ext.class.path=/tmp/common-custom-user-data-maven-extension-1.0.0.jar -Dtest=true"                                                  || false
        "-Dfoo=bar -Dmaven.ext.class.path=/tmp/gradle-enterprise-maven-extension-1.0.0.zip:/tmp/common-custom-user-data-maven-extension-1.0.0.jar -Dtest=true" || false
    }
}
