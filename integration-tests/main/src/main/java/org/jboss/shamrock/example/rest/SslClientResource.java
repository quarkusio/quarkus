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

import javax.inject.Inject;
import javax.ws.rs.GET;

import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * Disabled by default as it establishes external connections.
 * <p>
 * Uncomment when you want to test SSL support.
 */
//@Path("/ssl")
public class SslClientResource {

    @Inject
    @RestClient
    SslRestInterface sslRestInterface;

    @GET
    public String https() throws Exception {
        return sslRestInterface.get();
    }

}
