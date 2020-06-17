package io.quarkus.devtools.codestarts;

import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.dependencies.Extension;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public final class QuarkusCodestarts {
    private QuarkusCodestarts() {
    }

    public static CodestartInputBuilder inputBuilder(QuarkusPlatformDescriptor platformDescr) {
        return CodestartInput.builder(resourceLoader(platformDescr), buildCodestartMapping(platformDescr.getExtensions()));
    }

    public static CodestartResourceLoader resourceLoader(QuarkusPlatformDescriptor platformDescr) {
        return new QuarkusPlatformCodestartResourceLoader(platformDescr);
    }

    public static Map<AppArtifactKey, String> buildCodestartMapping(Collection<Extension> extensions) {
        return extensions.stream()
                .filter(e -> e.getCodestart() != null)
                .collect(Collectors.toMap(e -> new AppArtifactKey(e.getGroupId(), e.getArtifactId(), e.getClassifier(),
                        e.getType() == null ? "jar" : e.getType()), Extension::getCodestart));
    }

    private static class QuarkusPlatformCodestartResourceLoader implements CodestartResourceLoader {
        private QuarkusPlatformDescriptor platformDescr;

        QuarkusPlatformCodestartResourceLoader(QuarkusPlatformDescriptor platformDescr) {
            this.platformDescr = platformDescr;
        }

        @Override
        public <T> T loadResourceAsPath(String name, Consumer<T> consumer) throws IOException {
            return platformDescr.loadResourceAsPath(name, consumer::consume);
        }
    };
}
