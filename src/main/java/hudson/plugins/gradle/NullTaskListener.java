package hudson.plugins.gradle;

import hudson.model.TaskListener;
import org.apache.commons.io.output.NullPrintStream;

import java.io.PrintStream;

public final class NullTaskListener implements TaskListener {

    private static final long serialVersionUID = 1L;

    @Override
    public PrintStream getLogger() {
        return new NullPrintStream();
    }
}
