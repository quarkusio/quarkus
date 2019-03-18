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

package io.quarkus.vertx.tests;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * @author Thomas Ssegismont
 */
@Path("/body-writers")
public class JsonTestResource {

    @GET
    @Path("/json/sync")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject jsonSync() {
        return new JsonObject().put("Hello", "World");
    }

    @GET
    @Path("/array/sync")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonArray arraySync() {
        return new JsonArray().add("Hello").add("World");
    }

    @GET
    @Path("/json/async")
    @Produces(MediaType.APPLICATION_JSON)
    public CompletionStage<JsonObject> jsonAsync() {
        return CompletableFuture.completedFuture(new JsonObject().put("Hello", "World"));
    }

    @GET
    @Path("/array/async")
    @Produces(MediaType.APPLICATION_JSON)
    public CompletionStage<JsonArray> arrayAsync() {
        return CompletableFuture.completedFuture(new JsonArray().add("Hello").add("World"));
    }
}
