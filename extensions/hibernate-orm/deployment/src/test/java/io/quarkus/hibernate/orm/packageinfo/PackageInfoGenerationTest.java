package io.quarkus.hibernate.orm.packageinfo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.packageinfo.withoutpackageinfo.EntityWithoutPackageInfo;
import io.quarkus.hibernate.orm.packageinfo.withpackageinfo.EntityWithPackageInfo;
import io.quarkus.hibernate.orm.packageinfo.withpackageinfo.TestAnnotation;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests that package-info files are generated for packages with entities when AOT class loading is enabled.
 * - Existing package-info files should NOT be overwritten
 * - Missing package-info files should be generated (empty)
 */
public class PackageInfoGenerationTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addPackage(EntityWithPackageInfo.class.getPackage())
                    .addPackage(EntityWithoutPackageInfo.class.getPackage()))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.package.jar.enabled", "true")
            .overrideConfigKey("quarkus.package.jar.type", "aot-jar");

    @Test
    public void testExistingPackageInfoNotOverwritten() throws Exception {
        // Verify that the existing package-info with @TestAnnotation is preserved
        Class<?> packageInfoClass = Class.forName(
                "io.quarkus.hibernate.orm.packageinfo.withpackageinfo.package-info", true,
                Thread.currentThread().getContextClassLoader());
        assertThat(packageInfoClass).isNotNull();

        // Check that our custom annotation is still present
        TestAnnotation annotation = packageInfoClass.getAnnotation(TestAnnotation.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("original");
    }

    @Test
    public void testMissingPackageInfoGenerated() throws Exception {
        // Verify that a package-info was generated for the package without one
        Class<?> packageInfoClass = Class.forName(
                "io.quarkus.hibernate.orm.packageinfo.withoutpackageinfo.package-info", true,
                Thread.currentThread().getContextClassLoader());
        assertThat(packageInfoClass).isNotNull();

        // It should be a synthetic interface
        assertThat(packageInfoClass.isInterface()).isTrue();
        assertThat(packageInfoClass.isSynthetic()).isTrue();
    }
}
