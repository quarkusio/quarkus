package org.jboss.resteasy.reactive.common.processor;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Vetoed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.Encoded;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.MatrixParam;
import jakarta.ws.rs.NameBinding;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.InvocationCallback;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.Providers;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.jboss.jandex.DotName;
import org.jboss.resteasy.reactive.DummyElementType;
import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.PartFilename;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestCookie;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestHeader;
import org.jboss.resteasy.reactive.RestMatrix;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.RestSseElementType;
import org.jboss.resteasy.reactive.RestStreamElementType;
import org.reactivestreams.Publisher;

public final class ResteasyReactiveDotNames {

    // injectable @Context types
    public static final DotName URI_INFO = DotName.createSimple(UriInfo.class.getName());
    public static final DotName HTTP_HEADERS = DotName.createSimple(HttpHeaders.class.getName());
    public static final DotName REQUEST = DotName.createSimple(Request.class.getName());
    public static final DotName RESPONSE = DotName.createSimple(Response.class.getName());
    public static final DotName SECURITY_CONTEXT = DotName.createSimple(SecurityContext.class.getName());
    public static final DotName PROVIDERS = DotName.createSimple(Providers.class.getName());
    public static final DotName RESOURCE_CONTEXT = DotName.createSimple(ResourceContext.class.getName());
    public static final DotName CONFIGURATION = DotName.createSimple(Configuration.class.getName());
    public static final DotName SSE = DotName.createSimple(Sse.class.getName());
    public static final DotName SSE_EVENT_SINK = DotName.createSimple(SseEventSink.class.getName());
    public static final DotName RESOURCE_INFO = DotName.createSimple(ResourceInfo.class.getName());
    public static final DotName SERVER_REQUEST_CONTEXT = DotName
            .createSimple("org.jboss.resteasy.reactive.server.spi.ServerRequestContext");

    public static final DotName REST_SSE_ELEMENT_TYPE = DotName.createSimple(RestSseElementType.class.getName());
    public static final DotName REST_STREAM_ELEMENT_TYPE = DotName.createSimple(RestStreamElementType.class.getName());
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
    public static final DotName REST_PATH_PARAM = DotName.createSimple(RestPath.class.getName());
    public static final DotName REST_QUERY_PARAM = DotName.createSimple(RestQuery.class.getName());
    public static final DotName REST_HEADER_PARAM = DotName.createSimple(RestHeader.class.getName());
    public static final DotName REST_FORM_PARAM = DotName.createSimple(RestForm.class.getName());
    public static final DotName MULTI_PART_FORM_PARAM = DotName.createSimple(MultipartForm.class.getName());
    public static final DotName PART_TYPE_NAME = DotName.createSimple(PartType.class.getName());
    public static final DotName PART_FILE_NAME = DotName.createSimple(PartFilename.class.getName());
    public static final DotName REST_MATRIX_PARAM = DotName.createSimple(RestMatrix.class.getName());
    public static final DotName REST_COOKIE_PARAM = DotName.createSimple(RestCookie.class.getName());
    public static final DotName GET = DotName.createSimple(jakarta.ws.rs.GET.class.getName());
    public static final DotName HEAD = DotName.createSimple(jakarta.ws.rs.HEAD.class.getName());
    public static final DotName DELETE = DotName.createSimple(jakarta.ws.rs.DELETE.class.getName());
    public static final DotName OPTIONS = DotName.createSimple(jakarta.ws.rs.OPTIONS.class.getName());
    public static final DotName PATCH = DotName.createSimple(jakarta.ws.rs.PATCH.class.getName());
    public static final DotName POST = DotName.createSimple(jakarta.ws.rs.POST.class.getName());
    public static final DotName PUT = DotName.createSimple(jakarta.ws.rs.PUT.class.getName());
    public static final DotName HTTP_METHOD = DotName.createSimple(jakarta.ws.rs.HttpMethod.class.getName());
    public static final DotName APPLICATION_PATH = DotName.createSimple(ApplicationPath.class.getName());
    public static final DotName PATH = DotName.createSimple(Path.class.getName());
    public static final DotName PARAM_CONVERTER_PROVIDER = DotName.createSimple(ParamConverterProvider.class.getName());
    public static final DotName FEATURE = DotName.createSimple(Feature.class.getName());
    public static final DotName DYNAMIC_FEATURE = DotName.createSimple(DynamicFeature.class.getName());
    public static final DotName CONTEXT = DotName.createSimple(Context.class.getName());
    public static final DotName CDI_INJECT = DotName.createSimple(Inject.class.getName());
    public static final DotName PRIORITY = DotName.createSimple(Priority.class.getName());
    public static final DotName APPLICATION = DotName.createSimple(Application.class.getName());
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
    public static final DotName APPLICATION_SCOPED = DotName.createSimple(ApplicationScoped.class.getName());
    public static final DotName SINGLETON = DotName.createSimple(Singleton.class.getName());
    public static final DotName REQUEST_SCOPED = DotName.createSimple(RequestScoped.class.getName());
    public static final DotName WEB_APPLICATION_EXCEPTION = DotName.createSimple(WebApplicationException.class.getName());

    public static final DotName INVOCATION_CALLBACK = DotName.createSimple(InvocationCallback.class.getName());

    public static final DotName BLOCKING = DotName.createSimple(Blocking.class.getName());
    public static final DotName RUN_ON_VIRTUAL_THREAD = DotName.createSimple(RunOnVirtualThread.class.getName());
    public static final DotName NON_BLOCKING = DotName.createSimple(NonBlocking.class.getName());
    public static final DotName SUSPENDED = DotName.createSimple(Suspended.class.getName());
    public static final DotName PRE_MATCHING = DotName.createSimple(PreMatching.class.getName());
    public static final DotName TRANSACTIONAL = DotName.createSimple("jakarta.transaction.Transactional");

    public static final DotName COLLECTION = DotName.createSimple(Collection.class.getName());
    public static final DotName LIST = DotName.createSimple(List.class.getName());
    public static final DotName SET = DotName.createSimple(Set.class.getName());
    public static final DotName SORTED_SET = DotName.createSimple(SortedSet.class.getName());
    public static final DotName MAP = DotName.createSimple(Map.class.getName());
    public static final DotName DUMMY_ELEMENT_TYPE = DotName.createSimple(DummyElementType.class.getName());
    public static final DotName MULTI_VALUED_MAP = DotName.createSimple(MultivaluedMap.class.getName());
    public static final DotName PATH_SEGMENT = DotName.createSimple(PathSegment.class.getName());
    public static final DotName INSTANT = DotName.createSimple(Instant.class.getName());
    public static final DotName LOCAL_DATE = DotName.createSimple(LocalDate.class.getName());
    public static final DotName LOCAL_DATE_TIME = DotName.createSimple(LocalDateTime.class.getName());
    public static final DotName LOCAL_TIME = DotName.createSimple(LocalTime.class.getName());
    public static final DotName OFFSET_DATE_TIME = DotName.createSimple(OffsetDateTime.class.getName());
    public static final DotName OFFSET_TIME = DotName.createSimple(OffsetTime.class.getName());
    public static final DotName ZONED_DATE_TIME = DotName.createSimple(ZonedDateTime.class.getName());

    public static final DotName UNI = DotName.createSimple(Uni.class.getName());
    public static final DotName MULTI = DotName.createSimple(Multi.class.getName());
    public static final DotName COMPLETION_STAGE = DotName.createSimple(CompletionStage.class.getName());
    public static final DotName COMPLETABLE_FUTURE = DotName.createSimple(CompletableFuture.class.getName());
    public static final DotName PUBLISHER = DotName.createSimple(Publisher.class.getName());
    public static final DotName REST_RESPONSE = DotName.createSimple(RestResponse.class.getName());

    public static final DotName INTEGER = DotName.createSimple(Integer.class.getName());
    public static final DotName LONG = DotName.createSimple(Long.class.getName());
    public static final DotName FLOAT = DotName.createSimple(Float.class.getName());
    public static final DotName DOUBLE = DotName.createSimple(Double.class.getName());
    public static final DotName BOOLEAN = DotName.createSimple(Boolean.class.getName());
    public static final DotName CHARACTER = DotName.createSimple(Character.class.getName());
    public static final DotName BIG_DECIMAL = DotName.createSimple(BigDecimal.class.getName());
    public static final DotName BIG_INTEGER = DotName.createSimple(BigInteger.class.getName());
    public static final DotName VOID = DotName.createSimple(Void.class.getName());
    public static final DotName OPTIONAL = DotName.createSimple(Optional.class.getName());

    public static final DotName PRIMITIVE_INTEGER = DotName.createSimple(int.class.getName());
    public static final DotName PRIMITIVE_LONG = DotName.createSimple(long.class.getName());
    public static final DotName PRIMITIVE_FLOAT = DotName.createSimple(float.class.getName());
    public static final DotName PRIMITIVE_DOUBLE = DotName.createSimple(double.class.getName());
    public static final DotName PRIMITIVE_BOOLEAN = DotName.createSimple(boolean.class.getName());
    public static final DotName PRIMITIVE_CHAR = DotName.createSimple(char.class.getName());

    public static final DotName STRING = DotName.createSimple(String.class.getName());
    public static final DotName BYTE_ARRAY_DOT_NAME = DotName.createSimple(byte[].class.getName());
    public static final DotName INPUT_STREAM = DotName.createSimple(InputStream.class.getName());
    public static final DotName OUTPUT_STREAM = DotName.createSimple(OutputStream.class.getName());
    public static final DotName THROWABLE = DotName.createSimple(Throwable.class.getName());

    public static final DotName JSONP_JSON_OBJECT = DotName.createSimple(jakarta.json.JsonObject.class.getName());
    public static final DotName JSONP_JSON_ARRAY = DotName.createSimple(jakarta.json.JsonArray.class.getName());
    public static final DotName JSONP_JSON_STRUCTURE = DotName.createSimple(jakarta.json.JsonStructure.class.getName());
    public static final DotName JSONP_JSON_NUMBER = DotName.createSimple(jakarta.json.JsonNumber.class.getName());
    public static final DotName JSONP_JSON_VALUE = DotName.createSimple(jakarta.json.JsonValue.class.getName());
    public static final DotName JSONP_JSON_STRING = DotName.createSimple(jakarta.json.JsonString.class.getName());

    public static final DotName CONTAINER_REQUEST_CONTEXT = DotName.createSimple(ContainerRequestContext.class.getName());
    public static final DotName CONTAINER_RESPONSE_CONTEXT = DotName.createSimple(ContainerResponseContext.class.getName());

    public static final DotName DATE_FORMAT = DotName.createSimple(org.jboss.resteasy.reactive.DateFormat.class.getName());

    public static final Set<DotName> RESOURCE_CTOR_PARAMS_THAT_NEED_HANDLING = new HashSet<>(
            Arrays.asList(QUERY_PARAM, HEADER_PARAM, PATH_PARAM, MATRIX_PARAM, COOKIE_PARAM));

    public static final Set<DotName> JAX_RS_ANNOTATIONS_FOR_FIELDS = new HashSet<>(
            Arrays.asList(BEAN_PARAM, MULTI_PART_FORM_PARAM, PATH_PARAM, QUERY_PARAM, HEADER_PARAM, FORM_PARAM, MATRIX_PARAM,
                    COOKIE_PARAM, REST_PATH_PARAM, REST_QUERY_PARAM, REST_HEADER_PARAM, REST_FORM_PARAM, REST_MATRIX_PARAM,
                    REST_COOKIE_PARAM, CONTEXT, DEFAULT_VALUE, SUSPENDED));

    public static final DotName ENCODED = DotName.createSimple(Encoded.class.getName());

    public static final DotName QUARKUS_REST_CONTAINER_RESPONSE_FILTER = DotName
            .createSimple("org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerResponseFilter");
    public static final DotName QUARKUS_REST_CONTAINER_REQUEST_FILTER = DotName
            .createSimple("org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestFilter");
    public static final DotName OBJECT = DotName.createSimple(Object.class.getName());

    public static final DotName CONTINUATION = DotName.createSimple("kotlin.coroutines.Continuation");
    public static final DotName KOTLIN_UNIT = DotName.createSimple("kotlin.Unit");

    // TODO: fix this hack by moving all the logic that handles this annotation to the server processor
    public static final DotName SERVER_EXCEPTION_MAPPER = DotName
            .createSimple("org.jboss.resteasy.reactive.server.ServerExceptionMapper");
    public static final DotName OBJECT_NAME = DotName.createSimple(Object.class.getName());
    // Types ignored for reflection used by the RESTEasy and SmallRye REST client extensions.
    private static final Set<DotName> TYPES_IGNORED_FOR_REFLECTION = new HashSet<>(Arrays.asList(
            // jakarta.json
            DotName.createSimple("jakarta.json.JsonObject"),
            DotName.createSimple("jakarta.json.JsonArray"),
            DotName.createSimple("jakarta.json.JsonValue"),

            // Jackson
            DotName.createSimple("com.fasterxml.jackson.databind.JsonNode"),

            // JAX-RS
            DotName.createSimple("jakarta.ws.rs.core.Response"),
            DotName.createSimple("jakarta.ws.rs.container.AsyncResponse"),
            DotName.createSimple("jakarta.ws.rs.core.StreamingOutput"),
            DotName.createSimple("jakarta.ws.rs.core.Form"),
            DotName.createSimple("jakarta.ws.rs.core.MultivaluedMap"),

            // RESTEasy
            DotName.createSimple("org.jboss.resteasy.plugins.providers.multipart.MultipartInput"),
            DotName.createSimple("org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput"),
            DotName.createSimple("org.jboss.resteasy.plugins.providers.multipart.MultipartOutput"),
            DotName.createSimple("org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput"),

            // Vert-x
            DotName.createSimple("io.vertx.core.json.JsonArray"),
            DotName.createSimple("io.vertx.core.json.JsonObject")));
}
