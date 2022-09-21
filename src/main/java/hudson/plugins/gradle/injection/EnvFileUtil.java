package hudson.plugins.gradle.injection;

import com.google.common.base.Splitter;
import hudson.EnvVars;
import hudson.FilePath;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class EnvFileUtil {

    private static final String ENV_FILE = ".env";
    private static final Splitter LINE_SPLITTER = Splitter.on(System.lineSeparator()).omitEmptyStrings().trimResults();

    private EnvFileUtil() {
    }

    public static void write(FilePath nodePath, String mavenOpts) throws IOException, InterruptedException {
        byte[] content =
            ("MAVEN_OPTS=" + mavenOpts + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);

        FilePath envFilePath = nodePath.child(MavenExtensionsHandler.LIB_DIR_PATH).child(ENV_FILE);
        envFilePath.copyFrom(new ByteArrayInputStream(content));
    }

    @CheckForNull
    @Nullable
    public static EnvVars read(FilePath nodePath) throws IOException, InterruptedException {
        FilePath envFilePath = nodePath.child(MavenExtensionsHandler.LIB_DIR_PATH).child(ENV_FILE);
        if (!envFilePath.exists()) {
            return null;
        }

        EnvVars envVars = new EnvVars();

        String content = envFilePath.readToString();
        LINE_SPLITTER.split(content).forEach(envVars::addLine);

        return envVars;
    }
}
