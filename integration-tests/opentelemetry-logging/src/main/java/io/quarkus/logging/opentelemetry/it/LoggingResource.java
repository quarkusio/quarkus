/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package io.quarkus.logging.opentelemetry.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/logging-opentelemetry")
@ApplicationScoped
public class LoggingResource {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingResource.class);

    @GET
    @Path("/hello")
    public String hello() {
        LOG.info("Hello {}", "World");
        return "Hello World";
    }

    @GET
    @Path("/exception")
    public String exception() {
        var exception = new RuntimeException("Exception!");
        LOG.error("Oh no {}", exception.getMessage(), exception);
        return "Oh no! An exception";
    }
}
