/*
 * Copyright 2019 Red Hat, Inc.
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

package io.quarkus.example.config;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.config.Config;

/**
 * Test some MicroProfile config primitives.
 * <p>
 * It needs to be there as RESTEasy recently broke MicroProfile Config
 * so we want to test this with RESTEasy in the classpath.
 */
@Path("/microprofile-config")
public class MicroProfileConfigResource {

    @Inject
    Config config;

    @GET
    @Path("/get-property-names")
    public String manual() throws Exception {
        if (!config.getPropertyNames().iterator().hasNext()) {
            return "No config property found. Some were expected.";
        }

        return "OK";
    }

}
