package io.quarkus.runtime;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class JVMChecksRecorder {

    public void check() {
        if (!isUnsafeMemoryAccessAllowed()) {
            Logger.getLogger("JVM").warn(
                    "Unsafe memory access is not going to be allowed in future versions of the JVM. Since Java 24, the JVM will print a warning on boot (most likely shown above), but several Quarkus extensions still require it. "
                            +
                            "There is currently no need to worry: please add the `--sun-misc-unsafe-memory-access=allow` JVM argument to avoid these warnings. "
                            +
                            "We are working with the maintainers of those libraries to get this resolved in future versions; when this is done, we will remove the need for this argument.");
        }
    }

    public static boolean isUnsafeMemoryAccessAllowed() {
        if (Runtime.version().feature() < 24) {
            //Versions before Java 24 would not complain about the use of Unsafe.
            //Also, setting `--sun-misc-unsafe-memory-access=allow` isn't possible (not a valid argument) before Java 24.
            return true;
        }
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        List<String> arguments = runtimeMxBean.getInputArguments();
        return arguments.contains("--sun-misc-unsafe-memory-access=allow");
    }

}
