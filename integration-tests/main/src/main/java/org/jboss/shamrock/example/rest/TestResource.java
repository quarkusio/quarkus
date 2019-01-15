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

package org.jboss.shamrock.example.rest;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.xml.bind.annotation.XmlRootElement;

import io.reactivex.Single;

@Path("/test")
public class TestResource {

    @Context
    HttpServletRequest request;

    private final AtomicInteger count = new AtomicInteger(0);

    @GET
    public String getTest() {
        return "TEST";
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
    @Path("/fooprovider")
    @Produces("application/foo")
    public String fooProvider() {
        return "hello";
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

    @Path("params/{path}")
    @GET
    public void regularParams(@PathParam("path") String path,
                              @FormParam("form") String form,
                              @CookieParam("cookie") String cookie,
                              @HeaderParam("header") String header,
                              @MatrixParam("matrix") String matrix,
                              @QueryParam("query") String query) {
    }

    @Path("params2/{path}")
    @GET
    public void resteasyParams(@org.jboss.resteasy.annotations.jaxrs.PathParam String path,
                               @org.jboss.resteasy.annotations.jaxrs.FormParam String form,
                               @org.jboss.resteasy.annotations.jaxrs.CookieParam String cookie,
                               @org.jboss.resteasy.annotations.jaxrs.HeaderParam String header,
                               @org.jboss.resteasy.annotations.jaxrs.MatrixParam String matrix,
                               @org.jboss.resteasy.annotations.jaxrs.QueryParam String query) {
    }
}
