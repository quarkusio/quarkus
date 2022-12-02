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
    private String method;

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

    public String getMethod() {
        return method;
    }

    public ServletSecurityInfoProxy setMethod(String method) {
        this.method = method;
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
