package io.quarkus.platform.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import io.quarkus.platform.tools.ExtensionStructure.ClassFacts;

/**
 * Checks an extension against the structure described in
 * <a href="https://github.com/quarkusio/quarkus/blob/main/adr/0009-extension-structure.adoc">ADR 0009</a>.
 * <p>
 * Package rules only apply to extensions in the {@code io.quarkus} and {@code io.quarkiverse} namespaces, where the
 * root package is derived from the coordinates.
 */
public final class ExtensionStructureChecker {

    private static final String ARTIFACT_ID_PREFIX = "quarkus-";
    private static final String DEPLOYMENT_SUFFIX = "-deployment";
    private static final String PROCESSOR_SUFFIX = "Processor";

    private ExtensionStructureChecker() {
    }

    /**
     * @param structure the extension to check
     * @return the violations found, empty if the extension follows the structure
     */
    public static List<Violation> check(ExtensionStructure structure) {
        List<Violation> violations = new ArrayList<>();
        checkDeploymentArtifactId(structure, violations);
        report(violations, buildStepsIn(structure.runtimeClasses()),
                "Build steps should be declared in the deployment module, not in the runtime module "
                        + structure.runtimeArtifactId());
        report(violations, filter(buildStepsIn(structure.deploymentClasses()),
                c -> !c.simpleName().endsWith(PROCESSOR_SUFFIX)),
                "Build steps of the deployment module " + structure.deploymentArtifactId()
                        + " should be declared in classes whose name ends with " + PROCESSOR_SUFFIX);
        namespace(structure.groupId()).ifPresent(namespace -> checkNamespacedStructure(namespace, structure, violations));
        return violations;
    }

    private static void checkDeploymentArtifactId(ExtensionStructure structure, List<Violation> violations) {
        String expected = structure.runtimeArtifactId() + DEPLOYMENT_SUFFIX;
        if (!expected.equals(structure.deploymentArtifactId())) {
            violations.add(new Violation("The deployment artifact ID should be " + expected,
                    List.of(structure.deploymentArtifactId())));
        }
    }

    private static void checkNamespacedStructure(String namespace, ExtensionStructure structure,
            List<Violation> violations) {
        String runtimeArtifactId = structure.runtimeArtifactId();
        if (!runtimeArtifactId.startsWith(ARTIFACT_ID_PREFIX)) {
            violations.add(new Violation("The runtime artifact ID should start with " + ARTIFACT_ID_PREFIX,
                    List.of(runtimeArtifactId)));
            return;
        }
        List<String> roots = rootPackages(namespace, runtimeArtifactId.substring(ARTIFACT_ID_PREFIX.length()));
        String rootList = String.join(" or ", roots);

        report(violations, filter(structure.runtimeClasses(), c -> !inRuntimePackage(c, roots)),
                "Classes of the runtime module " + runtimeArtifactId + " should be in the " + rootList
                        + " package or its subpackages, except .api, .dev and .deployment");
        report(violations, filter(structure.deploymentClasses(), c -> !inDeploymentPackage(c, roots)),
                "Classes of the deployment module " + structure.deploymentArtifactId() + " should be in the "
                        + suffixed(roots, ".deployment") + " package or its subpackages, except .deployment.spi");
        report(violations, filter(structure.runtimeClasses(), c -> c.nativeImageHook() && !inGraalPackage(c, roots)),
                "GraalVM substitutions and features of the runtime module " + runtimeArtifactId + " should be in the "
                        + suffixed(roots, ".runtime.graal") + " package");
    }

    private static Optional<String> namespace(String groupId) {
        if (groupId.equals("io.quarkus")) {
            return Optional.of("io.quarkus");
        }
        if (groupId.equals("io.quarkiverse") || groupId.startsWith("io.quarkiverse.")) {
            return Optional.of("io.quarkiverse");
        }
        return Optional.empty();
    }

    /**
     * Hyphens in the extension name map to either underscores or dots, e.g. {@code quarkus-hibernate-orm} accepts
     * both {@code io.quarkus.hibernate_orm} and {@code io.quarkus.hibernate.orm}.
     */
    private static List<String> rootPackages(String namespace, String extensionName) {
        String underscored = namespace + "." + extensionName.replace('-', '_');
        String dotted = namespace + "." + extensionName.replace('-', '.');
        return underscored.equals(dotted) ? List.of(underscored) : List.of(underscored, dotted);
    }

    private static boolean inRuntimePackage(ClassFacts c, List<String> roots) {
        String pkg = c.packageName();
        for (String root : roots) {
            if (isIn(pkg, root) && !isIn(pkg, root + ".api") && !isIn(pkg, root + ".dev")
                    && !isIn(pkg, root + ".deployment")) {
                return true;
            }
        }
        return false;
    }

    private static boolean inDeploymentPackage(ClassFacts c, List<String> roots) {
        String pkg = c.packageName();
        for (String root : roots) {
            if (isIn(pkg, root + ".deployment") && !isIn(pkg, root + ".deployment.spi")) {
                return true;
            }
        }
        return false;
    }

    private static boolean inGraalPackage(ClassFacts c, List<String> roots) {
        for (String root : roots) {
            if (isIn(c.packageName(), root + ".runtime.graal")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isIn(String pkg, String expected) {
        return pkg.equals(expected) || pkg.startsWith(expected + ".");
    }

    private static String suffixed(List<String> roots, String suffix) {
        return String.join(" or ", roots.stream().map(r -> r + suffix).toList());
    }

    private static List<ClassFacts> buildStepsIn(List<ClassFacts> classes) {
        return filter(classes, ClassFacts::declaresBuildSteps);
    }

    private static List<ClassFacts> filter(List<ClassFacts> classes, Predicate<ClassFacts> predicate) {
        return classes.stream().filter(predicate).toList();
    }

    private static void report(List<Violation> violations, List<ClassFacts> offenders, String message) {
        if (!offenders.isEmpty()) {
            violations.add(new Violation(message, offenders.stream().map(ClassFacts::name).toList()));
        }
    }

    /**
     * A rule broken by an extension.
     *
     * @param message what the rule expects
     * @param offenders the classes or artifact IDs breaking the rule
     */
    public record Violation(String message, List<String> offenders) {

        static final int MAX_DESCRIBED_OFFENDERS = 5;

        public Violation {
            offenders = List.copyOf(offenders);
        }

        /**
         * @return the message followed by at most {@value #MAX_DESCRIBED_OFFENDERS} offenders
         */
        public String describe() {
            StringBuilder sb = new StringBuilder(message).append(": ")
                    .append(String.join(", ", offenders.subList(0, Math.min(offenders.size(), MAX_DESCRIBED_OFFENDERS))));
            if (offenders.size() > MAX_DESCRIBED_OFFENDERS) {
                sb.append(" and ").append(offenders.size() - MAX_DESCRIBED_OFFENDERS).append(" more");
            }
            return sb.toString();
        }
    }
}
