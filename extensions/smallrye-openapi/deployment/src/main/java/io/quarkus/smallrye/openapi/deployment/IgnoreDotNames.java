package io.quarkus.smallrye.openapi.deployment;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.jboss.jandex.DotName;

import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;

/**
 * Copied from resteasy-common, but changed to not depend on JAX-RS. This is so
 * that when open-api is used without scanning (so no JAX-RS) we do not pull in
 * the jax-rs dep
 */
public final class IgnoreDotNames {

    public static final IgnoreForReflectionPredicate IGNORE_FOR_REFLECTION_PREDICATE = new IgnoreForReflectionPredicate();

    private static class IgnoreForReflectionPredicate implements Predicate<DotName> {

        @Override
        public boolean test(DotName name) {
            return IgnoreDotNames.TYPES_IGNORED_FOR_REFLECTION.contains(name)
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
