/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.it.rest;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlRootElement;

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.reactivex.Single;

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
    @Path("/complex")
    @Produces("application/json")
    public List<ComponentType> complex() {
        ComponentType ret = new ComponentType();
        ret.setValue("component value");
        CollectionType ct = new CollectionType();
        ct.setValue("collection type");
        ret.getCollectionTypes().add(ct);
        SubComponent subComponent = new SubComponent();
        subComponent.getData().add("sub component list value");
        ret.setSubComponent(subComponent);
        return Collections.singletonList(ret);
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
    public MyEntity fromJson() {
        MyEntity entity = new MyEntity();
        entity.name = "my entity name";
        entity.value = "my entity value";

        JsonbConfig config = new JsonbConfig();
        Jsonb jsonb = JsonbBuilder.create(config);
        String json = jsonb.toJson(entity);
        MyEntity fromJsonEntity = jsonb.fromJson(json, MyEntity.class);

        return fromJsonEntity;
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
    public String gzip(byte[] message) {
        return "gzipped:" + new String(message);
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

    @RegisterForReflection
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
