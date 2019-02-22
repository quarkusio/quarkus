/**
 * Copyright 2018 Red Hat, Inc, and individual contributors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.quarkus.security.test;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 6/13/18
 */
@Path("/parameterized-paths")
public class ParametrizedPathsResource {
    @GET
    @Path("/my/{path}/admin")
    @RolesAllowed("admin")
    public String admin(@PathParam("path") String path) {
        return "Admin accessed " + path;
    }

    @GET
    @Path("/my/{path}/view")
    @RolesAllowed("user")
    public String view(@PathParam("path") String path) {
        return "View accessed " + path;
    }
}
