/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.quarkus.it.vertx;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import javax.ws.rs.*;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * @author Thomas Ssegismont
 */
@Path("/json-bodies")
public class JsonTestResource {

    @GET
    @Path("/json/sync")
    @Produces(APPLICATION_JSON)
    public JsonObject jsonSync() {
        return new JsonObject().put("Hello", "World");
    }

    @POST
    @Path("/json/sync")
    @Consumes(APPLICATION_JSON)
    @Produces(TEXT_PLAIN)
    public String jsonSync(JsonObject jsonObject) {
        return "Hello " + jsonObject.getString("Hello");
    }

    @GET
    @Path("/array/sync")
    @Produces(APPLICATION_JSON)
    public JsonArray arraySync() {
        return new JsonArray().add("Hello").add("World");
    }

    @POST
    @Path("/array/sync")
    @Consumes(APPLICATION_JSON)
    @Produces(TEXT_PLAIN)
    public String arraySync(JsonArray jsonArray) {
        return jsonArray.stream().map(String.class::cast).collect(Collectors.joining(" "));
    }

    @GET
    @Path("/json/async")
    @Produces(APPLICATION_JSON)
    public CompletionStage<JsonObject> jsonAsync() {
        return CompletableFuture.completedFuture(new JsonObject().put("Hello", "World"));
    }

    @GET
    @Path("/array/async")
    @Produces(APPLICATION_JSON)
    public CompletionStage<JsonArray> arrayAsync() {
        return CompletableFuture.completedFuture(new JsonArray().add("Hello").add("World"));
    }
}
