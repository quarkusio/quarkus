
package io.quarkus.container.image.s2i.deployment;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import io.dekorate.deps.kubernetes.api.model.ObjectMeta;
import io.dekorate.deps.openshift.api.model.BuildConfigSpecFluent;
import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
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
