package io.quarkus.resteasy.reactive.server.deployment;

import java.util.function.Predicate;

import javax.ws.rs.core.Context;

import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.jboss.resteasy.reactive.server.SimpleResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext;

import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.vertx.ext.web.RoutingContext;

public class ResteasyReactiveServerDotNames {
    public static final DotName CONTEXT = DotName.createSimple(Context.class.getName());
    public static final DotName SERVER_REQUEST_FILTER = DotName
            .createSimple(ServerRequestFilter.class.getName());
    public static final DotName SERVER_RESPONSE_FILTER = DotName
            .createSimple(ServerResponseFilter.class.getName());
    public static final DotName QUARKUS_REST_CONTAINER_REQUEST_CONTEXT = DotName
            .createSimple(ResteasyReactiveContainerRequestContext.class.getName());
    public static final DotName SIMPLIFIED_RESOURCE_INFO = DotName.createSimple(SimpleResourceInfo.class.getName());
    public static final DotName ROUTING_CONTEXT = DotName.createSimple(RoutingContext.class.getName());

    public static final DotName JSON_IGNORE = DotName.createSimple("com.fasterxml.jackson.annotation.JsonIgnore");
    public static final DotName JSONB_TRANSIENT = DotName.createSimple("javax.json.bind.annotation.JsonbTransient");

    public static final IgnoreTypeForReflectionPredicate IGNORE_TYPE_FOR_REFLECTION_PREDICATE = new IgnoreTypeForReflectionPredicate();
    public static final IgnoreFieldForReflectionPredicate IGNORE_FIELD_FOR_REFLECTION_PREDICATE = new IgnoreFieldForReflectionPredicate();
    public static final IgnoreMethodForReflectionPredicate IGNORE_METHOD_FOR_REFLECTION_PREDICATE = new IgnoreMethodForReflectionPredicate();

    private static class IgnoreTypeForReflectionPredicate implements Predicate<DotName> {

        @Override
        public boolean test(DotName dotName) {
            if (ReflectiveHierarchyBuildItem.DefaultIgnoreTypePredicate.INSTANCE.test(dotName)) {
                return true;
            }
            String name = dotName.toString();
            for (String packageName : PACKAGES_IGNORED_FOR_REFLECTION) {
                if (name.startsWith(packageName)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static class IgnoreFieldForReflectionPredicate implements Predicate<FieldInfo> {

        @Override
        public boolean test(FieldInfo fieldInfo) {
            return fieldInfo.hasAnnotation(JSON_IGNORE)
                    || fieldInfo.hasAnnotation(JSONB_TRANSIENT);
        }
    }

    private static class IgnoreMethodForReflectionPredicate implements Predicate<MethodInfo> {

        @Override
        public boolean test(MethodInfo methodInfo) {
            return methodInfo.hasAnnotation(JSON_IGNORE)
                    || methodInfo.hasAnnotation(JSONB_TRANSIENT);
        }
    }

    private static final String[] PACKAGES_IGNORED_FOR_REFLECTION = {
            // JSON-P
            "javax.json.",
            // Jackson
            "com.fasterxml.jackson.databind.",
            // JAX-RS
            "javax.ws.rs.",
            // RESTEasy
            "org.jboss.resteasy.reactive",
            // Vert.x JSON layer
            "io.vertx.core.json.",
            // Mutiny
            "io.smallrye.mutiny."
    };
}
