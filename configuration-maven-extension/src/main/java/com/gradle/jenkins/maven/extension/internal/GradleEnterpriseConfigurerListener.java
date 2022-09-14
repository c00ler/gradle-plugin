package com.gradle.jenkins.maven.extension.internal;

import com.gradle.maven.extension.api.GradleEnterpriseApi;
import com.gradle.maven.extension.api.GradleEnterpriseListener;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    role = GradleEnterpriseListener.class,
    hint = "gradle-enterprise-configurer"
)
public class GradleEnterpriseConfigurerListener implements GradleEnterpriseListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(GradleEnterpriseConfigurerListener.class);

    private static final String JENKINSGRADLEPLUGIN_GRADLE_ENTERPRISE_URL = "JENKINSGRADLEPLUGIN_GRADLE_ENTERPRISE_URL";
    private static final String JENKINSGRADLEPLUGIN_GRADLE_ENTERPRISE_ALLOW_UNTRUSTED_SERVER = "JENKINSGRADLEPLUGIN_GRADLE_ENTERPRISE_ALLOW_UNTRUSTED_SERVER";

    @Override
    public void configure(GradleEnterpriseApi api, MavenSession session) throws Exception {
        if (api.getServer() != null) {
            LOGGER.info("Gradle Enterprise server is already configured");
            return;
        }

        String server = System.getenv(JENKINSGRADLEPLUGIN_GRADLE_ENTERPRISE_URL);
        if (server == null || server.isEmpty()) {
            LOGGER.info("Environment variable {} is not set", JENKINSGRADLEPLUGIN_GRADLE_ENTERPRISE_URL);
            return;
        }

        api.setServer(server);
        LOGGER.info("Gradle Enterprise server URL is set to: {}", server);

        api.getBuildScan().setUploadInBackground(false);

        if (Boolean.parseBoolean(System.getenv(JENKINSGRADLEPLUGIN_GRADLE_ENTERPRISE_ALLOW_UNTRUSTED_SERVER))) {
            api.setAllowUntrustedServer(true);
            LOGGER.info("Allow communication with a Gradle Enterprise server using an untrusted SSL certificate");
        }
    }
}
