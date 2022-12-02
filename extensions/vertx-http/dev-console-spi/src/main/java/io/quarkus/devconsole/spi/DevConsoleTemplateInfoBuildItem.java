package io.quarkus.devconsole.spi;

import java.util.Map;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.util.ArtifactInfoUtil;

/**
 * Information that can be directly displayed in dev console templates, using the info: prefix
 * <p>
 * This is scoped to the extension that produced it, to prevent namespace clashes
 */
public final class DevConsoleTemplateInfoBuildItem extends MultiBuildItem {

    private final String name;
    private final Object object;
    private final Class<?> callerClass;

    public DevConsoleTemplateInfoBuildItem(String name, Object object) {
        String callerClassName = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass()
                .getCanonicalName();
        try {
            callerClass = Thread.currentThread().getContextClassLoader().loadClass(callerClassName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        this.name = name;
        this.object = object;
    }

    /**
     * Gets the group id and artifact ID. This needs the curate result to map the calling class to the
     * artifact that contains it in some situations (namely in dev mode tests).
     */
    public Map.Entry<String, String> groupIdAndArtifactId(CurateOutcomeBuildItem curateOutcomeBuildItem) {
        return ArtifactInfoUtil.groupIdAndArtifactId(callerClass, curateOutcomeBuildItem);
    }

    public String getName() {
        return name;
    }

    public Object getObject() {
        return object;
    }
}
