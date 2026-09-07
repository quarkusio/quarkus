package io.quarkus.avro.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.function.Consumer;

import org.apache.avro.specific.AvroGenerated;
import org.apache.avro.util.ClassSecurityValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.avro.deployment.pkgtrust.TrustedByPackageClass;
import io.quarkus.avro.spi.AvroTrustedClassBuildItem;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that the {@code AvroRecorder} installs a global Avro {@link ClassSecurityValidator} that trusts
 * Avro-generated classes, classes marked by {@link AvroTrustedClassBuildItem} (as produced by other extensions such as
 * gRPC and Pulsar), and the classes and packages configured via {@code quarkus.avro.trusted-classes} and
 * {@code quarkus.avro.trusted-packages}.
 */
public class AvroClassSecurityValidatorTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.avro.trusted-classes", ExplicitlyTrustedClass.class.getName())
            .overrideConfigKey("quarkus.avro.trusted-packages", "io.quarkus.avro.deployment.pkgtrust")
            .addBuildChainCustomizer(produceTrustedClassBuildItem(TrustedViaBuildItemClass.class.getName()))
            .withApplicationRoot(jar -> jar.addClasses(
                    GeneratedRecord.class,
                    ExplicitlyTrustedClass.class,
                    TrustedByPackageClass.class,
                    TrustedViaBuildItemClass.class));

    static Consumer<BuildChainBuilder> produceTrustedClassBuildItem(String className) {
        return chainBuilder -> chainBuilder.addBuildStep(new BuildStep() {
            @Override
            public void execute(BuildContext context) {
                context.produce(new AvroTrustedClassBuildItem(className));
            }
        }).produces(AvroTrustedClassBuildItem.class).build();
    }

    @Test
    void avroGeneratedClassesAreTrusted() {
        assertThat(ClassSecurityValidator.getGlobal().isTrusted(GeneratedRecord.class)).isTrue();
    }

    @Test
    void classesMarkedByBuildItemAreTrusted() {
        assertThat(ClassSecurityValidator.getGlobal().isTrusted(TrustedViaBuildItemClass.class)).isTrue();
    }

    @Test
    void explicitlyConfiguredClassesAreTrusted() {
        assertThat(ClassSecurityValidator.getGlobal().isTrusted(ExplicitlyTrustedClass.class)).isTrue();
    }

    @Test
    void classesInConfiguredPackagesAreTrusted() {
        assertThat(ClassSecurityValidator.getGlobal().isTrusted(TrustedByPackageClass.class)).isTrue();
    }

    @Test
    void avroDefaultTrustedTypesStillWork() {
        assertThat(ClassSecurityValidator.getGlobal().isTrusted(String.class)).isTrue();
    }

    @Test
    void untrustedClassesAreRejected() {
        assertThat(ClassSecurityValidator.getGlobal().isTrusted(File.class)).isFalse();
    }

    @AvroGenerated
    public static class GeneratedRecord {
    }

    public static class ExplicitlyTrustedClass {
    }

    public static class TrustedViaBuildItemClass {
    }
}
