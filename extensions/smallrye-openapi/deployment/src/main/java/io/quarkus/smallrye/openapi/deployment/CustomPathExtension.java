package io.quarkus.smallrye.openapi.deployment;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

/**
 * This adds support for the quarkus.http.root-path config option
 */
public class CustomPathExtension {

    static final Set<DotName> APPLICATION_PATH = new TreeSet<>(Arrays.asList(
            DotName.createSimple("jakarta.ws.rs.ApplicationPath"),
            DotName.createSimple("javax.ws.rs.ApplicationPath")));

    private final String rootPath;
    private final String appPath;

    public CustomPathExtension(String rootPath, String appPath) {
        this.rootPath = rootPath;
        this.appPath = appPath;
    }

    public String resolveContextRoot(Collection<ClassInfo> applications) {
        Optional<String> appPathAnnotationValue = applications.stream()
                .flatMap(app -> APPLICATION_PATH.stream().map(app::declaredAnnotation))
                .filter(Objects::nonNull)
                .map(AnnotationInstance::value)
                .map(AnnotationValue::asString)
                .findFirst();

        /*
         * If the @ApplicationPath was found, ignore the appPath given in configuration and only
         * use the rootPath for the contextRoot.
         */
        String contextRoot = appPathAnnotationValue.map(path -> buildContextRoot(rootPath))
                .orElseGet(() -> buildContextRoot(rootPath, this.appPath));

        return "/".equals(contextRoot) ? null : contextRoot;
    }

    static String buildContextRoot(String... segments) {
        String path = Stream.of(segments)
                .filter(Objects::nonNull)
                .map(CustomPathExtension::stripSlashes)
                .filter(Predicate.not(String::isEmpty))
                .map("/"::concat)
                .collect(Collectors.joining());

        return path.isEmpty() ? "/" : path;
    }

    static String stripSlashes(String segment) {
        if (segment.startsWith("/")) {
            segment = segment.substring(1);
        }

        if (segment.endsWith("/")) {
            segment = segment.substring(0, segment.length() - 1);
        }

        return segment;
    }
}