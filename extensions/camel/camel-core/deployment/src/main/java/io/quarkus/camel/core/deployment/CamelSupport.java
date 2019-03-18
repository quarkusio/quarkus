package io.quarkus.camel.core.deployment;

import java.io.IOError;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.jboss.jandex.ClassInfo;

import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;

public final class CamelSupport {

    public static final String CAMEL_SERVICE_BASE_PATH = "META-INF/services/org/apache/camel";

    public static final String CAMEL_ROOT_PACKAGE_DIRECTORY = "org/apache/camel";

    private CamelSupport() {
    }

    public static boolean isConcrete(ClassInfo ci) {
        return (ci.flags() & Modifier.ABSTRACT) == 0;
    }

    public static boolean isPublic(ClassInfo ci) {
        return (ci.flags() & Modifier.PUBLIC) != 0;
    }

    public static Stream<Path> safeWalk(Path p) {
        try {
            return Files.walk(p);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public static Stream<Path> resources(ApplicationArchivesBuildItem archives, String path) {
        return archives.getAllApplicationArchives().stream()
                .map(arch -> arch.getArchiveRoot().resolve(path))
                .filter(Files::isDirectory)
                .flatMap(CamelSupport::safeWalk)
                .filter(Files::isRegularFile);
    }
}
