package io.quarkus.bootstrap.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

import io.quarkus.maven.dependency.ArtifactCoords;

/**
 * Resolver-agnostic component that tracks provided and required capabilities
 * during dependency resolution and determines which default providers to inject.
 *
 * <p>
 * Both Maven and Gradle resolvers feed discovered extension capabilities into
 * this component. After each conditional dependency cycle converges, the resolver
 * asks {@link #getNextDefaultProvider()} for the highest-priority unsatisfied
 * requirement that has a configured default provider.
 *
 * <p>
 * Default providers are injected one at a time because a single provider may
 * transitively bring in other extensions that satisfy currently unsatisfied
 * requirements, avoiding unnecessary additions.
 *
 * <p>
 * Only unconditional capabilities participate in resolution. Capabilities with
 * conditional syntax ({@code capability-name?BooleanSupplierClass}) are ignored
 * because the {@code BooleanSupplier} can only be evaluated at build time.
 *
 * <p>
 * This class is not thread-safe. Callers must ensure that all method invocations
 * on a given instance are performed from a single thread.
 */
public class DefaultCapabilityProviderResolver {

    private static final Logger log = Logger.getLogger(DefaultCapabilityProviderResolver.class);
    private static final Pattern CAPABILITIES_SEPARATOR = Pattern.compile("\\s*,\\s*");

    private final Map<String, ArtifactCoords> defaultProviders;
    private final Map<String, String> providedCapabilities;
    private final List<ExtensionRequirement> requirements;
    private int injectionCount;

    /**
     * Creates a new resolver with the given default provider mappings.
     *
     * @param defaultProviders mapping from capability name to the artifact coordinates
     *        of the default provider extension, typically parsed from platform properties
     */
    public DefaultCapabilityProviderResolver(Map<String, ArtifactCoords> defaultProviders) {
        this.defaultProviders = defaultProviders == null ? Map.of() : defaultProviders;
        this.providedCapabilities = new HashMap<>();
        this.requirements = new ArrayList<>();
    }

    /**
     * Registers a capability as provided by the given extension.
     *
     * <p>
     * Called by resolvers when they discover a Quarkus extension that provides
     * an unconditional capability.
     *
     * @param capabilityName the name of the provided capability
     * @param extensionKey the artifact key of the providing extension
     */
    public void registerProvided(String capabilityName, String extensionKey) {
        providedCapabilities.put(capabilityName, extensionKey);
    }

    /**
     * Registers a capability requirement from the given extension, unless the
     * capability is already provided.
     *
     * <p>
     * Called by resolvers when they discover a Quarkus extension that requires
     * an unconditional capability. The BFS path reflects the extension's
     * position in the dependency graph and is used for priority ordering.
     *
     * @param capabilityName the name of the required capability
     * @param extensionKey the artifact key of the requiring extension
     * @param bfsPathSupplier supplier that computes the BFS path of local child indices from root to the requiring extension
     */
    public void registerRequired(String capabilityName, String extensionKey, Supplier<int[]> bfsPathSupplier) {
        if (!providedCapabilities.containsKey(capabilityName)) {
            requirements.add(new ExtensionRequirement(capabilityName, extensionKey, bfsPathSupplier));
        }
    }

    /**
     * Returns the next default provider to inject, or {@code null} if no more
     * unsatisfied requirements have configured default providers.
     *
     * <p>
     * Among unsatisfied requirements with configured default providers, picks the
     * one whose highest-priority requiring extension has the lowest (depth, siblingIndex)
     * by lexicographic comparison. The injection location is always the root dependency node.
     *
     * <p>
     * If unsatisfied requirements exist but none have a configured default provider,
     * a warning is logged for each such requirement.
     *
     * @return the next default provider injection, or {@code null} if done
     */
    public ArtifactCoords getNextDefaultProvider() {
        if (injectionCount >= defaultProviders.size()) {
            logUnsatisfiedWarnings();
            return null;
        }
        removeSatisfiedRequirements();
        if (requirements.isEmpty()) {
            return null;
        }
        Collections.sort(requirements);
        for (ExtensionRequirement req : requirements) {
            final ArtifactCoords provider = defaultProviders.get(req.getCapabilityName());
            if (provider != null) {
                ++injectionCount;
                return provider;
            }
        }
        logUnsatisfiedWarnings();
        return null;
    }

    /**
     * Checks whether any default providers were configured.
     *
     * @return {@code true} if at least one default provider mapping exists
     */
    public boolean hasDefaultProviders() {
        return !defaultProviders.isEmpty();
    }

    /**
     * Parses a comma-separated capabilities string and returns only the
     * unconditional entries (those not containing '?').
     *
     * <p>
     * Conditional capabilities use the syntax {@code capability-name?BooleanSupplierClass}
     * and require build-time evaluation. They are excluded from dependency-resolution-time
     * capability tracking.
     *
     * @param capabilitiesStr comma-separated capabilities string, may be {@code null}
     * @return list of unconditional capability names, never {@code null}
     */
    public static List<String> parseUnconditionalCapabilities(String capabilitiesStr) {
        if (capabilitiesStr == null || capabilitiesStr.isBlank()) {
            return List.of();
        }
        final String[] parts = CAPABILITIES_SEPARATOR.split(capabilitiesStr);
        final List<String> result = new ArrayList<>(parts.length);
        for (String part : parts) {
            if (!part.isEmpty() && !isConditional(part)) {
                result.add(part);
            }
        }
        return result;
    }

    /**
     * Checks whether a capability string contains conditional syntax.
     */
    private static boolean isConditional(String capability) {
        return capability.indexOf('?') >= 0;
    }

    /**
     * Removes requirements that have been satisfied by a provided capability.
     */
    private void removeSatisfiedRequirements() {
        requirements.removeIf(req -> providedCapabilities.containsKey(req.getCapabilityName()));
    }

    /**
     * Logs a warning for each remaining unsatisfied requirement that has no default provider configured.
     */
    private void logUnsatisfiedWarnings() {
        for (ExtensionRequirement req : requirements) {
            if (!providedCapabilities.containsKey(req.getCapabilityName())
                    && !defaultProviders.containsKey(req.getCapabilityName())) {
                log.warnf("Extension %s requires capability %s but no provider or default provider is configured",
                        req.getExtensionArtifactKey(), req.getCapabilityName());
            }
        }
    }
}
