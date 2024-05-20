package io.quarkus.container.image.docker.common.deployment;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface DockerFileBaseInformationProvider {

    Optional<DockerFileBaseInformation> determine(Path dockerFile);

    static DockerFileBaseInformationProvider impl() {
        return new DockerFileBaseInformationProvider() {

            private final List<DockerFileBaseInformationProvider> delegates = List.of(new UbiMinimalBaseProvider(),
                    new RedHatOpenJDKRuntimeBaseProvider());

            @Override
            public Optional<DockerFileBaseInformation> determine(Path dockerFile) {
                for (var delegate : delegates) {
                    var result = delegate.determine(dockerFile);

                    if (result.isPresent()) {
                        return result;
                    }
                }

                return Optional.empty();
            }
        };
    }

    record DockerFileBaseInformation(String baseImage, int javaVersion) {
    }
}
