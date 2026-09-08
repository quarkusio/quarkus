package io.quarkus.sbom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.PlatformImports;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependency;

/**
 * Adds platform-member product components to an {@link ApplicationManifest}.
 * <p>
 * When a platform member declares a product CPE (and, optionally, the artifacts attributed to it via
 * the {@code cpe-artifacts} platform property), a top-level component of type
 * {@link ApplicationComponent#TYPE_FRAMEWORK} is added for that product. The product component carries
 * the CPE, is scoped {@link ApplicationComponent#SCOPE_EXCLUDED}, {@code provides} the attributed
 * artifacts that are actually present in the application (CycloneDX 1.6 {@code dependency.provides}),
 * and is linked as a dependency of the main application component.
 * <p>
 * Products identified by the same product coordinates and CPE are merged into a single component whose
 * attributed artifacts are the union of the members' contributions.
 */
public final class ProductAttribution {

    private ProductAttribution() {
    }

    /**
     * Returns a manifest augmented with platform-member product components, or the given manifest
     * unchanged when there is nothing to attribute.
     *
     * @param manifest the application manifest to augment
     * @param model the application model providing platform properties and dependencies
     * @return the augmented manifest, or the original when no products are attributed
     */
    public static ApplicationManifest augment(ApplicationManifest manifest, ApplicationModel model) {
        if (manifest == null || model == null) {
            return manifest;
        }
        final Map<String, String> props = model.getPlatformProperties();
        if (props == null || props.isEmpty()) {
            return manifest;
        }
        final PlatformImports platforms = model.getPlatforms();
        if (platforms == null || platforms.getImportedPlatformBoms() == null) {
            return manifest;
        }

        // present component coords and used runtime extension coords, collected lazily once a CPE is found
        Set<ArtifactCoords> presentCoords = null;
        List<ArtifactCoords> presentRtExtCoords = null;

        // products keyed by product coordinates + CPE so that members sharing both are merged
        final Map<String, Product> products = new LinkedHashMap<>();
        for (ArtifactCoords memberBom : platforms.getImportedPlatformBoms()) {
            final String prefix = BootstrapConstants.PLATFORM_PROPERTY_PREFIX
                    + memberBom.getGroupId() + "." + memberBom.getArtifactId() + ".";
            final String cpe = value(props, prefix + "cpe");
            if (cpe == null) {
                continue;
            }
            if (presentCoords == null) {
                presentCoords = collectPresentCoords(manifest);
                presentRtExtCoords = collectPresentRtExtCoords(model);
            }

            final Map<ArtifactCoords, List<ArtifactCoords>> originalExtDeps = decodeCpeArtifacts(
                    props.get(prefix + "cpe-artifacts"));
            final ArtifactCoords productCoords = resolveProductCoords(memberBom, prefix, props);
            final String key = productCoords.toGACTVString() + "#" + cpe;
            final Product product = products.computeIfAbsent(key, k -> new Product(productCoords, cpe, prefix));

            for (ArtifactCoords rtExtCoords : presentRtExtCoords) {
                final List<ArtifactCoords> deps = originalExtDeps.get(rtExtCoords);
                if (deps != null) {
                    for (ArtifactCoords coords : deps) {
                        product.attribute(coords, presentCoords);
                    }
                }
            }
        }

        if (products.isEmpty()) {
            return manifest;
        }
        return rebuild(manifest, products.values(), props);
    }

    private static ApplicationManifest rebuild(ApplicationManifest manifest, Collection<Product> products,
            Map<String, String> props) {
        // link the products as dependencies of the main application component
        final ApplicationComponent main = manifest.getMainComponent();
        final List<ArtifactCoords> mainDeps = new ArrayList<>(main.getDependencies());
        for (Product p : products) {
            mainDeps.add(p.coordinates);
        }
        final ApplicationComponent newMain = new ApplicationComponent.Builder(main)
                .setDependencies(mainDeps)
                .build();

        final ApplicationManifest.Builder builder = ApplicationManifest.builder()
                .setMainComponent(newMain)
                .setRunnerPath(manifest.getRunnerPath());
        for (ApplicationComponent c : manifest.getComponents()) {
            builder.addComponent(c);
        }
        for (Product p : products) {
            builder.addComponent(p.toComponent(props));
        }
        return builder.build();
    }

    private static Set<ArtifactCoords> collectPresentCoords(ApplicationManifest manifest) {
        final Set<ArtifactCoords> present = new HashSet<>();
        for (ApplicationComponent c : manifest.getComponents()) {
            final ArtifactCoords coords = c.getCoordinates();
            if (coords != null) {
                present.add(coords);
            }
        }
        return present;
    }

    private static List<ArtifactCoords> collectPresentRtExtCoords(ApplicationModel model) {
        final List<ArtifactCoords> coords = new ArrayList<>();
        for (ResolvedDependency dep : model.getDependenciesWithAnyFlag(DependencyFlags.RUNTIME_EXTENSION_ARTIFACT)) {
            coords.add(normalize(dep));
        }
        return coords;
    }

    private static ArtifactCoords normalize(ArtifactCoords c) {
        return ArtifactCoords.of(c.getGroupId(), c.getArtifactId(), c.getClassifier(), c.getType(), c.getVersion());
    }

    private static ArtifactCoords resolveProductCoords(ArtifactCoords memberBom, String prefix, Map<String, String> props) {
        final String version = value(props, prefix + "product-version");
        return ArtifactCoords.pom(memberBom.getGroupId(), memberBom.getArtifactId(),
                version == null ? memberBom.getVersion() : version);
    }

    private static Map<ArtifactCoords, List<ArtifactCoords>> decodeCpeArtifacts(String encoded) {
        return encoded == null || encoded.isBlank() ? Map.of() : CpeArtifactsEncoder.decode(encoded);
    }

    private static String value(Map<String, String> props, String key) {
        final String v = props.get(key);
        return v == null || v.isBlank() ? null : v;
    }

    /**
     * Accumulates the artifacts attributed to a single product (identified by coordinates + CPE),
     * merging the contributions of every platform member that maps to it.
     */
    private static final class Product {
        final ArtifactCoords coordinates;
        final String cpe;
        // prefix of the first member seen for this product, used to read the product metadata properties
        final String prefix;
        final List<ArtifactCoords> attributed = new ArrayList<>();
        final Set<ArtifactCoords> seen = new HashSet<>();

        Product(ArtifactCoords coordinates, String cpe, String prefix) {
            this.coordinates = coordinates;
            this.cpe = cpe;
            this.prefix = prefix;
        }

        void attribute(ArtifactCoords coords, Set<ArtifactCoords> presentCoords) {
            final ArtifactCoords normalized = normalize(coords);
            if (presentCoords.contains(normalized) && seen.add(normalized)) {
                attributed.add(normalized);
            }
        }

        ApplicationComponent toComponent(Map<String, String> props) {
            final String type = value(props, prefix + "product-type");
            final ApplicationComponent.Builder builder = ApplicationComponent.builder()
                    .setCoordinates(coordinates)
                    .setCpe(cpe)
                    .setScope(ApplicationComponent.SCOPE_EXCLUDED)
                    .setType(type == null ? ApplicationComponent.TYPE_FRAMEWORK : type);
            final String name = value(props, prefix + "product-name");
            if (name != null) {
                builder.setName(name);
            }
            final String description = value(props, prefix + "product-description");
            if (description != null) {
                builder.setDescription(description);
            }
            if (!attributed.isEmpty()) {
                builder.setProvides(attributed);
            }
            return builder.build();
        }
    }
}
