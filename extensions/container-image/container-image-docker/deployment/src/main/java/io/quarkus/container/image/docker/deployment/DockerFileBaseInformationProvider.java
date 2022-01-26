package io.quarkus.container.image.docker.deployment;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

interface DockerFileBaseInformationProvider {

    Optional<DockerFileBaseInformation> determine(Path dockerFile);

    static DockerFileBaseInformationProvider impl() {
        return new DockerFileBaseInformationProvider() {

            private final List<DockerFileBaseInformationProvider> delegates = List.of(new UbiMinimalBaseProvider(),
                    new RedHatOpenJDKRuntimeBaseProvider());

            @Override
            public Optional<DockerFileBaseInformation> determine(Path dockerFile) {
                for (DockerFileBaseInformationProvider delegate : delegates) {
                    Optional<DockerFileBaseInformation> result = delegate.determine(dockerFile);
                    if (result.isPresent()) {
                        return result;
                    }
                }
                return Optional.empty();
            }
        };
    }

    class DockerFileBaseInformation {
        private final int javaVersion;
        private final String baseImage;

        public DockerFileBaseInformation(String baseImage, int javaVersion) {
            this.javaVersion = javaVersion;
            this.baseImage = baseImage;
        }

        public int getJavaVersion() {
            return javaVersion;
        }

        public String getBaseImage() {
            return baseImage;
        }
    }
}
