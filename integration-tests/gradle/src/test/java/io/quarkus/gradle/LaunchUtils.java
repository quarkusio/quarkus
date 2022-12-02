package io.quarkus.gradle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.jboss.logging.Logger;

import io.quarkus.utilities.JavaBinFinder;

public class LaunchUtils {
    private static final Logger log = Logger.getLogger(LaunchUtils.class);

    protected static Process launch(Path jar, File output) throws IOException {
        return launch(jar, output, null);
    }

    protected static Process launch(Path jar, File output, Map<String, String> env) throws IOException {
        List<String> commands = new ArrayList<>();
        commands.add(JavaBinFinder.findBin());
        commands.add("-jar");
        commands.add(jar.toString());
        ProcessBuilder processBuilder = new ProcessBuilder(commands.toArray(new String[0]));
        processBuilder.redirectOutput(output);
        processBuilder.redirectError(output);
        if (env != null) {
            processBuilder.environment().putAll(env);
        }
        return processBuilder.start();
    }

    public static void dumpFileContentOnFailure(final Callable<Void> operation, final File logFile,
            final Class<? extends Throwable> failureType) throws Exception {

        try {
            operation.call();
        } catch (Throwable t) {
            log.error("Dumping logs that were generated in " + logFile + " for an operation that resulted in "
                    + t.getClass().getName() + ":", t);

            throw t;
        }
    }

}
