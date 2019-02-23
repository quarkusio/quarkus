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

import io.quarkus.runtime.ObjectSubstitution;
import io.undertow.servlet.api.HttpMethodSecurityInfo;
import io.undertow.servlet.api.ServletSecurityInfo;

public class ServletSecurityInfoSubstitution implements ObjectSubstitution<ServletSecurityInfo, ServletSecurityInfoProxy> {
    @Override
    public ServletSecurityInfoProxy serialize(ServletSecurityInfo obj) {
        ServletSecurityInfoProxy sub = new ServletSecurityInfoProxy();
        sub.setEmptyRoleSemantic(obj.getEmptyRoleSemantic());
        sub.setTransportGuaranteeType(obj.getTransportGuaranteeType());
        sub.getRolesAllowed().addAll(obj.getRolesAllowed());

        for (HttpMethodSecurityInfo i : obj.getHttpMethodSecurityInfo()) {
            ServletSecurityInfoProxy ns = new ServletSecurityInfoProxy();
            ns.setTransportGuaranteeType(i.getTransportGuaranteeType());
            ns.setEmptyRoleSemantic(i.getEmptyRoleSemantic());
            ns.getRolesAllowed().addAll(i.getRolesAllowed());
            sub.getHttpMethodSecurityInfo().add(ns);
        }
        return sub;
    }

    @Override
    public ServletSecurityInfo deserialize(ServletSecurityInfoProxy obj) {
        ServletSecurityInfo sub = new ServletSecurityInfo();
        sub.setEmptyRoleSemantic(obj.getEmptyRoleSemantic());
        sub.setTransportGuaranteeType(obj.getTransportGuaranteeType());
        sub.addRolesAllowed(obj.getRolesAllowed());

        for (ServletSecurityInfoProxy i : obj.getHttpMethodSecurityInfo()) {
            HttpMethodSecurityInfo ns = new HttpMethodSecurityInfo();
            ns.setTransportGuaranteeType(i.getTransportGuaranteeType());
            ns.setEmptyRoleSemantic(i.getEmptyRoleSemantic());
            ns.addRolesAllowed(i.getRolesAllowed());
            sub.getHttpMethodSecurityInfo().add(ns);
        }
        return sub;
    }
}
