package io.quarkus.devconsole.spi;

import java.util.AbstractMap;
import java.util.Map;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.recording.BytecodeRecorderImpl;
import io.quarkus.deployment.util.ArtifactInfoUtil;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * A route for handling requests in the dev console.
 * <p>
 * Routes are registered under /q/dev/{groupId}.{artifactId}/
 * <p>
 * The route is registered:
 * <ul>
 * <li>in the "regular" app router (runtime class loader), if the handler is produced by a recorder (i.e. implements
 * {@link io.quarkus.deployment.recording.BytecodeRecorderImpl.ReturnedProxy}),</li>
 * <li>in the Dev UI router (deployment class loader).</li>
 */
public final class DevConsoleRouteBuildItem extends MultiBuildItem {

    private final String groupId;
    private final String artifactId;
    private final String path;
    private final String method;
    private final Class<?> callerClass;
    private final Handler<RoutingContext> handler;
    private final boolean isBodyHandlerRequired;

    public DevConsoleRouteBuildItem(String groupId, String artifactId, String path, String method,
            Handler<RoutingContext> handler) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.path = path;
        this.method = method;
        this.handler = handler;
        this.callerClass = null;
        this.isBodyHandlerRequired = false;
    }

    public DevConsoleRouteBuildItem(String path, String method,
            Handler<RoutingContext> handler) {
        // we cannot use this() because the caller detection would not work
        String callerClassName = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass()
                .getCanonicalName();
        try {
            callerClass = Thread.currentThread().getContextClassLoader().loadClass(callerClassName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        this.groupId = null;
        this.artifactId = null;
        this.path = path;
        this.method = method;
        this.handler = handler;
        this.isBodyHandlerRequired = false;
    }

    public DevConsoleRouteBuildItem(String path, String method,
            Handler<RoutingContext> handler, boolean isBodyHandlerRequired) {
        String callerClassName = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass()
                .getCanonicalName();
        try {
            callerClass = Thread.currentThread().getContextClassLoader().loadClass(callerClassName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        this.groupId = null;
        this.artifactId = null;
        this.path = path;
        this.method = method;
        this.handler = handler;
        this.isBodyHandlerRequired = isBodyHandlerRequired;
    }

    /**
     * Gets the group id and artifact ID. This needs the curate result to map the calling class to the
     * artifact that contains it in some situations (namely in dev mode tests).
     */
    public Map.Entry<String, String> groupIdAndArtifactId(CurateOutcomeBuildItem curateOutcomeBuildItem) {
        if (callerClass == null) {
            return new AbstractMap.SimpleEntry<>(groupId, artifactId);
        } else {
            return ArtifactInfoUtil.groupIdAndArtifactId(callerClass, curateOutcomeBuildItem);
        }
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

    public boolean isDeploymentSide() {
        return !(handler instanceof BytecodeRecorderImpl.ReturnedProxy);
    }

    public boolean isBodyHandlerRequired() {
        return isBodyHandlerRequired;
    }

}
