package io.quarkus.devconsole.spi;

import java.util.Map;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.util.ArtifactInfoUtil;

/**
 * Information that can be directly displayed in dev console templates, using the info: prefix
 *
 * This is scoped to the extension that produced it, to prevent namespace clashes
 */
public final class DevConsoleTemplateInfoBuildItem extends MultiBuildItem {

    private final String groupId;
    private final String artifactId;
    private final String name;
    private final Object object;

    public DevConsoleTemplateInfoBuildItem(String groupId, String artifactId, String name, Object object) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.name = name;
        this.object = object;
    }

    public DevConsoleTemplateInfoBuildItem(String name, Object object) {
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

    public Object getObject() {
        return object;
    }
}
