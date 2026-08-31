package io.quarkus.sbom;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class ComponentDescriptorTest {

    @Test
    void topLevelDefaultsToFalse() {
        ComponentDescriptor descriptor = ComponentDescriptor.builder()
                .setPurl(Purl.npm(null, "lodash", "4.17.21"))
                .build();
        assertThat(descriptor.isTopLevel()).isFalse();
    }

    @Test
    void topLevelSetToTrue() {
        ComponentDescriptor descriptor = ComponentDescriptor.builder()
                .setPurl(Purl.npm(null, "lodash", "4.17.21"))
                .setTopLevel(true)
                .build();
        assertThat(descriptor.isTopLevel()).isTrue();
    }

    @Test
    void topLevelPreservedByCopyBuilder() {
        ComponentDescriptor original = ComponentDescriptor.builder()
                .setPurl(Purl.npm(null, "lodash", "4.17.21"))
                .setTopLevel(true)
                .build();
        ComponentDescriptor copy = new ComponentDescriptor.Builder(original).build();
        assertThat(copy.isTopLevel()).isTrue();
    }

    @Test
    void licensesDefaultToEmptyList() {
        ComponentDescriptor descriptor = ComponentDescriptor.builder()
                .setPurl(Purl.npm(null, "lodash", "4.17.21"))
                .build();
        assertThat(descriptor.getLicenses()).isEmpty();
    }

    @Test
    void addLicense() {
        ComponentDescriptor descriptor = ComponentDescriptor.builder()
                .setPurl(Purl.npm(null, "lodash", "4.17.21"))
                .addLicense(new LicenseInfo("MIT"))
                .build();
        assertThat(descriptor.getLicenses()).containsExactly(new LicenseInfo("MIT"));
    }

    @Test
    void setMultipleLicenses() {
        ComponentDescriptor descriptor = ComponentDescriptor.builder()
                .setPurl(Purl.npm(null, "lodash", "4.17.21"))
                .setLicenses(List.of(new LicenseInfo("Apache-2.0"), new LicenseInfo("MIT")))
                .build();
        assertThat(descriptor.getLicenses())
                .containsExactly(new LicenseInfo("Apache-2.0"), new LicenseInfo("MIT"));
    }

    @Test
    void licensesPreservedByCopyBuilder() {
        ComponentDescriptor original = ComponentDescriptor.builder()
                .setPurl(Purl.npm(null, "lodash", "4.17.21"))
                .addLicense(new LicenseInfo("MIT"))
                .addLicense(new LicenseInfo("Apache-2.0"))
                .build();
        ComponentDescriptor copy = new ComponentDescriptor.Builder(original).build();
        assertThat(copy.getLicenses())
                .containsExactly(new LicenseInfo("MIT"), new LicenseInfo("Apache-2.0"));
    }

    @Test
    void copyBuilderCanAddLicenses() {
        ComponentDescriptor original = ComponentDescriptor.builder()
                .setPurl(Purl.npm(null, "lodash", "4.17.21"))
                .addLicense(new LicenseInfo("MIT"))
                .build();
        ComponentDescriptor modified = new ComponentDescriptor.Builder(original)
                .addLicense(new LicenseInfo("Apache-2.0"))
                .build();
        assertThat(modified.getLicenses())
                .containsExactly(new LicenseInfo("MIT"), new LicenseInfo("Apache-2.0"));
        // original is unchanged
        assertThat(original.getLicenses()).containsExactly(new LicenseInfo("MIT"));
    }

    @Test
    void builtLicensesAreImmutable() {
        ComponentDescriptor descriptor = ComponentDescriptor.builder()
                .setPurl(Purl.npm(null, "lodash", "4.17.21"))
                .addLicense(new LicenseInfo("MIT"))
                .build();
        assertThat(descriptor.getLicenses()).isUnmodifiable();
    }

    @Test
    void productFieldsAndOverridesAreCarried() {
        ComponentDescriptor d = ComponentDescriptor.builder()
                .setPurl(Purl.maven("com.redhat.quarkus.platform", "quarkus-camel-bom", "3.20.0", "pom", null))
                .setCpe("cpe:2.3:a:redhat:camel_quarkus:3.20:*:*:*:*:*:*:*")
                .setComponentType("framework")
                .setName("Camel Extensions for Quarkus")
                .setVersion("3.20")
                .setScope(ComponentDescriptor.SCOPE_EXCLUDED)
                .build();

        assertThat(d.getCpe()).isEqualTo("cpe:2.3:a:redhat:camel_quarkus:3.20:*:*:*:*:*:*:*");
        assertThat(d.getComponentType()).isEqualTo("framework");
        assertThat(d.getName()).isEqualTo("Camel Extensions for Quarkus");
        assertThat(d.getVersion()).isEqualTo("3.20");
        assertThat(d.getScope()).isEqualTo(ComponentDescriptor.SCOPE_EXCLUDED);

        // copy-constructor preserves the new fields
        ComponentDescriptor copy = new ComponentDescriptor.Builder(d).build();
        assertThat(copy.getCpe()).isEqualTo(d.getCpe());
        assertThat(copy.getComponentType()).isEqualTo("framework");
        assertThat(copy.getName()).isEqualTo("Camel Extensions for Quarkus");
        assertThat(copy.getVersion()).isEqualTo("3.20");
    }

    @Test
    void nameAndVersionFallBackToPurlWhenNoOverride() {
        ComponentDescriptor d = ComponentDescriptor.builder()
                .setPurl(Purl.maven("io.quarkus", "quarkus-rest", "3.20.0", "jar", null))
                .build();
        assertThat(d.getName()).isEqualTo("quarkus-rest");
        assertThat(d.getVersion()).isEqualTo("3.20.0");
        assertThat(d.getCpe()).isNull();
        assertThat(d.getComponentType()).isNull();
    }
}
