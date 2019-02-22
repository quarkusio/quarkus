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

package io.quarkus.undertow.runtime;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.undertow.servlet.api.SecurityInfo;
import io.undertow.servlet.api.TransportGuaranteeType;

public class ServletSecurityInfoProxy {

    private final List<ServletSecurityInfoProxy> httpMethodSecurityInfo = new ArrayList<>();
    private volatile SecurityInfo.EmptyRoleSemantic emptyRoleSemantic = SecurityInfo.EmptyRoleSemantic.DENY;
    private final Set<String> rolesAllowed = new HashSet<>();
    private volatile TransportGuaranteeType transportGuaranteeType = TransportGuaranteeType.NONE;

    public List<ServletSecurityInfoProxy> getHttpMethodSecurityInfo() {
        return httpMethodSecurityInfo;
    }

    public SecurityInfo.EmptyRoleSemantic getEmptyRoleSemantic() {
        return emptyRoleSemantic;
    }

    public ServletSecurityInfoProxy setEmptyRoleSemantic(SecurityInfo.EmptyRoleSemantic emptyRoleSemantic) {
        this.emptyRoleSemantic = emptyRoleSemantic;
        return this;
    }

    public Set<String> getRolesAllowed() {
        return rolesAllowed;
    }

    public TransportGuaranteeType getTransportGuaranteeType() {
        return transportGuaranteeType;
    }

    public ServletSecurityInfoProxy setTransportGuaranteeType(TransportGuaranteeType transportGuaranteeType) {
        this.transportGuaranteeType = transportGuaranteeType;
        return this;
    }
}
