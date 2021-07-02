package io.quarkus.devconsole.spi;

import java.util.Map;
import java.util.function.Supplier;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.util.ArtifactInfoUtil;

/**
 * Information that can be directly displayed in dev console templates, using the info: prefix
 *
 * This is scoped to the extension that produced it, to prevent namespace clashes.
 *
 * This value will be evaluated at runtime, so can contain info that is produced from recorders
 */
public final class DevConsoleRuntimeTemplateInfoBuildItem extends MultiBuildItem {

    private final String groupId;
    private final String artifactId;
    private final String name;
    private final Supplier<? extends Object> object;

    public DevConsoleRuntimeTemplateInfoBuildItem(String groupId, String artifactId, String name,
            Supplier<? extends Object> object) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.name = name;
        this.object = object;
    }

    public DevConsoleRuntimeTemplateInfoBuildItem(String name, Supplier<? extends Object> object) {
        String callerClassName = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass()
                .getCanonicalName();
        Class<?> callerClass = null;
        try {
            callerClass = Thread.currentThread().getContextClassLoader().loadClass(callerClassName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        Map.Entry<String, String> info = ArtifactInfoUtil.groupIdAndArtifactId(callerClass);
        this.groupId = info.getKey();
        this.artifactId = info.getValue();
        this.name = name;
        this.object = object;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getName() {
        return name;
    }

    public Supplier<? extends Object> getObject() {
        return object;
    }
}
