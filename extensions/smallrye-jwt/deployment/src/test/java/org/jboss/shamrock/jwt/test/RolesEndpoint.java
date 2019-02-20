/*
 * Copyright (c) 2016-2017 Contributors to the Eclipse Foundation
 *
 *  See the NOTICE file(s) distributed with this work for additional
 *  information regarding copyright ownership.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.jboss.shamrock.jwt.test;

import java.security.Principal;
import java.util.Date;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.ClaimValue;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/endp")
@DenyAll
@RequestScoped
public class RolesEndpoint {

    @Inject
    JsonWebToken jwtPrincipal;

    @Inject
    @Claim("raw_token")
    ClaimValue<String> rawToken;

    @GET
    @Path("/echo")
    @RolesAllowed("Echoer")
    public String echoInput(@Context SecurityContext sec, @QueryParam("input") String input) {
        Principal user = sec.getUserPrincipal();
        return input + ", user="+user.getName();
    }

    @GET
    @Path("/echo2")
    @RolesAllowed("NoSuchUser")
    public String echoInput2(@Context SecurityContext sec, @QueryParam("input") String input) {
        Principal user = sec.getUserPrincipal();
        String name = user != null ? user.getName() : "<null>";
        return input + ", user="+name;
    }

    @GET
    @Path("/echoNeedsToken2Role")
    @RolesAllowed("Token2Role")
    public String echoNeedsToken2Role(@Context SecurityContext sec, @QueryParam("input") String input) {
        Principal user = sec.getUserPrincipal();
        return input + ", user="+user.getName();
    }

    /**
     * Validate that the  SecurityContext#getUserPrincipal is a JsonWebToken
     * @param sec
     * @return
     */
    @GET
    @Path("/getPrincipalClass")
    @RolesAllowed("Tester")
    public String getPrincipalClass(@Context SecurityContext sec) {
        Principal user = sec.getUserPrincipal();
        boolean isJsonWebToken = user instanceof JsonWebToken;
        return "isJsonWebToken:"+isJsonWebToken;
    }

    /**
     * This endpoint requires a role that is mapped to the group1 role
     * @return principal name
     */
    @GET
    @Path("/needsGroup1Mapping")
    @RolesAllowed("Group1MappedRole")
    public String needsGroup1Mapping(@Context SecurityContext sec) {
        Principal user = sec.getUserPrincipal();
        sec.isUserInRole("group1");
        return user.getName();
    }

    /**
     * This endpoint requires a Tester role, and also validates that the caller has the role Echoer by calling
     * {@linkplain SecurityContext#isUserInRole(String)}.
     *
     * @return principal name or FORBIDDEN error
     */
    @GET
    @Path("/checkIsUserInRole")
    @RolesAllowed("Tester")
    public Response checkIsUserInRole(@Context SecurityContext sec) {
        Principal user = sec.getUserPrincipal();
        Response response;
        if(!sec.isUserInRole("Echoer")) {
            response = Response.status(new Response.StatusType() {
                @Override
                public int getStatusCode() {
                    return Response.Status.FORBIDDEN.getStatusCode();
                }

                @Override
                public Response.Status.Family getFamily() {
                    return Response.Status.FORBIDDEN.getFamily();
                }

                @Override
                public String getReasonPhrase() {
                    return "SecurityContext.isUserInRole(Echoer) was false";
                }
            }).build();
        }
        else {
            response = Response.ok(user.getName(), MediaType.TEXT_PLAIN).build();
        }
        return response;
    }

    @GET
    @Path("/getInjectedPrincipal")
    @RolesAllowed("Tester")
    public String getInjectedPrincipal(@Context SecurityContext sec) {
        boolean isJsonWebToken = this.jwtPrincipal instanceof JsonWebToken;
        return "isJsonWebToken:"+isJsonWebToken;
    }

    @GET
    @Path("/heartbeat")
    @PermitAll
    public String heartbeat() {
        return "Heartbeat: "+ new Date(System.currentTimeMillis()).toString();
    }
}
