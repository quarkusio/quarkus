package io.quarkus.devconsole.spi;

import java.util.Map;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.util.ArtifactInfoUtil;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * A route for handling requests in the dev console.
 *
 * Routes are registered under /@dev/{groupId}.{artifactId}/
 * 
 * This handler executes in the deployment class loader.
 *
 */
public final class DevConsoleRouteBuildItem extends MultiBuildItem {

    private final String groupId;
    private final String artifactId;
    private final String path;
    private final String method;
    private final Handler<RoutingContext> handler;

    public DevConsoleRouteBuildItem(String groupId, String artifactId, String path, String method,
            Handler<RoutingContext> handler) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.path = path;
        this.method = method;
        this.handler = handler;
    }

    public DevConsoleRouteBuildItem(String path, String method,
            Handler<RoutingContext> handler) {
        String callerClassName = new RuntimeException().getStackTrace()[1].getClassName();
        Class<?> callerClass = null;
        try {
            callerClass = Thread.currentThread().getContextClassLoader().loadClass(callerClassName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        Map.Entry<String, String> info = ArtifactInfoUtil.groupIdAndArtifactId(callerClass);
        this.groupId = info.getKey();
        this.artifactId = info.getValue();
        this.path = path;
        this.method = method;
        this.handler = handler;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getPath() {
        return path;
    }

    public String getMethod() {
        return method;
    }

    public Handler<RoutingContext> getHandler() {
        return handler;
    }

}
