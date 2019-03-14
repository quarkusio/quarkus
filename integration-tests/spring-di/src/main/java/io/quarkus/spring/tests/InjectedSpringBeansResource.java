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

package io.quarkus.spring.tests;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

@Path("/")
public class InjectedSpringBeansResource {

    @Inject
    GreeterBean greeterBean;
    @Inject
    RequestBean requestBean;
    @Inject
    SessionBean sessionBean;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return greeterBean.greet("world");
    }

    @GET
    @Path("request")
    @Produces(MediaType.TEXT_PLAIN)
    public int getRequestValue() {
        return requestBean.getValue();
    }

    @GET
    @Path("session")
    @Produces(MediaType.TEXT_PLAIN)
    public int getSessionValue() {
        return sessionBean.getValue();
    }

    @POST
    @Path("invalidate")
    public void invalidate(final @Context HttpServletRequest req) {
        req.getSession().invalidate();
    }
}
