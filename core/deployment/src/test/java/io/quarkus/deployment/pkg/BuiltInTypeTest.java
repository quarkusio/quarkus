package io.quarkus.deployment.pkg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class BuiltInTypeTest {

    @ParameterizedTest
    @EnumSource(PackageConfig.JarConfig.JarType.class)
    void packageTypeConversion(PackageConfig.JarConfig.JarType packageType) {
        assertThat(PackageConfig.JarConfig.JarType.fromString(packageType.toString())).isSameAs(packageType);
    }

    @Test
    void invalidPackageType() {
        assertThatIllegalArgumentException().isThrownBy(() -> PackageConfig.JarConfig.JarType.fromString("not-a-package-type"))
                .withMessage("Unknown JAR package type 'not-a-package-type'");
    }
}
