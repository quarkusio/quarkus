package io.quarkus.resteasy.reactive.server.test.security;

import java.security.Permission;
import java.util.List;
import java.util.Objects;

import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;

import org.junit.jupiter.api.Assertions;

public class SimpleBeanParamPermission extends Permission {

    static final SimpleBeanParamPermission EMPTY = new SimpleBeanParamPermission(null, null, null, null, null, null, null,
            null);

    private final String publicQuery;
    private final String header;
    private final List<String> queryList;
    private final SecurityContext securityContext;
    private final UriInfo uriInfo;
    private final String privateQuery;
    private final String cookie;

    public SimpleBeanParamPermission(String name, String publicQuery, String header, List<String> queryList,
            SecurityContext securityContext, UriInfo uriInfo, String privateQuery, String cookie) {
        super(name);
        this.publicQuery = publicQuery;
        this.header = header;
        this.queryList = queryList;
        this.securityContext = securityContext;
        this.uriInfo = uriInfo;
        this.privateQuery = privateQuery;
        this.cookie = cookie;
    }

    @Override
    public boolean implies(Permission p) {
        if (p instanceof SimpleBeanParamPermission simplePermission) {
            Assertions.assertEquals("perm1", simplePermission.getName());
            Assertions.assertEquals("one-query", simplePermission.privateQuery);
            Assertions.assertEquals("one-query", simplePermission.publicQuery);
            Assertions.assertEquals("one-header", simplePermission.header);
            Assertions.assertEquals("admin", simplePermission.securityContext.getUserPrincipal().getName());
            Assertions.assertNotNull(simplePermission.securityContext);
            Assertions.assertEquals("/simple/param", simplePermission.uriInfo.getPath());
            Assertions.assertEquals("one", simplePermission.queryList.get(0));
            Assertions.assertEquals("two", simplePermission.queryList.get(1));
            Assertions.assertEquals("cookie", simplePermission.cookie);
            return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SimpleBeanParamPermission that = (SimpleBeanParamPermission) o;
        return Objects.equals(publicQuery, that.publicQuery) && Objects.equals(header, that.header)
                && Objects.equals(queryList, that.queryList) && Objects.equals(securityContext, that.securityContext)
                && Objects.equals(uriInfo, that.uriInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(publicQuery, header, queryList, securityContext, uriInfo);
    }

    @Override
    public String getActions() {
        return "";
    }
}
