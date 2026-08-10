package io.quarkus.produi.runtime.advisor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.produi.api.SecretMasker;
import io.quarkus.produi.runtime.advisor.ProdUIAdvisor.Check;
import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.config.SmallRyeConfig;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Read-only Prod UI production-readiness / security advisor. It surfaces exactly the kind of misconfiguration a prod
 * console should flag - an unsecured management interface, plaintext secrets in a config source, development features
 * left on in production - and gives a single readiness score.
 * <p>
 * All scoring/decision logic lives in the pure, unit-tested {@link ProdUIAdvisor}. This service only extracts
 * non-sensitive facts from the running configuration and hands them over. Secrets are detected via
 * {@link SecretMasker} and reported by <em>name</em> only; no secret value is ever read out or returned.
 */
@ApplicationScoped
public class AdvisorProdUIService {

    // Config-source names that store values in the clear. Everything else (e.g. a vault or keystore source) is treated
    // as secure and its secrets are not flagged.
    private static final List<String> SECURE_SOURCE_FRAGMENTS = List.of("vault", "keystore", "key-store");

    // Property (name suffix, risky values) pairs that indicate a development feature left enabled in production.
    private static final List<String> RISKY_SCHEMA_VALUES = List.of("drop-and-create", "create", "drop");

    @NonBlocking
    @JsonRpcUsage({ Usage.PROD_UI })
    @JsonRpcDescription("Run the read-only production-readiness and security checks and return them with an overall score")
    public JsonObject getReadinessChecks() {
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);

        boolean rolesAllowed = !config.getOptionalValues("quarkus.prod-ui.roles-allowed", String.class)
                .orElse(List.of()).isEmpty();
        boolean managementEnabled = config.getOptionalValue("quarkus.management.enabled", Boolean.class).orElse(false);
        boolean managementAuth = isManagementAuthConfigured(config, rolesAllowed);
        List<String> secretNames = plaintextSecretNames(config);
        List<String> riskyNames = riskyDevFeatureNames(config);

        List<Check> checks = ProdUIAdvisor.checks(rolesAllowed, managementEnabled, managementAuth, secretNames,
                riskyNames);

        JsonArray checksJson = new JsonArray();
        for (Check check : checks) {
            checksJson.add(new JsonObject()
                    .put("id", check.id())
                    .put("category", check.category())
                    .put("title", check.title())
                    .put("status", check.status().name())
                    .put("detail", check.detail()));
        }
        return new JsonObject()
                .put("score", ProdUIAdvisor.score(checks))
                .put("checks", checksJson);
    }

    private boolean isManagementAuthConfigured(SmallRyeConfig config, boolean rolesAllowed) {
        if (rolesAllowed) {
            // Prod UI's own roles-allowed contributes a management auth permission over its routes.
            return true;
        }
        if (config.getOptionalValue("quarkus.management.auth.basic", Boolean.class).orElse(false)) {
            return true;
        }
        for (String name : allPropertyNames(config)) {
            if (name.startsWith("quarkus.management.auth.permission.")) {
                return true;
            }
        }
        return false;
    }

    private List<String> plaintextSecretNames(SmallRyeConfig config) {
        TreeSet<String> names = new TreeSet<>();
        for (String name : allPropertyNames(config)) {
            if (name.isEmpty() || name.startsWith("%")) {
                continue;
            }
            var configValue = config.getConfigValue(name);
            if (configValue == null) {
                continue;
            }
            String value = configValue.getValue();
            if (value == null || value.isEmpty()) {
                continue;
            }
            if (SecretMasker.isSecret(name, value) && !isSecureSource(configValue.getSourceName())) {
                names.add(name);
            }
        }
        return new ArrayList<>(names);
    }

    private boolean isSecureSource(String sourceName) {
        if (sourceName == null) {
            return false;
        }
        String lower = sourceName.toLowerCase(Locale.ROOT);
        for (String fragment : SECURE_SOURCE_FRAGMENTS) {
            if (lower.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    private List<String> riskyDevFeatureNames(SmallRyeConfig config) {
        TreeSet<String> names = new TreeSet<>();
        for (String name : allPropertyNames(config)) {
            if (name.isEmpty() || name.startsWith("%")) {
                continue;
            }
            if (isRiskySchemaGeneration(config, name) || isSwaggerAlwaysIncluded(config, name)) {
                names.add(name);
            }
        }
        return new ArrayList<>(names);
    }

    private boolean isRiskySchemaGeneration(SmallRyeConfig config, String name) {
        // Covers quarkus.hibernate-orm[.<pu>].database.generation and
        // quarkus.hibernate-search-orm[.<pu>].schema-management.strategy.
        boolean schemaProperty = name.endsWith(".database.generation")
                || name.endsWith(".schema-management.strategy");
        if (!schemaProperty) {
            return false;
        }
        String value = config.getOptionalValue(name, String.class).orElse("");
        return RISKY_SCHEMA_VALUES.contains(value.toLowerCase(Locale.ROOT));
    }

    private boolean isSwaggerAlwaysIncluded(SmallRyeConfig config, String name) {
        return name.equals("quarkus.swagger-ui.always-include")
                && config.getOptionalValue(name, Boolean.class).orElse(false);
    }

    private TreeSet<String> allPropertyNames(SmallRyeConfig config) {
        TreeSet<String> names = new TreeSet<>();
        for (String name : config.getPropertyNames()) {
            names.add(name);
        }
        for (var source : config.getConfigSources()) {
            names.addAll(source.getPropertyNames());
        }
        return names;
    }
}
