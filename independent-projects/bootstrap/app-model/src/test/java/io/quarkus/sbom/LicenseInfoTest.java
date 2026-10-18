package io.quarkus.sbom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class LicenseInfoTest {

    @Test
    void nameAccessor() {
        LicenseInfo license = new LicenseInfo("Apache-2.0");
        assertThat(license.name()).isEqualTo("Apache-2.0");
    }

    @Test
    void rejectsNullName() {
        assertThatThrownBy(() -> new LicenseInfo(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void toStringReturnsName() {
        assertThat(new LicenseInfo("MIT")).hasToString("MIT");
    }

    @Test
    void equalityByName() {
        LicenseInfo a = new LicenseInfo("MIT");
        LicenseInfo b = new LicenseInfo("MIT");
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void differentNamesAreNotEqual() {
        assertThat(new LicenseInfo("MIT")).isNotEqualTo(new LicenseInfo("Apache-2.0"));
    }

    @Test
    void forNameSetsNameAndNullUrl() {
        LicenseInfo license = LicenseInfo.forName("MIT");
        assertThat(license.name()).isEqualTo("MIT");
        assertThat(license.url()).isNull();
    }

    @Test
    void urlDefaultsToNull() {
        assertThat(new LicenseInfo("MIT").url()).isNull();
    }

    @Test
    void urlAccessor() {
        LicenseInfo license = new LicenseInfo("Apache-2.0", "https://www.apache.org/licenses/LICENSE-2.0");
        assertThat(license.url()).isEqualTo("https://www.apache.org/licenses/LICENSE-2.0");
    }
}
