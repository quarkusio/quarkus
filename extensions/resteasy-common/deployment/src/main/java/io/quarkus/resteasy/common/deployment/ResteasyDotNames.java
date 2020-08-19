package io.quarkus.resteasy.common.deployment;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;

import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;

public final class ResteasyDotNames {

    public static final DotName CONSUMES = DotName.createSimple(Consumes.class.getName());
    public static final DotName PRODUCES = DotName.createSimple(Produces.class.getName());
    public static final DotName PROVIDER = DotName.createSimple(Provider.class.getName());
    public static final DotName GET = DotName.createSimple(javax.ws.rs.GET.class.getName());
    public static final DotName HEAD = DotName.createSimple(javax.ws.rs.HEAD.class.getName());
    public static final DotName DELETE = DotName.createSimple(javax.ws.rs.DELETE.class.getName());
    public static final DotName OPTIONS = DotName.createSimple(javax.ws.rs.OPTIONS.class.getName());
    public static final DotName PATCH = DotName.createSimple(javax.ws.rs.PATCH.class.getName());
    public static final DotName POST = DotName.createSimple(javax.ws.rs.POST.class.getName());
    public static final DotName PUT = DotName.createSimple(javax.ws.rs.PUT.class.getName());
    public static final DotName APPLICATION_PATH = DotName.createSimple(ApplicationPath.class.getName());
    public static final DotName PATH = DotName.createSimple(Path.class.getName());
    public static final DotName DYNAMIC_FEATURE = DotName.createSimple(DynamicFeature.class.getName());
    public static final DotName CONTEXT = DotName.createSimple(Context.class.getName());
    public static final DotName RESTEASY_QUERY_PARAM = DotName
            .createSimple(org.jboss.resteasy.annotations.jaxrs.QueryParam.class.getName());
    public static final DotName RESTEASY_FORM_PARAM = DotName
            .createSimple(org.jboss.resteasy.annotations.jaxrs.FormParam.class.getName());
    public static final DotName RESTEASY_COOKIE_PARAM = DotName
            .createSimple(org.jboss.resteasy.annotations.jaxrs.CookieParam.class.getName());
    public static final DotName RESTEASY_PATH_PARAM = DotName
            .createSimple(org.jboss.resteasy.annotations.jaxrs.PathParam.class.getName());
    public static final DotName RESTEASY_HEADER_PARAM = DotName
            .createSimple(org.jboss.resteasy.annotations.jaxrs.HeaderParam.class.getName());
    public static final DotName RESTEASY_MATRIX_PARAM = DotName
            .createSimple(org.jboss.resteasy.annotations.jaxrs.MatrixParam.class.getName());
    public static final DotName RESTEASY_SSE_ELEMENT_TYPE = DotName
            .createSimple(org.jboss.resteasy.annotations.SseElementType.class.getName());
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
            "io.vertx.core.json."
    };
}
