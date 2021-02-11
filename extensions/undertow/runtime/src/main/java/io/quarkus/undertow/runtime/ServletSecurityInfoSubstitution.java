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
            ns.setMethod(i.getMethod());
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
            ns.setMethod(i.getMethod());
            sub.addHttpMethodSecurityInfo(ns);
        }
        return sub;
    }
}
