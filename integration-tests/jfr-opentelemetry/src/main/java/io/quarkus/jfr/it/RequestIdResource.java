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
package io.quarkus.jfr.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.jfr.runtime.RequestIdProducer;
import io.quarkus.jfr.runtime.TracingRequestId;
import io.quarkus.jfr.runtime.TracingRequestIdProducer;

@Path("")
@ApplicationScoped
public class RequestIdResource {

    @Inject
    RequestIdProducer idProducer;

    @Path("/requestId")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public TracingId hello() {
        TracingRequestIdProducer tracingRequestIdProducer = (TracingRequestIdProducer) idProducer;
        TracingRequestId id = tracingRequestIdProducer.create();
        return new TracingId(id.id, id.traceId, id.spanId);
    }

    class TracingId {

        public String id;
        public String traceId;
        public String spanId;

        public TracingId() {
        }

        public TracingId(String id, String traceId, String spanId) {
            this.id = id;
            this.traceId = traceId;
            this.spanId = spanId;
        }
    }
}
