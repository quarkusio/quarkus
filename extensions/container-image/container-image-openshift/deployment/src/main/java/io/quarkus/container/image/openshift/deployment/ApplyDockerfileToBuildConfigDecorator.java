
package io.quarkus.container.image.openshift.deployment;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.openshift.api.model.BuildConfigSpecFluent;
import io.quarkus.deployment.util.FileUtil;

public class ApplyDockerfileToBuildConfigDecorator extends NamedResourceDecorator<BuildConfigSpecFluent<?>> {

    private final Path pathToDockerfile;

    public ApplyDockerfileToBuildConfigDecorator(String name, Path pathToDockerfile) {
        super(name);
        this.pathToDockerfile = pathToDockerfile;
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
