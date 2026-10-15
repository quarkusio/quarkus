package io.quarkus.avro.runtime;

import java.util.List;
import java.util.Set;

import org.apache.avro.util.ClassSecurityValidator.ClassSecurityPredicate;

/**
 * An Avro {@link ClassSecurityPredicate} that decides which classes are trusted for Avro deserialization.
 * <p>
 * Starting with Avro 1.12.2, classes are validated against a global {@link org.apache.avro.util.ClassSecurityValidator}
 * before they are deserialized. This predicate trusts:
 * <ul>
 * <li>classes whose fully qualified name is in the trusted set (collected at build time from
 * {@code io.quarkus.avro.spi.AvroTrustedClassBuildItem} — e.g. Avro-generated records and Protobuf-generated messages —
 * plus the classes listed in {@code quarkus.avro.trusted-classes}),</li>
 * <li>classes belonging to a package (or any of its sub-packages) listed in {@code quarkus.avro.trusted-packages}.</li>
 * </ul>
 * Package matching respects package boundaries: {@code org.acme} trusts {@code org.acme.Foo} and
 * {@code org.acme.sub.Bar}, but not {@code org.acme2.Foo}.
 */
public final class QuarkusAvroClassSecurityPredicate implements ClassSecurityPredicate {

    private final Set<String> trustedClassNames;
    private final List<String> trustedPackagePrefixes;

    /**
     * @param trustedClassNames fully qualified names of classes that are trusted
     * @param trustedPackages package names whose classes (and sub-package classes) are trusted
     */
    public QuarkusAvroClassSecurityPredicate(Set<String> trustedClassNames, List<String> trustedPackages) {
        this.trustedClassNames = trustedClassNames;
        // Pre-compute "package." prefixes so that matching respects package boundaries,
        // i.e. "org.acme" trusts "org.acme.Foo" but not "org.acme2.Foo".
        this.trustedPackagePrefixes = trustedPackages.stream().map(p -> p + ".").toList();
    }

    @Override
    public boolean isTrusted(Class<?> clazz) {
        String className = clazz.getName();
        if (trustedClassNames.contains(className)) {
            return true;
        }
        for (String prefix : trustedPackagePrefixes) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
