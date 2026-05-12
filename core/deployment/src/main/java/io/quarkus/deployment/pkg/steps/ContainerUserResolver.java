package io.quarkus.deployment.pkg.steps;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

import io.quarkus.deployment.util.ContainerRuntimeUtil.ContainerRuntime;

/**
 * Resolves the UID and GID of the user that runs inside a container image by executing
 * {@code id} inside an ephemeral container. Results are cached per (runtime, image) pair
 * for the lifetime of the JVM to avoid repeated process launches.
 */
public final class ContainerUserResolver {

    private static final Logger log = Logger.getLogger(ContainerUserResolver.class);

    private static final Pattern UID_PATTERN = Pattern.compile("uid=(\\d+)");
    private static final Pattern GID_PATTERN = Pattern.compile("gid=(\\d+)");

    // Cache keyed by "runtime:image" — stores Optional so that failed resolutions
    // (Optional.empty()) are also cached and do not re-trigger the expensive `id` run.
    private static final Map<String, Optional<ContainerUser>> CACHE = new ConcurrentHashMap<>();

    private ContainerUserResolver() {
    }

    /**
     * Returns the {@link ContainerUser} (uid, gid) for the default user of the given image,
     * or {@code null} if detection failed (e.g. the {@code id} command is not available).
     * Failed resolutions are cached so the container is only probed once per JVM.
     */
    public static ContainerUser resolve(ContainerRuntime runtime, String image) {
        String key = runtime.getExecutableName() + ":" + image;
        return CACHE.computeIfAbsent(key, k -> Optional.ofNullable(runIdInContainer(runtime, image))).orElse(null);
    }

    private static ContainerUser runIdInContainer(ContainerRuntime runtime, String image) {
        try {
            Process process = new ProcessBuilder(
                    runtime.getExecutableName(),
                    "run", "--rm",
                    "--entrypoint", "id",
                    image)
                    .redirectErrorStream(true)
                    .start();

            if (!process.waitFor(5, TimeUnit.MINUTES)) {
                process.destroyForcibly();
                log.warnf("Timed out resolving container UID/GID for image '%s'", image);
                return null;
            }

            // Read output first (stderr merged into stdout via redirectErrorStream) so it is
            // available for logging regardless of whether the process succeeded or failed.
            String output = new String(process.getInputStream().readAllBytes()).trim();

            if (process.exitValue() != 0) {
                log.warnf("Could not resolve container UID/GID for image '%s': 'id' command returned exit code %d. Output: %s",
                        image, process.exitValue(), output);
                return null;
            }

            Matcher uidMatcher = UID_PATTERN.matcher(output);
            Matcher gidMatcher = GID_PATTERN.matcher(output);

            if (uidMatcher.find() && gidMatcher.find()) {
                return new ContainerUser(uidMatcher.group(1), gidMatcher.group(1));
            }

            log.warnf("Could not parse 'id' output for image '%s': %s", image, output);
            return null;

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warnf(e, "Error resolving container UID/GID for image '%s'", image);
            return null;
        }
    }

    public record ContainerUser(String uid, String gid) {
    }
}
