package io.quarkus.it.rest;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.MatrixParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.reactivex.Single;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@Path("/test")
public class TestResource {

    @Context
    HttpServletRequest request;

    @Inject
    ExternalService service;

    @Inject
    ServiceWithConfig config;

    private final AtomicInteger count = new AtomicInteger(0);

    @GET
    public String getTest() {
        return "TEST";
    }

    @GET
    @Path("/service")
    public String service() {
        return service.service();
    }

    @GET
    @Path("/config/host")
    public String configPort() {
        return config.host();
    }

    @GET
    @Path("/config/message")
    public String configMessage() {
        return config.message();
    }

    @GET
    @Path("/config/names")
    @Produces("application/json")
    public String configNames() {
        return String.join(",", config.names());
    }

    @GET
    @Path("/count")
    public int count() {
        return count.incrementAndGet();
    }

    @GET
    @Path("/int/{val}")
    public Integer getInt(@PathParam("val") Integer val) {
        return val + 1;
    }

    @GET
    @Path("/request-test")
    public String requestTest() {
        return request.getRequestURI();
    }

    @GET
    @Path("/jackson")
    @Produces("application/json")
    public MyData get() {
        MyData m = new MyData();
        m.setName("Stuart");
        m.setValue("A Value");
        return m;
    }

    @GET
    @Path("/jsonp")
    @Produces("application/json")
    public JsonObject jsonp() {
        return Json.createObjectBuilder()
                .add("name", "Stuart")
                .add("value", "A Value")
                .build();
    }

    @GET
    @Produces("application/xml")
    @Path("/xml")
    public XmlObject xml() {
        XmlObject xmlObject = new XmlObject();
        xmlObject.setValue("A Value");
        return xmlObject;
    }

    @POST
    @Consumes("application/xml")
    @Produces("text/plain")
    @Path("/consumeXml")
    public String consumeXml(XmlObject xmlObject) {
        return xmlObject.getValue();
    }

    @GET
    @Path("/cs")
    public CompletionStage<String> cs() {
        return CompletableFuture.completedFuture("Hello");
    }

    @GET
    @Path("/rx")
    public Single<String> rx() {
        return Single.just("Hello");
    }

    @GET
    @Path("/uni")
    public Uni<String> uni() {
        return Uni.createFrom().item("Hello from Uni");
    }

    @GET
    @Path("/multi")
    public Multi<String> multi() {
        return Multi.createFrom().items("Hello", "from", "Multi");
    }

    @GET
    @Path("/uniType")
    public Uni<ComponentType> uniType() {
        return Uni.createFrom().item(this::createComponent);
    }

    @GET
    @Path("/multiType")
    public Multi<ComponentType> multiType() {
        return Multi.createFrom().items(createComponent(), createComponent());
    }

    @GET
    @Path("/compType")
    public ComponentType getComponentType() {
        return createComponent();
    }

    @GET
    @Path("/complex")
    @Produces("application/json")
    public List<ComponentType> complex() {
        return Collections.singletonList(createComponent());
    }

    private ComponentType createComponent() {
        ComponentType ret = new ComponentType();
        ret.setValue("component value");
        CollectionType ct = new CollectionType();
        ct.setValue("collection type");
        ret.getCollectionTypes().add(ct);
        SubComponent subComponent = new SubComponent();
        subComponent.getData().add("sub component list value");
        ret.setSubComponent(subComponent);
        return ret;
    }

    @GET
    @Path("/headers")
    @Produces("application/json")
    public Map<String, String> getAllHeaders(@Context HttpHeaders headers) {
        Map<String, String> resultMap = new HashMap<>();
        headers.getRequestHeaders().forEach(
                (key, values) -> resultMap.put(key, String.join(",", values)));
        return resultMap;
    }

    @GET
    @Path("/subclass")
    @Produces("application/json")
    public ParentClass subclass() {
        ChildClass child = new ChildClass();
        child.setName("my name");
        child.setValue("my value");
        return child;
    }

    @GET
    @Path("/implementor")
    @Produces("application/json")
    public MyInterface implementor() {
        MyImplementor child = new MyImplementor();
        child.setName("my name");
        child.setValue("my value");
        return child;
    }

    @GET
    @Path("/response")
    @Produces("application/json")
    public Response response() {
        MyEntity entity = new MyEntity();
        entity.setName("my entity name");
        entity.setValue("my entity value");
        return Response.ok(entity).build();
    }

    @GET
    @Path("/openapi/responses")
    @Produces("application/json")
    @APIResponse(content = @Content(mediaType = "application/json", schema = @Schema(type = SchemaType.OBJECT, implementation = MyOpenApiEntityV1.class)))
    public Response openApiResponse() {
        MyOpenApiEntityV1 entity = new MyOpenApiEntityV1();
        entity.setName("my openapi entity name");
        return Response.ok(entity).build();
    }

    @GET
    @Path("/openapi/responses/{version}")
    @Produces("application/json")
    @APIResponses({
            @APIResponse(content = @Content(mediaType = "application/json", schema = @Schema(type = SchemaType.OBJECT, implementation = MyOpenApiEntityV1.class))),
            @APIResponse(content = @Content(mediaType = "application/json", schema = @Schema(type = SchemaType.OBJECT, implementation = MyOpenApiEntityV2.class)))
    })
    public Response openApiResponses(@PathParam("version") String version) {
        if ("v1".equals(version)) {
            MyOpenApiEntityV1 entityV1 = new MyOpenApiEntityV1();
            entityV1.setName("my openapi entity version one name");
            return Response.ok(entityV1).build();
        }

        MyOpenApiEntityV2 entityV2 = new MyOpenApiEntityV2();
        entityV2.setName("my openapi entity version two name");
        entityV2.setValue(version);
        return Response.ok(entityV2).build();
    }

    @GET
    @Path("/openapi/schema")
    @Produces("application/json")
    public Response openApiSchemaResponses() {
        MyOpenApiSchemaEntity entity = new MyOpenApiSchemaEntity();
        entity.setName("my openapi schema");
        return Response.ok(entity).build();
    }

    @GET
    @APIResponses({ @APIResponse(responseCode = "204", description = "APIResponses with a no content response") })
    @Path("/openapi/no-content/api-responses")
    public Response apiResponsesNoContent() {
        return Response.noContent().build();
    }

    @GET
    @APIResponse(responseCode = "204", description = "APIResponse with no content response")
    @Path("/openapi/no-content/api-response")
    public Response apiResponseNoContent() {
        return Response.noContent().build();
    }

    @GET
    @Path("/fooprovider")
    @Produces("application/foo")
    public String fooProvider() {
        return "hello";
    }

    @GET
    @Path("/from-json")
    @Produces("application/json")
    public MyEntity fromJson() throws Exception {
        MyEntity entity = new MyEntity();
        entity.name = "my entity name";
        entity.value = "my entity value";

        JsonbConfig config = new JsonbConfig();
        try (Jsonb jsonb = JsonbBuilder.create(config)) {
            String json = jsonb.toJson(entity);
            MyEntity fromJsonEntity = jsonb.fromJson(json, MyEntity.class);

            return fromJsonEntity;
        }
    }

    @GET
    @Path("/echo/{echo}")
    @Produces("application/json")
    public String echo(@PathParam("echo") String echo) {
        return echo;
    }

    @GET
    @Path("params/{path}")
    public void regularParams(@PathParam("path") String path,
            @FormParam("form") String form,
            @CookieParam("cookie") String cookie,
            @HeaderParam("header") String header,
            @MatrixParam("matrix") String matrix,
            @QueryParam("query") String query) {
    }

    // FIXME: don't enable this until https://github.com/smallrye/smallrye-open-api/issues/197 has been fixed
    //    // make sure these don't break the build when fields
    //    @org.jboss.resteasy.annotations.jaxrs.PathParam
    //    String pathField;
    //    @org.jboss.resteasy.annotations.jaxrs.FormParam
    //    String formField;
    //    @org.jboss.resteasy.annotations.jaxrs.CookieParam
    //    String cookieField;
    //    @org.jboss.resteasy.annotations.jaxrs.HeaderParam
    //    String headerField;
    //    @org.jboss.resteasy.annotations.jaxrs.MatrixParam
    //    String matrixField;
    //    @org.jboss.resteasy.annotations.jaxrs.QueryParam
    //    String queryField;
    //
    //    // make sure these don't break the build when properties
    //    public String getPathProperty() {
    //        return null;
    //    }
    //
    //    @org.jboss.resteasy.annotations.jaxrs.PathParam
    //    public void setPathProperty(String p) {
    //    }
    //
    //    public String getFormProperty() {
    //        return null;
    //    }
    //
    //    @org.jboss.resteasy.annotations.jaxrs.FormParam
    //    public void setFormProperty(String p) {
    //    }
    //
    //    public String getCookieProperty() {
    //        return null;
    //    }
    //
    //    @org.jboss.resteasy.annotations.jaxrs.CookieParam
    //    public void setCookieProperty(String p) {
    //    }
    //
    //    public String getHeaderProperty() {
    //        return null;
    //    }
    //
    //    @org.jboss.resteasy.annotations.jaxrs.HeaderParam
    //    public void setHeaderProperty(String p) {
    //    }
    //
    //    public String getMatrixProperty() {
    //        return null;
    //    }
    //
    //    @org.jboss.resteasy.annotations.jaxrs.MatrixParam
    //    public void setMatrixProperty(String p) {
    //    }
    //
    //    public String getQueryProperty() {
    //        return null;
    //    }
    //
    //    @org.jboss.resteasy.annotations.jaxrs.QueryParam
    //    public void setQueryProperty(String p) {
    //    }

    @GET
    @Path("params2/{path}")
    public void resteasyParams(@org.jboss.resteasy.annotations.jaxrs.PathParam String path,
            @org.jboss.resteasy.annotations.jaxrs.FormParam String form,
            @org.jboss.resteasy.annotations.jaxrs.CookieParam String cookie,
            @org.jboss.resteasy.annotations.jaxrs.HeaderParam String header,
            @org.jboss.resteasy.annotations.jaxrs.MatrixParam String matrix,
            @org.jboss.resteasy.annotations.jaxrs.QueryParam String query) {
    }

    @POST
    @Path("/gzip")
    public String gzip(byte[] message) throws UnsupportedEncodingException {
        return "gzipped:" + new String(message, "UTF-8");
    }

    @POST
    @Path("/max-body-size")
    public String echoPayload(String payload) {
        return payload;
    }

    @GET
    @Path("/failure")
    public Response alwaysFail() {
        return Response.serverError().build();
    }

    @XmlRootElement
    public static class XmlObject {

        String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public static class MyData {
        private String name;
        private String value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public static class ParentClass {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class ChildClass extends ParentClass {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public static class MyImplementor implements MyInterface {
        private String name;
        private String value;

        @Override
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public interface MyInterface {

        String getValue();

        String getName();
    }

    public static class MyEntity {
        private String name;
        private String value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    @RegisterForReflection(targets = MyEntity.class)
    public static class EmptyClass {

    }

    public static class MyOpenApiEntityV1 {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class MyOpenApiEntityV2 {
        private String value;
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    @Schema()
    public static class MyOpenApiSchemaEntity {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

}
