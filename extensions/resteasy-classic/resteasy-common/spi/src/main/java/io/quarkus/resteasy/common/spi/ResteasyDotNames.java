package io.quarkus.resteasy.common.spi;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;

import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;

public final class ResteasyDotNames {

    public static final DotName APPLICATION = DotName.createSimple("javax.ws.rs.core.Application");
    public static final DotName CONSUMES = DotName.createSimple("javax.ws.rs.Consumes");
    public static final DotName PRODUCES = DotName.createSimple("javax.ws.rs.Produces");
    public static final DotName PROVIDER = DotName.createSimple("javax.ws.rs.ext.Provider");
    public static final DotName GET = DotName.createSimple("javax.ws.rs.GET");
    public static final DotName HEAD = DotName.createSimple("javax.ws.rs.HEAD");
    public static final DotName DELETE = DotName.createSimple("javax.ws.rs.DELETE");
    public static final DotName OPTIONS = DotName.createSimple("javax.ws.rs.OPTIONS");
    public static final DotName PATCH = DotName.createSimple("javax.ws.rs.PATCH");
    public static final DotName POST = DotName.createSimple("javax.ws.rs.POST");
    public static final DotName PUT = DotName.createSimple("javax.ws.rs.PUT");
    public static final DotName APPLICATION_PATH = DotName.createSimple("javax.ws.rs.ApplicationPath");
    public static final DotName PATH = DotName.createSimple("javax.ws.rs.Path");
    public static final DotName DYNAMIC_FEATURE = DotName.createSimple("javax.ws.rs.container.DynamicFeature");
    public static final DotName CONTEXT = DotName.createSimple("javax.ws.rs.core.Context");
    public static final DotName RESTEASY_QUERY_PARAM = DotName
            .createSimple("org.jboss.resteasy.annotations.jaxrs.QueryParam");
    public static final DotName RESTEASY_FORM_PARAM = DotName
            .createSimple("org.jboss.resteasy.annotations.jaxrs.FormParam");
    public static final DotName RESTEASY_COOKIE_PARAM = DotName
            .createSimple("org.jboss.resteasy.annotations.jaxrs.CookieParam");
    public static final DotName RESTEASY_PATH_PARAM = DotName
            .createSimple("org.jboss.resteasy.annotations.jaxrs.PathParam");
    public static final DotName RESTEASY_HEADER_PARAM = DotName
            .createSimple("org.jboss.resteasy.annotations.jaxrs.HeaderParam");
    public static final DotName RESTEASY_MATRIX_PARAM = DotName
            .createSimple("org.jboss.resteasy.annotations.jaxrs.MatrixParam");
    public static final DotName RESTEASY_SSE_ELEMENT_TYPE = DotName
            .createSimple("org.jboss.resteasy.annotations.SseElementType");
    public static final DotName RESTEASY_PART_TYPE = DotName
            .createSimple("org.jboss.resteasy.annotations.providers.multipart.PartType");
    public static final DotName CONFIG_PROPERTY = DotName
            .createSimple(ConfigProperty.class.getName());
    public static final DotName CDI_INSTANCE = DotName
            .createSimple(javax.enterprise.inject.Instance.class.getName());
    public static final DotName JSON_IGNORE = DotName.createSimple("com.fasterxml.jackson.annotation.JsonIgnore");
    public static final DotName JSONB_TRANSIENT = DotName.createSimple("javax.json.bind.annotation.JsonbTransient");
    public static final DotName XML_TRANSIENT = DotName.createSimple("javax.xml.bind.annotation.XmlTransient");

    public static final List<DotName> JAXRS_METHOD_ANNOTATIONS = Collections
            .unmodifiableList(Arrays.asList(GET, POST, HEAD, DELETE, PUT, PATCH, OPTIONS));

    public static final IgnoreTypeForReflectionPredicate IGNORE_TYPE_FOR_REFLECTION_PREDICATE = new IgnoreTypeForReflectionPredicate();
    public static final IgnoreFieldForReflectionPredicate IGNORE_FIELD_FOR_REFLECTION_PREDICATE = new IgnoreFieldForReflectionPredicate();
    public static final IgnoreMethodForReflectionPredicate IGNORE_METHOD_FOR_REFLECTION_PREDICATE = new IgnoreMethodForReflectionPredicate();

    private static class IgnoreTypeForReflectionPredicate implements Predicate<DotName> {

        @Override
        public boolean test(DotName dotName) {
            if (ResteasyDotNames.TYPES_IGNORED_FOR_REFLECTION.contains(dotName)
                    || ReflectiveHierarchyBuildItem.DefaultIgnoreTypePredicate.INSTANCE.test(dotName)) {
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
                    || fieldInfo.hasAnnotation(JSONB_TRANSIENT)
                    || fieldInfo.hasAnnotation(XML_TRANSIENT);
        }
    }

    private static class IgnoreMethodForReflectionPredicate implements Predicate<MethodInfo> {

        @Override
        public boolean test(MethodInfo methodInfo) {
            return methodInfo.hasAnnotation(JSON_IGNORE)
                    || methodInfo.hasAnnotation(JSONB_TRANSIENT)
                    || methodInfo.hasAnnotation(XML_TRANSIENT);
        }
    }

    // Types ignored for reflection used by the RESTEasy and SmallRye REST client extensions.
    private static final Set<DotName> TYPES_IGNORED_FOR_REFLECTION = new HashSet<>(Arrays.asList(
    // Consider adding packages below instead if it makes more sense
    ));

    private static final String[] PACKAGES_IGNORED_FOR_REFLECTION = {
            // JSON-P
            "javax.json.",
            // Jackson
            "com.fasterxml.jackson.databind.",
            // JAX-RS
            "javax.ws.rs.",
            // RESTEasy
            "org.jboss.resteasy.",
            // Vert.x JSON layer
            "io.vertx.core.json.",
            // Mutiny
            "io.smallrye.mutiny."
    };
}
