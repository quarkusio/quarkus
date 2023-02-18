package io.quarkus.deployment.pkg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class BuiltInTypeTest {

    @ParameterizedTest
    @EnumSource(PackageConfig.BuiltInType.class)
    void packageTypeConversion(PackageConfig.BuiltInType packageType) {
        assertThat(PackageConfig.BuiltInType.fromString(packageType.toString())).isSameAs(packageType);
    }

    @Test
    void invalidPackageType() {
        assertThatIllegalArgumentException().isThrownBy(() -> PackageConfig.BuiltInType.fromString("not-a-package-type"))
                .withMessage("Unknown Quarkus package type 'not-a-package-type'");
    }
}
