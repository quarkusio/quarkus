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
}
