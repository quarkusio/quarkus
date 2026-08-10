package io.quarkus.produi.runtime.advisor;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure, read-only production-readiness and security checks for Prod UI.
 * <p>
 * This is the production-safe counterpart of BootUI's scoring/advisor panels. Each check turns already-extracted,
 * non-sensitive facts about the running application into a {@link Check} with a {@link Status} and a human-readable
 * detail. The logic is kept here - free of any config or CDI dependency - so it can be unit-tested in isolation; the
 * {@code AdvisorProdUIService} does the (untestable) config extraction and calls into these methods.
 * <p>
 * Nothing here reads or emits secret <em>values</em>: the plaintext-secret check is fed only the property <em>names</em>
 * of secrets already detected (and never shown) by {@link io.quarkus.produi.api.SecretMasker}.
 */
public final class ProdUIAdvisor {

    /** Outcome of a single check. {@code WARN} is a soft finding worth attention; {@code FAIL} is a clear problem. */
    public enum Status {
        PASS,
        WARN,
        FAIL
    }

    /** Broad grouping shown in the UI. */
    public static final String SECURITY = "Security";
    public static final String READINESS = "Readiness";

    /**
     * A single advisor finding.
     *
     * @param id stable identifier (kebab-case)
     * @param category one of {@link #SECURITY} / {@link #READINESS}
     * @param title short human-readable summary
     * @param status the outcome
     * @param detail one-line explanation, safe to display (never contains secret values)
     */
    public record Check(String id, String category, String title, Status status, String detail) {
    }

    private ProdUIAdvisor() {
    }

    /**
     * Prod UI itself should carry an authorization gate ({@code quarkus.prod-ui.roles-allowed}) so that access does not
     * rely solely on whatever secures the interface it is served on.
     */
    public static Check prodUiSecured(boolean rolesAllowedConfigured) {
        if (rolesAllowedConfigured) {
            return new Check("prod-ui-secured", SECURITY, "Prod UI access is role-restricted",
                    Status.PASS, "quarkus.prod-ui.roles-allowed is set, so Prod UI requires an authorized role.");
        }
        return new Check("prod-ui-secured", SECURITY, "Prod UI has no role restriction",
                Status.WARN,
                "quarkus.prod-ui.roles-allowed is empty; access depends entirely on how the interface is secured.");
    }

    /**
     * When Prod UI is exposed on the management interface, that interface must be secured; an enabled but unsecured
     * management interface exposes Prod UI (and other management endpoints) to anyone who can reach the port.
     */
    public static Check managementInterfaceAuth(boolean managementEnabled, boolean managementAuthConfigured) {
        if (!managementEnabled) {
            return new Check("management-auth", SECURITY, "Management interface disabled",
                    Status.PASS, "The management interface is not enabled.");
        }
        if (managementAuthConfigured) {
            return new Check("management-auth", SECURITY, "Management interface is secured",
                    Status.PASS, "The management interface has an authorization policy configured.");
        }
        return new Check("management-auth", SECURITY, "Management interface is not secured",
                Status.FAIL,
                "The management interface is enabled but no authorization is configured; secure it with "
                        + "quarkus.management.auth.* or quarkus.prod-ui.roles-allowed.");
    }

    /**
     * Flags credential-bearing properties that live in a plaintext config source. Only property <em>names</em> are
     * passed in and shown - the values are never handled here.
     */
    public static Check plaintextSecrets(List<String> secretPropertyNames) {
        List<String> names = secretPropertyNames == null ? List.of() : secretPropertyNames;
        if (names.isEmpty()) {
            return new Check("plaintext-secrets", SECURITY, "No plaintext secrets detected",
                    Status.PASS, "No credential-bearing properties were found in plaintext config sources.");
        }
        return new Check("plaintext-secrets", SECURITY, "Plaintext secrets in configuration",
                Status.WARN,
                names.size() + " credential-bearing propert" + (names.size() == 1 ? "y is" : "ies are")
                        + " stored in a plaintext config source: " + String.join(", ", names)
                        + ". Consider a vault or keystore.");
    }

    /**
     * Flags development-oriented settings that are risky in production (e.g. schema drop-and-create, always-on API
     * docs). Only property names are passed in.
     */
    public static Check devFeaturesInProduction(List<String> riskyPropertyNames) {
        List<String> names = riskyPropertyNames == null ? List.of() : riskyPropertyNames;
        if (names.isEmpty()) {
            return new Check("dev-features", READINESS, "No development features enabled",
                    Status.PASS, "No development-oriented settings were detected in this configuration.");
        }
        return new Check("dev-features", READINESS, "Development features enabled in production",
                Status.WARN,
                names.size() + " development-oriented setting" + (names.size() == 1 ? " is" : "s are")
                        + " active: " + String.join(", ", names) + ".");
    }

    /**
     * A single readiness score in {@code [0, 100]}: {@link Status#PASS} counts full, {@link Status#WARN} half,
     * {@link Status#FAIL} nothing. An empty list scores 100.
     */
    public static int score(List<Check> checks) {
        if (checks == null || checks.isEmpty()) {
            return 100;
        }
        double sum = 0;
        for (Check check : checks) {
            sum += switch (check.status()) {
                case PASS -> 1.0;
                case WARN -> 0.5;
                case FAIL -> 0.0;
            };
        }
        return (int) Math.round(100.0 * sum / checks.size());
    }

    /**
     * Convenience aggregate that assembles the standard checks from already-extracted facts, in display order.
     */
    public static List<Check> checks(boolean rolesAllowedConfigured,
            boolean managementEnabled,
            boolean managementAuthConfigured,
            List<String> secretPropertyNames,
            List<String> riskyPropertyNames) {
        List<Check> checks = new ArrayList<>();
        checks.add(prodUiSecured(rolesAllowedConfigured));
        checks.add(managementInterfaceAuth(managementEnabled, managementAuthConfigured));
        checks.add(plaintextSecrets(secretPropertyNames));
        checks.add(devFeaturesInProduction(riskyPropertyNames));
        return checks;
    }
}
