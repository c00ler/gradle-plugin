package hudson.plugins.gradle;

import hudson.model.TaskListener;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Objects;

/**
 * @author Gregory Boissinot
 */
public class GradleLogger implements Serializable {

    private final TaskListener listener;

    private GradleLogger(TaskListener listener) {
        this.listener = Objects.requireNonNull(listener, "listener must not be null");
    }

    public void info(String message) {
        listener.getLogger().println("[Gradle] - " + message);
    }

    public void error(String message) {
        listener.getLogger().println("[Gradle] - [ERROR] " + message);
    }

    public static GradleLogger of(@Nullable TaskListener taskListener) {
        return taskListener != null
            ? new GradleLogger(taskListener)
            : new GradleLogger(new NullTaskListener());
    }
}
