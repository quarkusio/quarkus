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
package io.quarkus.jwt.test;

import java.util.List;
import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.Claims;

@Path("/endp")
@RequestScoped
@RolesAllowed("Tester")
public class PrimitiveInjectionEndpoint {
    @Inject
    @Claim("raw_token")
    String rawToken;
    @Inject
    @Claim("iss")
    String issuer;
    @Inject
    @Claim("upn")
    String upn;
    @Inject
    @Claim("jti")
    String jti;
    @Inject
    @Claim("aud")
    Set<String> aud;
    @Inject
    @Claim("groups")
    Set<String> groups;
    @Inject
    @Claim("iat")
    long issuedAt;
    @Inject
    @Claim("exp")
    long expiration;
    @Inject
    @Claim("sub")
    String subject;
    @Inject
    @Claim("customString")
    String customString;
    @Inject
    @Claim("customDouble")
    double customDouble;

    @GET
    @Path("/verifyInjectedIssuer")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject verifyInjectedIssuer(@QueryParam("iss") String iss) {
        boolean pass = false;
        String msg;
        String issValue = this.issuer;
        if (issValue == null || issValue.length() == 0) {
            msg = Claims.iss.name() + "value is null or empty, FAIL";
        } else if (issValue.equals(iss)) {
            msg = Claims.iss.name() + " PASS";
            pass = true;
        } else {
            msg = String.format("%s: %s != %s", Claims.iss.name(), issValue, iss);
        }
        JsonObject result = Json.createObjectBuilder()
                .add("pass", pass)
                .add("msg", msg)
                .build();
        return result;
    }

    @GET
    @Path("/verifyInjectedRawToken")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject verifyInjectedRawToken(@QueryParam("raw_token") String rt) {
        boolean pass = false;
        String msg;
        // raw_token
        String rawTokenValue = this.rawToken;
        if (rawTokenValue == null || rawTokenValue.length() == 0) {
            msg = Claims.raw_token.name() + "value is null or empty, FAIL";
        } else if (rawTokenValue.equals(rt)) {
            msg = Claims.raw_token.name() + " PASS";
            pass = true;
        } else {
            msg = String.format("%s: %s != %s", Claims.raw_token.name(), rawTokenValue, rt);
        }
        JsonObject result = Json.createObjectBuilder()
                .add("pass", pass)
                .add("msg", msg)
                .build();
        return result;
    }

    @GET
    @Path("/verifyInjectedJTI")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject verifyInjectedJTI(@QueryParam("jti") String jwtID) {
        boolean pass = false;
        String msg;
        // jti
        String jtiValue = this.jti;
        if (jtiValue == null || jtiValue.length() == 0) {
            msg = Claims.jti.name() + "value is null or empty, FAIL";
        } else if (jtiValue.equals(jwtID)) {
            msg = Claims.jti.name() + " PASS";
            pass = true;
        } else {
            msg = String.format("%s: %s != %s", Claims.jti.name(), jtiValue, jwtID);
        }
        JsonObject result = Json.createObjectBuilder()
                .add("pass", pass)
                .add("msg", msg)
                .build();
        return result;
    }

    @GET
    @Path("/verifyInjectedUPN")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject verifyInjectedUPN(@QueryParam("upn") String upn) {
        boolean pass = false;
        String msg;
        // uPN
        String upnValue = this.upn;
        if (upnValue == null || upnValue.length() == 0) {
            msg = Claims.upn.name() + "value is null or empty, FAIL";
        } else if (upnValue.equals(upn)) {
            msg = Claims.upn.name() + " PASS";
            pass = true;
        } else {
            msg = String.format("%s: %s != %s", Claims.upn.name(), upnValue, upn);
        }
        JsonObject result = Json.createObjectBuilder()
                .add("pass", pass)
                .add("msg", msg)
                .build();
        return result;
    }

    @GET
    @Path("/verifyInjectedSUB")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject verifyInjectedSUB(@QueryParam("sub") String sub) {
        boolean pass = false;
        String msg;
        // sUB
        String subValue = this.subject;
        if (subValue == null || subValue.length() == 0) {
            msg = Claims.sub.name() + "value is null or empty, FAIL";
        } else if (subValue.equals(sub)) {
            msg = Claims.sub.name() + " PASS";
            pass = true;
        } else {
            msg = String.format("%s: %s != %s", Claims.sub.name(), subValue, sub);
        }
        JsonObject result = Json.createObjectBuilder()
                .add("pass", pass)
                .add("msg", msg)
                .build();
        return result;
    }

    @GET
    @Path("/verifyInjectedAudience")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject verifyInjectedAudience(@QueryParam("aud") String audience) {
        boolean pass = false;
        String msg;
        // aud
        Set<String> audValue = aud;
        if (audValue == null || audValue.size() == 0) {
            msg = Claims.aud.name() + "value is null or empty, FAIL";
        } else if (audValue.contains(audience)) {
            msg = Claims.aud.name() + " PASS";
            pass = true;
        } else {
            msg = String.format("%s: %s != %s", Claims.aud.name(), audValue, audience);
        }
        JsonObject result = Json.createObjectBuilder()
                .add("pass", pass)
                .add("msg", msg)
                .build();
        return result;
    }

    @GET
    @Path("/verifyInjectedGroups")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject verifyInjectedGroups(@QueryParam("groups") List<String> groups) {
        boolean pass = false;
        String msg;
        // groups
        Set<String> groupsValue = this.groups;
        if (groupsValue == null || groupsValue.size() == 0) {
            msg = Claims.groups.name() + "value is null or empty, FAIL";
        } else if (groupsValue.containsAll(groups)) {
            msg = Claims.groups.name() + " PASS";
            pass = true;
        } else {
            msg = String.format("%s: %s != %s", Claims.groups.name(), groupsValue, groups);
        }
        JsonObject result = Json.createObjectBuilder()
                .add("pass", pass)
                .add("msg", msg)
                .build();
        return result;
    }

    @GET
    @Path("/verifyInjectedIssuedAt")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject verifyInjectedIssuedAt(@QueryParam("iat") Long iat) {
        boolean pass = false;
        String msg;
        // iat
        Long iatValue = this.issuedAt;
        if (iatValue == null || iatValue.intValue() == 0) {
            msg = Claims.iat.name() + "value is null or empty, FAIL";
        } else if (iatValue.equals(iat)) {
            msg = Claims.iat.name() + " PASS";
            pass = true;
        } else {
            msg = String.format("%s: %s != %s", Claims.iat.name(), iatValue, iat);
        }
        JsonObject result = Json.createObjectBuilder()
                .add("pass", pass)
                .add("msg", msg)
                .build();
        return result;
    }

    @GET
    @Path("/verifyInjectedExpiration")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject verifyInjectedExpiration(@QueryParam("exp") Long exp) {
        boolean pass = false;
        String msg;
        // exp
        Long expValue = this.expiration;
        if (expValue == null || expValue.intValue() == 0) {
            msg = Claims.exp.name() + "value is null or empty, FAIL";
        } else if (expValue.equals(exp)) {
            msg = Claims.exp.name() + " PASS";
            pass = true;
        } else {
            msg = String.format("%s: %s != %s", Claims.exp.name(), expValue, exp);
        }
        JsonObject result = Json.createObjectBuilder()
                .add("pass", pass)
                .add("msg", msg)
                .build();
        return result;
    }

    @GET
    @Path("/verifyInjectedCustomString")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject verifyInjectedCustomString(@QueryParam("value") String value) {
        boolean pass = false;
        String msg;
        // iat
        String customValue = this.customString;
        if (customValue == null || customValue.length() == 0) {
            msg = "customString value is null or empty, FAIL";
        } else if (customValue.equals(value)) {
            msg = "customString PASS";
            pass = true;
        } else {
            msg = String.format("customString: %s != %s", customValue, value);
        }
        JsonObject result = Json.createObjectBuilder()
                .add("pass", pass)
                .add("msg", msg)
                .build();
        return result;
    }

    @GET
    @Path("/verifyInjectedCustomDouble")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject verifyInjectedCustomDouble(@QueryParam("value") double value) {
        boolean pass = false;
        String msg;
        if (customDouble == 0) {
            msg = "customString value is not set or empty, FAIL";
        } else if (Math.IEEEremainder(customDouble, value) == 0) {
            msg = "customString PASS";
            pass = true;
        } else {
            msg = String.format("customDouble: %.6f != %.6f", customDouble, value);
        }
        JsonObject result = Json.createObjectBuilder()
                .add("pass", pass)
                .add("msg", msg)
                .build();
        return result;
    }
}
