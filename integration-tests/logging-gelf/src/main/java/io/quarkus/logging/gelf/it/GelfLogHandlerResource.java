/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.quarkus.logging.gelf.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

/**
 * This endpoint allow to test central logging solution by generating a log event when
 * calling it's `log` operation.
 */
@Path("/gelf-log-handler")
@ApplicationScoped
public class GelfLogHandlerResource {
    private static final Logger LOG = Logger.getLogger(GelfLogHandlerResource.class);

    @GET
    public void log() {
        MDC.put("field3", 99);
        MDC.put("field4", 98);
        LOG.info("Some useful log message");
    }

}
