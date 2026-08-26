package io.quarkus.bootstrap.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.bootstrap.model.PlatformImports;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;

class ApplicationModelRelocationTest {

    @TempDir
    Path tempDir;

    /**
     * The point of the exercise: the same model, built in two different places, has to serialize to the
     * same bytes.
     */
    @Test
    void modelsFromDifferentCheckoutsSerializeIdentically() throws IOException {
        Path checkoutA = tempDir.resolve("slot-a");
        Path checkoutB = tempDir.resolve("slot-b");

        String serializedA = serializeAt(checkoutA);
        String serializedB = serializeAt(checkoutB);

        assertThat(serializedA).isEqualTo(serializedB);
        assertThat(serializedA).doesNotContain(checkoutA.toString(), checkoutB.toString());
    }

    /**
     * A relocated model has to come back with paths that are absolute again, and correct for the
     * environment reading it.
     */
    @Test
    void relocatedModelIsResolvedBackToAbsolutePaths() throws IOException {
        Path checkout = tempDir.resolve("slot-a");
        Path file = tempDir.resolve("model-a.dat");
        ApplicationModelSerializer.serialize(modelAt(checkout), file, roots(checkout));

        ApplicationModel deserialized = ApplicationModelSerializer.deserialize(file, roots(checkout));

        assertThat(deserialized.getAppArtifact().getResolvedPaths().getSinglePath())
                .isEqualTo(checkout.resolve("target/classes"));
    }

    /**
     * The same serialized model read against a different project location resolves to that location -
     * this is what makes a cache entry produced elsewhere usable here.
     */
    @Test
    void relocatedModelIsResolvedAgainstTheReadingEnvironment() throws IOException {
        Path writtenAt = tempDir.resolve("slot-a");
        Path readAt = tempDir.resolve("slot-b");
        Path file = tempDir.resolve("model.dat");
        ApplicationModelSerializer.serialize(modelAt(writtenAt), file, roots(writtenAt));

        ApplicationModel deserialized = ApplicationModelSerializer.deserialize(file, roots(readAt));

        assertThat(deserialized.getAppArtifact().getResolvedPaths().getSinglePath())
                .isEqualTo(readAt.resolve("target/classes"));
    }

    /**
     * A model written before relocation existed, or with it disabled, carries no roots and has to be read
     * back exactly as it is.
     */
    @Test
    void modelWithoutRelocationIsReadUnchanged() throws IOException {
        Path checkout = tempDir.resolve("slot-a");
        Path file = tempDir.resolve("model-absolute.dat");
        ApplicationModelSerializer.serialize(modelAt(checkout), file, List.of());

        assertThat(Files.readString(file))
                .contains(checkout.toString())
                .doesNotContain(ApplicationModelRelocation.RELOCATION_ROOTS);

        ApplicationModel deserialized = ApplicationModelSerializer.deserialize(file, roots(checkout));
        assertThat(deserialized.getAppArtifact().getResolvedPaths().getSinglePath())
                .isEqualTo(checkout.resolve("target/classes"));
    }

    /**
     * Only the names of the roots may be recorded: writing their locations would put the checkout
     * directory straight back into the file.
     */
    @Test
    void recordedRootsCarryNamesOnly() throws IOException {
        Path checkout = tempDir.resolve("slot-a");
        Path file = tempDir.resolve("model.dat");
        ApplicationModelSerializer.serialize(modelAt(checkout), file, roots(checkout));

        assertThat(Files.readString(file))
                .contains(ApplicationModelRelocation.RELOCATION_ROOTS)
                .doesNotContain(checkout.toString());
    }

    private String serializeAt(Path checkout) throws IOException {
        Path file = tempDir.resolve(checkout.getFileName() + ".dat");
        ApplicationModelSerializer.serialize(modelAt(checkout), file, roots(checkout));
        return Files.readString(file);
    }

    private static List<ApplicationModelRelocation.Root> roots(Path projectDir) {
        return List.of(new ApplicationModelRelocation.Root("quarkus.project.dir", projectDir));
    }

    private static ApplicationModel modelAt(Path projectDir) {
        return new ApplicationModelBuilder()
                .setAppArtifact(ResolvedDependencyBuilder.newInstance()
                        .setGroupId("com.example")
                        .setArtifactId("my-app")
                        .setVersion("1.0.0")
                        .setResolvedPath(projectDir.resolve("target/classes")))
                .setPlatformImports(PlatformImports.fromMap(Collections.emptyMap()))
                .build();
    }
}
