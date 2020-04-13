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

    public static final List<DotName> JAXRS_METHOD_ANNOTATIONS = Collections
            .unmodifiableList(Arrays.asList(GET, POST, HEAD, DELETE, PUT, PATCH, OPTIONS));

    public static final IgnoreForReflectionPredicate IGNORE_FOR_REFLECTION_PREDICATE = new IgnoreForReflectionPredicate();

    private static class IgnoreForReflectionPredicate implements Predicate<DotName> {

        @Override
        public boolean test(DotName name) {
            return ResteasyDotNames.TYPES_IGNORED_FOR_REFLECTION.contains(name) ||
                    isInIgnoredPackage(name.toString());
        }

        private boolean isInIgnoredPackage(String name) {
            for (String containerPackageName : PACKAGES_IGNORED_FOR_REFLECTION) {
                if (name.startsWith(containerPackageName)) {
                    return true;
                }
            }
            return false;
        }
    }

    // Types ignored for reflection used by the RESTEasy and SmallRye REST client extensions.
    private static final Set<DotName> TYPES_IGNORED_FOR_REFLECTION = new HashSet<>(Arrays.asList(
            // javax.json
            DotName.createSimple("javax.json.JsonObject"),
            DotName.createSimple("javax.json.JsonArray"),

            // Jackson
            DotName.createSimple("com.fasterxml.jackson.databind.JsonNode"),

            // JAX-RS
            DotName.createSimple("javax.ws.rs.core.Response"),
            DotName.createSimple("javax.ws.rs.container.AsyncResponse"),
            DotName.createSimple("javax.ws.rs.core.StreamingOutput"),
            DotName.createSimple("javax.ws.rs.core.Form"),
            DotName.createSimple("javax.ws.rs.core.MultivaluedMap"),

            // RESTEasy
            DotName.createSimple("org.jboss.resteasy.plugins.providers.multipart.MultipartInput"),
            DotName.createSimple("org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput"),
            DotName.createSimple("org.jboss.resteasy.plugins.providers.multipart.MultipartOutput"),
            DotName.createSimple("org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput"),

            // Vert-x
            DotName.createSimple("io.vertx.core.json.JsonArray"),
            DotName.createSimple("io.vertx.core.json.JsonObject")));

    private static final List<String> PACKAGES_IGNORED_FOR_REFLECTION = Arrays.asList("java.", "io.reactivex.",
            "org.reactivestreams.");
}
