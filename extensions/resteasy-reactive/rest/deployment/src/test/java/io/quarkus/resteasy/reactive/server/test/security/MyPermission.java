package io.quarkus.resteasy.reactive.server.test.security;

import java.security.Permission;
import java.util.Objects;

public class MyPermission extends Permission {

    static final MyPermission EMPTY = new MyPermission("my-perm", null, null);

    private final String authorization;
    private final String queryParam;

    public MyPermission(String permissionName, String authorization, String queryParam) {
        super(permissionName);
        this.authorization = authorization;
        this.queryParam = queryParam;
    }

    @Override
    public boolean implies(Permission permission) {
        if (permission instanceof MyPermission myPermission) {
            return myPermission.authorization != null && "query1".equals(myPermission.queryParam);
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MyPermission that = (MyPermission) o;
        return Objects.equals(authorization, that.authorization)
                && Objects.equals(queryParam, that.queryParam);
    }

    @Override
    public int hashCode() {
        return Objects.hash(authorization, queryParam);
    }

    @Override
    public String getActions() {
        return "";
    }
}
