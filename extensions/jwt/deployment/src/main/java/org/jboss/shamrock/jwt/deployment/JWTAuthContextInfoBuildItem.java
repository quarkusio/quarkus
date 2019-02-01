package org.jboss.shamrock.jwt.deployment;

import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import org.jboss.builder.item.SimpleBuildItem;

public final class JWTAuthContextInfoBuildItem extends SimpleBuildItem {
    private final JWTAuthContextInfo jwtAuthContextInfo;

    public JWTAuthContextInfoBuildItem(JWTAuthContextInfo jwtAuthContextInfo) {
        this.jwtAuthContextInfo = jwtAuthContextInfo;
    }

    public JWTAuthContextInfo getJwtAuthContextInfo() {
        return jwtAuthContextInfo;
    }
}
