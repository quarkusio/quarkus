package io.quarkus.deployment.pkg.steps;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

import io.quarkus.deployment.util.ContainerRuntimeUtil.ContainerRuntime;
import io.smallrye.common.process.ProcessBuilder;
import io.smallrye.common.process.ProcessExecutionException;

/**
 * Resolves the UID and GID of the user that runs inside a container image by executing
 * {@code id} inside an ephemeral container. Results are cached per image for the lifetime of the JVM
 * to avoid repeated process launches.
 */
public final class ContainerUserResolver {

    private static final Logger log = Logger.getLogger(ContainerUserResolver.class);

    private static final Pattern ID_OUTPUT_PATTERN = Pattern.compile("uid=(\\d+).*gid=(\\d+)");

    // Cache keyed by image name. Only successful resolutions are cached.
    private static final Map<String, ContainerUser> CACHE = new ConcurrentHashMap<>();

    private ContainerUserResolver() {
    }

    /**
     * Returns the {@link ContainerUser} (uid, gid) for the default user of the given image,
     * or {@code null} if detection failed (e.g. the {@code id} command is not available).
     * Successful resolutions are cached for the lifetime of the JVM; failures are not cached
     * so callers can retry after transient errors (e.g. network issues while pulling the image).
     */
    public static ContainerUser resolve(ContainerRuntime runtime, String image) {
        ContainerUser cached = CACHE.get(image);
        if (cached != null) {
            return cached;
        }
        ContainerUser result = runIdInContainer(runtime, image);
        if (result != null) {
            CACHE.put(image, result);
        }
        return result;
    }

    private static ContainerUser runIdInContainer(ContainerRuntime runtime, String image) {
        try {
            String output = ProcessBuilder.newBuilder(runtime.getExecutableName())
                    .arguments("run", "--rm", "--entrypoint", "id", image)
                    .output().gatherOnFail(true).toSingleString(8192)
                    .error().redirect()
                    .run()
                    .trim();

            Matcher matcher = ID_OUTPUT_PATTERN.matcher(output);

            if (matcher.find()) {
                return new ContainerUser(matcher.group(1), matcher.group(2));
            }

            log.warnf("Could not parse 'id' output for image '%s': %s", image, output);
            return null;

        } catch (ProcessExecutionException e) {
            log.warnf("Could not resolve container UID/GID for image '%s': %s", image, e.getMessage());
            return null;
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warnf(e, "Error resolving container UID/GID for image '%s'", image);
            return null;
        }
    }

    public record ContainerUser(String uid, String gid) {
    }
}
