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

package io.quarkus.example.rest;

import java.net.URL;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/client")
public class ClientResource {

    @Inject
    @RestClient
    RestInterface restInterface;

    @GET
    @Path("/manual")
    public String manual() throws Exception {
        RestInterface iface = RestClientBuilder.newBuilder()
                .baseUrl(new URL(System.getProperty("test.url")))
                .build(RestInterface.class);
        return iface.get();
    }

    @GET
    @Path("/cdi")
    public String cdi() throws Exception {
        return restInterface.get();
    }

    @GET
    @Path("manual/jackson")
    @Produces("application/json")
    public TestResource.MyData getDataManual() throws Exception {
        RestInterface iface = RestClientBuilder.newBuilder()
                .baseUrl(new URL(System.getProperty("test.url")))
                .build(RestInterface.class);
        System.out.println(iface.getData());
        return iface.getData();
    }

    @GET
    @Path("cdi/jackson")
    @Produces("application/json")
    public TestResource.MyData getDataCdi() {
        return restInterface.getData();
    }

    @GET
    @Path("/manual/complex")
    @Produces("application/json")
    public List<ComponentType> complexManual() throws Exception {
        RestInterface iface = RestClientBuilder.newBuilder()
                .baseUrl(new URL(System.getProperty("test.url")))
                .build(RestInterface.class);
        System.out.println(iface.complex());
        return iface.complex();
    }

    @GET
    @Path("/cdi/complex")
    @Produces("application/json")
    public List<ComponentType> complexCdi() {
        return restInterface.complex();
    }

}
