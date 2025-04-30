package io.quarkus.resteasy.reactive.server.test.simple;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Providers;

import org.jboss.resteasy.reactive.RestHeader;
import org.jboss.resteasy.reactive.server.SimpleResourceInfo;

import io.quarkus.runtime.BlockingOperationControl;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

@Path("/simple")
public class SimpleQuarkusRestResource {

    private static final StackTraceElement[] EMPTY_STACK_TRACE = new StackTraceElement[0];

    @Inject
    HelloService service;

    @GET
    public String get() {
        return "GET";
    }

    @Path("sub")
    public Object subResource() {
        return new SubResource();
    }

    @GET
    @Path("/hello")
    public String hello() {
        return service.sayHello();
    }

    @GET
    @Path("{id}")
    public String get(@PathParam("id") String id) {
        return "GET:" + id;
    }

    @POST
    @Path("params/{p}")
    public String params(@PathParam("p") String p,
            @QueryParam("q") String q,
            @HeaderParam("h") int h,
            @FormParam("f") String f) {
        return "params: p: " + p + ", q: " + q + ", h: " + h + ", f: " + f;
    }

    @POST
    public String post() {
        return "POST";
    }

    @DELETE
    public String delete() {
        return "DELETE";
    }

    @PUT
    public String put() {
        return "PUT";
    }

    @PATCH
    public String patch() {
        return "PATCH";
    }

    @OPTIONS
    public String options() {
        return "OPTIONS";
    }

    @HEAD
    public Response head() {
        return Response.ok().header("Stef", "head").build();
    }

    @GET
    @Path("/person")
    @Produces(MediaType.APPLICATION_JSON)
    public Person getPerson() {
        Person person = new Person();
        person.setFirst("Bob");
        person.setLast("Builder");
        return person;
    }

    @GET
    @Path("/blocking")
    @Blocking
    public String blocking() {
        service.sayHello();
        return String.valueOf(BlockingOperationControl.isBlockingAllowed());
    }

    @GET
    @Path("providers")
    public Response filters(@Context Providers providers) {
        // TODO: enhance this test
        return Response.ok().entity(providers.getExceptionMapper(TestException.class).getClass().getName()).build();
    }

    @GET
    @Path("filters")
    public Response filters(@Context HttpHeaders headers, @RestHeader("filter-request") String header) {
        return Response.ok().header("filter-request", header).build();
    }

    @GET
    @Path("header-param-list")
    public Object headerParamWithList(@HeaderParam("header") List list) {
        return collectionToString(list);
    }

    @GET
    @Path("rest-header-list")
    public Object restHeaderWithList(@RestHeader List header) {
        return collectionToString(header);
    }

    @GET
    @Path("header-param-set")
    public Object headerParamWithSet(@HeaderParam("header") Set list) {
        return collectionToString(list);
    }

    @GET
    @Path("rest-header-set")
    public Object restHeaderWithSet(@RestHeader Set header) {
        return collectionToString(header);
    }

    @GET
    @Path("header-param-sorted-set")
    public Object headerParamWithSortedSet(@HeaderParam("header") SortedSet list) {
        return collectionToString(list);
    }

    @GET
    @Path("rest-header-sorted-set")
    public String restHeaderWithSortedSet(@RestHeader SortedSet header) {
        return collectionToString(header);
    }

    @GET
    @Path("feature-filters")
    public Response featureFilters(@Context HttpHeaders headers) {
        return Response.ok().header("feature-filter-request", headers.getHeaderString("feature-filter-request")).build();
    }

    @GET
    @Path("dynamic-feature-filters")
    public Response dynamicFeatureFilters(@Context HttpHeaders headers) {
        return Response.ok().header("feature-filter-request", headers.getHeaderString("feature-filter-request")).build();
    }

    @GET
    @Path("fooFilters")
    @Foo
    public Response fooFilters(@Context HttpHeaders headers) {
        return Response.ok().header("filter-request", headers.getHeaderString("filter-request")).build();
    }

    @GET
    @Path("barFilters")
    @Bar
    public Response barFilters(@Context HttpHeaders headers) {
        return Response.ok().header("filter-request", headers.getHeaderString("filter-request")).build();
    }

    @GET
    @Path("fooBarFilters")
    @Foo
    @Bar
    public Response fooBarFilters(@Context HttpHeaders headers) {
        return Response.ok().header("filter-request", headers.getHeaderString("filter-request")).build();
    }

    @GET
    @Path("mapped-exception")
    public String mappedException() {
        TestException exception = new TestException();
        exception.setStackTrace(EMPTY_STACK_TRACE);
        throw exception;
    }

    @GET
    @Path("feature-mapped-exception")
    public String featureMappedException() {
        FeatureMappedException exception = new FeatureMappedException();
        exception.setStackTrace(EMPTY_STACK_TRACE);
        throw exception;
    }

    @GET
    @Path("unknown-exception")
    public String unknownException() {
        RuntimeException exception = new RuntimeException("OUCH");
        exception.setStackTrace(EMPTY_STACK_TRACE);
        throw exception;
    }

    @GET
    @Path("web-application-exception")
    public String webApplicationException() {
        throw new WebApplicationException(Response.status(666).entity("OK").build());
    }

    @GET
    @Path("writer")
    public TestClass writer() {
        return new TestClass();
    }

    @GET
    @Path("fast-writer")
    @Produces("text/plain")
    public String fastWriter() {
        return "OK";
    }

    @GET
    @Path("lookup-writer")
    public Object slowWriter() {
        return "OK";
    }

    @GET
    @Path("writer/vertx-buffer")
    public Buffer vertxBuffer() {
        return Buffer.buffer("VERTX-BUFFER");
    }

    @GET
    @Path("writer/mutiny-buffer")
    public io.vertx.mutiny.core.buffer.Buffer mutinyBuffer() {
        return io.vertx.mutiny.core.buffer.Buffer.buffer("MUTINY-BUFFER");
    }

    @GET
    @Path("async/cs/ok")
    public CompletionStage<String> asyncCompletionStageOK() {
        return CompletableFuture.completedFuture("CS-OK");
    }

    @GET
    @Path("async/cs/fail")
    public CompletionStage<String> asyncCompletionStageFail() {
        CompletableFuture<String> ret = new CompletableFuture<>();
        ret.completeExceptionally(new TestException());
        return ret;
    }

    @GET
    @Path("async/cf/blocking")
    public CompletableFuture<String> asyncCompletableFutureIsBlocking() {
        return CompletableFuture.completedFuture(Boolean.toString(BlockingOperationControl.isBlockingAllowed()));
    }

    @GET
    @Path("async/cf/ok")
    public CompletableFuture<String> asyncCompletableFutureOK() {
        return CompletableFuture.completedFuture("CF-OK");
    }

    @GET
    @Path("async/cf/fail")
    public CompletableFuture<String> asyncCompletableFutureFail() {
        CompletableFuture<String> ret = new CompletableFuture<>();
        ret.completeExceptionally(new TestException());
        return ret;
    }

    @GET
    @Path("async/uni/ok")
    public Uni<String> asyncUniOK() {
        return Uni.createFrom().item("UNI-OK");
    }

    @Produces(MediaType.APPLICATION_JSON)
    @GET
    @Path("async/uni/list")
    public Uni<List<Person>> asyncUniListJson() {
        Person person = new Person();
        person.setFirst("Bob");
        person.setLast("Builder");
        return Uni.createFrom().item(Arrays.asList(person));
    }

    @GET
    @Path("async/uni/fail")
    public Uni<String> asyncUniStageFail() {
        return Uni.createFrom().failure(new TestException());
    }

    @GET
    @Path("pre-match")
    public String preMatchGet() {
        return "pre-match-get";
    }

    @POST
    @Path("pre-match")
    public String preMatchPost() {
        return "pre-match-post";
    }

    @GET
    @Path("request-response-params")
    public String requestAndResponseParams(@Context HttpServerRequest request, @Context HttpServerResponse response) {
        response.headers().add("dummy", "value");
        return request.remoteAddress().host();
    }

    @GET
    @Path("jax-rs-request")
    public String jaxRsRequest(@Context Request request) {
        return request.getMethod();
    }

    @GET
    @Path("resource-info")
    public Response resourceInfo(@Context ResourceInfo resourceInfo, @Context HttpHeaders headers) {
        return Response.ok()
                .header("class-name", resourceInfo.getResourceClass().getSimpleName())
                .header("method-name", headers.getHeaderString("method-name"))
                .build();
    }

    @Path("form-map")
    @POST
    @Produces(MediaType.APPLICATION_FORM_URLENCODED)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public MultivaluedMap<String, String> map(MultivaluedMap<String, String> map) {
        return map;
    }

    @Path("jsonp-object")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String jsonpObject(JsonObject jsonbObject) {
        return jsonbObject.getString("k");
    }

    @Path("jsonp-array")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Integer jsonpArray(JsonArray jsonArray) {
        return jsonArray.size();
    }

    @Path("/bool")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public boolean bool(boolean bool) {
        return bool;
    }

    @Path("/trace")
    @TRACE
    public Response trace() {
        return Response.status(Response.Status.OK).build();
    }

    @GET
    @Path("simplifiedResourceInfo")
    @Produces(MediaType.TEXT_PLAIN)
    public String simplifiedResourceInfo(@Context SimpleResourceInfo simplifiedResourceInfo) {
        return simplifiedResourceInfo.getResourceClass().getName() + "#" + simplifiedResourceInfo.getMethodName() + "-"
                + simplifiedResourceInfo.parameterTypes().length;
    }

    @GET
    @Path("bigDecimal/{val}")
    @Produces(MediaType.TEXT_PLAIN)
    public String bigDecimalConverter(BigDecimal val) {
        return val.toString();
    }

    private String collectionToString(Collection list) {
        return (String) list.stream().map(Object::toString).collect(Collectors.joining(", "));
    }
}
