
package io.quarkus.container.image.openshift.deployment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.openshift.api.model.BuildConfigSpecFluent;
import io.quarkus.deployment.util.FileUtil;

public class ApplyDockerfileToBuildConfigDecorator extends NamedResourceDecorator<BuildConfigSpecFluent<?>> {

    private final Path pathToDockerfile;

    public ApplyDockerfileToBuildConfigDecorator(String name, Path pathToDockerfile) {
        super(name);
        validate(pathToDockerfile);
        this.pathToDockerfile = pathToDockerfile;
    }

    private void validate(Path pathToDockerfile) {
        File file = pathToDockerfile.toFile();
        if (!file.exists()) {
            throw new IllegalArgumentException(
                    "Specified Dockerfile: '" + pathToDockerfile.toAbsolutePath().toString() + "' does not exist.");
        }
        if (!file.isFile()) {
            throw new IllegalArgumentException(
                    "Specified Dockerfile: '" + pathToDockerfile.toAbsolutePath().toString() + "' is not a normal file.");
        }

        try {
            Stream<String> lines = Files.lines(pathToDockerfile);
            Optional<String> fromLine = lines.filter(l -> !l.startsWith("#")).map(String::trim)
                    .filter(l -> l.startsWith("FROM")).findFirst();
            if (!fromLine.isPresent()) {
                throw new IllegalArgumentException("Specified Dockerfile: '" + pathToDockerfile.toAbsolutePath().toString()
                        + "' does not contain a FROM directive");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Unable to validate specified Dockerfile: '" + pathToDockerfile.toAbsolutePath().toString() + "'");
        }
    }

    @Override
    public void andThenVisit(final BuildConfigSpecFluent<?> spec, ObjectMeta meta) {
        try (InputStream is = new FileInputStream(pathToDockerfile.toFile())) {
            spec.withNewSource()
                    .withDockerfile(new String(FileUtil.readFileContents(is)))
                    .endSource()
                    .withNewStrategy()
                    .withNewDockerStrategy()
                    .endDockerStrategy()
                    .endStrategy();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
