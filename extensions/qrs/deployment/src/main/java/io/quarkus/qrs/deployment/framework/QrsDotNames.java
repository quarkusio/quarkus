package io.quarkus.qrs.deployment.framework;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.jandex.DotName;

import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.qrs.Blocking;

public final class QrsDotNames {

    public static final DotName CONSUMES = DotName.createSimple(Consumes.class.getName());
    public static final DotName PRODUCES = DotName.createSimple(Produces.class.getName());
    public static final DotName PROVIDER = DotName.createSimple(Provider.class.getName());
    public static final DotName PATH_PARAM = DotName.createSimple(PathParam.class.getName());
    public static final DotName QUERY_PARAM = DotName.createSimple(QueryParam.class.getName());
    public static final DotName HEADER_PARAM = DotName.createSimple(HeaderParam.class.getName());
    public static final DotName FORM_PARAM = DotName.createSimple(FormParam.class.getName());
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
    public static final DotName BLOCKING = DotName.createSimple(Blocking.class.getName());
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
            return QrsDotNames.TYPES_IGNORED_FOR_REFLECTION.contains(name)
                    || ReflectiveHierarchyBuildItem.DefaultIgnorePredicate.INSTANCE.test(name);
        }
    }

    // Types ignored for reflection used by the RESTEasy and SmallRye REST client extensions.
    private static final Set<DotName> TYPES_IGNORED_FOR_REFLECTION = new HashSet<>(Arrays.asList(
            // javax.json
            DotName.createSimple("javax.json.JsonObject"),
            DotName.createSimple("javax.json.JsonArray"),
            DotName.createSimple("javax.json.JsonValue"),

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
}
