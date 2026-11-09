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
import org.jboss.jandex.IndexView;

/**
 * This adds support for the quarkus.http.root-path config option
 */
public class CustomPathExtension {

    static final Set<DotName> APPLICATION_PATH = new TreeSet<>(Arrays.asList(
            DotName.createSimple("jakarta.ws.rs.ApplicationPath"),
            DotName.createSimple("javax.ws.rs.ApplicationPath")));

    static final Set<DotName> APPLICATION = new TreeSet<>(Arrays.asList(
            DotName.createSimple("jakarta.ws.rs.core.Application"),
            DotName.createSimple("javax.ws.rs.core.Application")));

    /**
     * Spring Web controllers are served by the REST layer (RESTEasy Classic or Reactive), so the REST path applies to them.
     */
    static final Set<DotName> SPRING_CONTROLLER = new TreeSet<>(Arrays.asList(
            DotName.createSimple("org.springframework.web.bind.annotation.RestController"),
            DotName.createSimple("org.springframework.web.bind.annotation.RequestMapping")));

    private final String rootPath;
    private final String appPath;
    private final IndexView index;

    public CustomPathExtension(String rootPath, String appPath, IndexView index) {
        this.rootPath = rootPath;
        this.appPath = appPath;
        this.index = index;
    }

    public String resolveContextRoot(Collection<ClassInfo> applications) {
        /*
         * SmallRye calls this for every scanner, passing the classes that scanner handles: JAX-RS passes the
         * Application subclasses (often none), Spring its controllers and Vert.x its route classes. The REST
         * path only applies to endpoints served by the REST layer, so anything else (e.g. Vert.x routes) only
         * gets the HTTP root path.
         */
        if (!applications.isEmpty() && applications.stream().noneMatch(this::isServedByRest)) {
            String contextRoot = buildContextRoot(rootPath);
            return "/".equals(contextRoot) ? null : contextRoot;
        }

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

    private boolean isServedByRest(ClassInfo classInfo) {
        return isRestApplication(classInfo)
                || SPRING_CONTROLLER.stream().anyMatch(classInfo::hasDeclaredAnnotation);
    }

    private boolean isRestApplication(ClassInfo classInfo) {
        ClassInfo current = classInfo;

        while (current != null) {
            if (APPLICATION_PATH.stream().anyMatch(current::hasDeclaredAnnotation)) {
                return true;
            }

            DotName superName = current.superName();

            if (superName == null) {
                return false;
            }
            if (APPLICATION.contains(superName)) {
                return true;
            }

            current = index.getClassByName(superName);
        }

        return false;
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