package io.quarkus.rest.deployment.framework;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Predicate;

import javax.annotation.Priority;
import javax.enterprise.inject.Vetoed;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.BeanParam;
import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.NameBinding;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.jandex.DotName;

import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.rest.Blocking;

public final class QuarkusRestDotNames {

    public static final DotName CONSUMES = DotName.createSimple(Consumes.class.getName());
    public static final DotName PRODUCES = DotName.createSimple(Produces.class.getName());
    public static final DotName PROVIDER = DotName.createSimple(Provider.class.getName());
    public static final DotName BEAN_PARAM = DotName.createSimple(BeanParam.class.getName());
    public static final DotName PATH_PARAM = DotName.createSimple(PathParam.class.getName());
    public static final DotName QUERY_PARAM = DotName.createSimple(QueryParam.class.getName());
    public static final DotName HEADER_PARAM = DotName.createSimple(HeaderParam.class.getName());
    public static final DotName FORM_PARAM = DotName.createSimple(FormParam.class.getName());
    public static final DotName MATRIX_PARAM = DotName.createSimple(MatrixParam.class.getName());
    public static final DotName COOKIE_PARAM = DotName.createSimple(CookieParam.class.getName());
    public static final DotName GET = DotName.createSimple(javax.ws.rs.GET.class.getName());
    public static final DotName HEAD = DotName.createSimple(javax.ws.rs.HEAD.class.getName());
    public static final DotName DELETE = DotName.createSimple(javax.ws.rs.DELETE.class.getName());
    public static final DotName OPTIONS = DotName.createSimple(javax.ws.rs.OPTIONS.class.getName());
    public static final DotName PATCH = DotName.createSimple(javax.ws.rs.PATCH.class.getName());
    public static final DotName POST = DotName.createSimple(javax.ws.rs.POST.class.getName());
    public static final DotName PUT = DotName.createSimple(javax.ws.rs.PUT.class.getName());
    public static final DotName HTTP_METHOD = DotName.createSimple(javax.ws.rs.HttpMethod.class.getName());
    public static final DotName APPLICATION_PATH = DotName.createSimple(ApplicationPath.class.getName());
    public static final DotName PATH = DotName.createSimple(Path.class.getName());
    public static final DotName FEATURE = DotName.createSimple(Feature.class.getName());
    public static final DotName DYNAMIC_FEATURE = DotName.createSimple(DynamicFeature.class.getName());
    public static final DotName CONTEXT = DotName.createSimple(Context.class.getName());
    public static final DotName CONFIG_PROPERTY = DotName
            .createSimple(ConfigProperty.class.getName());
    public static final DotName CDI_INSTANCE = DotName
            .createSimple(javax.enterprise.inject.Instance.class.getName());
    public static final DotName PRIORITY = DotName.createSimple(Priority.class.getName());
    public static final DotName CONTAINER_REQUEST_FILTER = DotName.createSimple(ContainerRequestFilter.class.getName());
    public static final DotName CONTAINER_RESPONSE_FILTER = DotName.createSimple(ContainerResponseFilter.class.getName());
    public static final DotName EXCEPTION_MAPPER = DotName.createSimple(ExceptionMapper.class.getName());
    public static final DotName CONTEXT_RESOLVER = DotName.createSimple(ContextResolver.class.getName());
    public static final DotName MESSAGE_BODY_WRITER = DotName.createSimple(MessageBodyWriter.class.getName());
    public static final DotName WRITER_INTERCEPTOR = DotName.createSimple(WriterInterceptor.class.getName());
    public static final DotName READER_INTERCEPTOR = DotName.createSimple(ReaderInterceptor.class.getName());
    public static final DotName MESSAGE_BODY_READER = DotName.createSimple(MessageBodyReader.class.getName());
    public static final DotName CONSTRAINED_TO = DotName.createSimple(ConstrainedTo.class.getName());
    public static final DotName DEFAULT_VALUE = DotName.createSimple(DefaultValue.class.getName());
    public static final DotName NAME_BINDING = DotName.createSimple(NameBinding.class.getName());
    public static final DotName VETOED = DotName.createSimple(Vetoed.class.getName());

    public static final DotName INVOCATION_CALLBACK = DotName.createSimple(InvocationCallback.class.getName());

    public static final DotName BLOCKING = DotName.createSimple(Blocking.class.getName());
    public static final DotName SUSPENDED = DotName.createSimple(Suspended.class.getName());
    public static final DotName PRE_MATCHING = DotName.createSimple(PreMatching.class.getName());

    public static final DotName LIST = DotName.createSimple(List.class.getName());
    public static final DotName SET = DotName.createSimple(Set.class.getName());
    public static final DotName SORTED_SET = DotName.createSimple(SortedSet.class.getName());
    public static final DotName MULTI_VALUED_MAP = DotName.createSimple(MultivaluedMap.class.getName());

    public static final DotName INTEGER = DotName.createSimple(Integer.class.getName());
    public static final DotName LONG = DotName.createSimple(Long.class.getName());
    public static final DotName FLOAT = DotName.createSimple(Float.class.getName());
    public static final DotName DOUBLE = DotName.createSimple(Double.class.getName());
    public static final DotName BOOLEAN = DotName.createSimple(Boolean.class.getName());
    public static final DotName CHARACTER = DotName.createSimple(Character.class.getName());

    public static final DotName PRIMITIVE_INTEGER = DotName.createSimple(int.class.getName());
    public static final DotName PRIMITIVE_LONG = DotName.createSimple(long.class.getName());
    public static final DotName PRIMITIVE_FLOAT = DotName.createSimple(float.class.getName());
    public static final DotName PRIMITIVE_DOUBLE = DotName.createSimple(double.class.getName());
    public static final DotName PRIMITIVE_BOOLEAN = DotName.createSimple(boolean.class.getName());
    public static final DotName PRIMITIVE_CHAR = DotName.createSimple(char.class.getName());

    public static final DotName STRING = DotName.createSimple(String.class.getName());
    public static final DotName BYTE_ARRAY_DOT_NAME = DotName.createSimple(byte[].class.getName());
    public static final DotName INPUT_STREAM = DotName.createSimple(InputStream.class.getName());

    public static final DotName JSONP_JSON_OBJECT = DotName.createSimple(javax.json.JsonObject.class.getName());
    public static final DotName JSONP_JSON_ARRAY = DotName.createSimple(javax.json.JsonArray.class.getName());
    public static final DotName JSONP_JSON_STRUCTURE = DotName.createSimple(javax.json.JsonStructure.class.getName());

    public static final List<DotName> JAXRS_METHOD_ANNOTATIONS = Collections
            .unmodifiableList(Arrays.asList(GET, POST, HEAD, DELETE, PUT, PATCH, OPTIONS));

    // TODO: add Path, Cookie and Matrix param handling
    public static final Set<DotName> RESOURCE_CTOR_PARAMS_THAT_NEED_HANDLING = new HashSet<>(
            Arrays.asList(QUERY_PARAM, HEADER_PARAM));

    public static final IgnoreForReflectionPredicate IGNORE_FOR_REFLECTION_PREDICATE = new IgnoreForReflectionPredicate();

    private static class IgnoreForReflectionPredicate implements Predicate<DotName> {

        @Override
        public boolean test(DotName name) {
            return QuarkusRestDotNames.TYPES_IGNORED_FOR_REFLECTION.contains(name)
                    || ReflectiveHierarchyBuildItem.DefaultIgnoreTypePredicate.INSTANCE.test(name);
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
