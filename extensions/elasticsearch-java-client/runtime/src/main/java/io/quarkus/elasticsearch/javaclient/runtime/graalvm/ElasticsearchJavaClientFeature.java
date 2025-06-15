package io.quarkus.elasticsearch.javaclient.runtime.graalvm;

import java.lang.reflect.Executable;
import java.lang.reflect.Modifier;

import jakarta.json.stream.JsonParser;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import co.elastic.clients.json.JsonpDeserializable;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.util.WithJsonObjectBuilderBase;

/**
 * Custom GraalVM feature to make Elasticsearch Java Client work in native mode.
 * <p>
 * In particular, when applications rely on `WithJsonObjectBuilderBase#withJson(...)`, this automatically registers the
 * corresponding Jsonp deserializers as accessed through reflection. We can't just register them all indiscriminately,
 * because this would result in literally thousands of registrations, most of which would probably be useless.
 */
public final class ElasticsearchJavaClientFeature implements Feature {

    private static final String BUILDER_BASE_CLASS_NAME = "co.elastic.clients.util.WithJsonObjectBuilderBase";

    /**
     * To set this, add `-J-Dio.quarkus.elasticsearch.javaclient.graalvm.diagnostics=true` to the native-image
     * parameters, e.g. pass this to Maven:
     * -Dquarkus.native.additional-build-args=-J-Dio.quarkus.elasticsearch.javaclient.graalvm.diagnostics=true
     */
    private static final boolean log = Boolean.getBoolean("io.quarkus.elasticsearch.javaclient.graalvm.diagnostics");

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        Class<?> builderClass = access.findClassByName(BUILDER_BASE_CLASS_NAME);
        Executable withJsonMethod;
        try {
            withJsonMethod = builderClass.getMethod("withJson", JsonParser.class, JsonpMapper.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Could not find " + BUILDER_BASE_CLASS_NAME + "#withJson(...);"
                    + " does the version of Elasticsearch Java Client match the version specified in the Quarkus BOM?");
        }
        access.registerReachabilityHandler(this::onWithJsonReachable, withJsonMethod);
    }

    private void onWithJsonReachable(DuringAnalysisAccess access) {
        // The builder base class' withJson(...) method is reachable.
        logf("%s#withJson(...) is reachable", BUILDER_BASE_CLASS_NAME);

        // We don't know on which builder subclass the withJson(...) method is called,
        // so to be safe we consider every reachable builder subclass.
        for (Class<?> builderSubClass : access.reachableSubtypes(WithJsonObjectBuilderBase.class)) {
            enableBuilderWithJson(builderSubClass, access);
        }
    }

    private void enableBuilderWithJson(Class<?> builderSubClass, DuringAnalysisAccess access) {
        // We don't care about abstract builder classes
        if (Modifier.isAbstract(builderSubClass.getModifiers())) {
            // Abstract builder classes may be top-level classes.
            return;
        }

        // When a builder's withJson() method is called,
        // the implementation will (indirectly) access the enclosing class'
        // _DESERIALIZER constant field through reflection,
        // so we need to let GraalVM know.

        // Best-guess of the built class, given the coding coventions in Elasticsearch Java Client;
        // ideally we'd resolve generics but it's hard and we don't have the right utils in our dependencies.
        var builtClass = builderSubClass.getEnclosingClass();
        if (builtClass == null) {
            logf("Could not guess the class built by %s", builderSubClass);
            // Just ignore and hope this class doesn't matter.
            return;
        }

        var deserializable = builtClass.getAnnotation(JsonpDeserializable.class);
        if (deserializable == null) {
            logf("Could not find @JsonpDeserializable on %s for builder %s", builtClass, builderSubClass);
            // Just ignore and hope this class doesn't matter.
            return;
        }

        // Technically the name of the constant field may be customized,
        // though in practice it's always the default name (_DESERIALIZER).
        String fieldName = deserializable.field();
        try {
            var field = builtClass.getDeclaredField(fieldName);
            logf("Registering deserializer field %s as accessed in %s", fieldName, builtClass);
            access.registerAsAccessed(field);
            RuntimeReflection.register(field);
        } catch (NoSuchFieldException e) {
            logf("Could not find deserializer field %s in %s", fieldName, builtClass);
        }
    }

    private void logf(String message, Object... args) {
        if (!log) {
            return;
        }
        System.out.printf("Quarkus's automatic feature for Elasticsearch Java Client: " + message + "\n", args);
    }

    @Override
    public String getDescription() {
        return "Support for Elasticsearch Java Client's withJson() methods in builders";
    }

}
