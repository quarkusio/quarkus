package io.quarkus.devui.spi;

import java.lang.invoke.MethodHandle;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.util.ArtifactInfoUtil;
import io.quarkus.maven.dependency.ArtifactKey;

/**
 * For All DEV UI Build Item, we need to distinguish between the extensions, and the internal usage of Dev UI
 */
public abstract class AbstractDevUIBuildItem extends MultiBuildItem {

    private final Class<?> callerClass;
    private String extensionIdentifier = null;
    private ArtifactKey artifactKey;
    private final boolean isInternal;

    public AbstractDevUIBuildItem() {
        this(null);
    }

    public AbstractDevUIBuildItem(String customIdentifier) {
        this.extensionIdentifier = customIdentifier;
        isInternal = customIdentifier == null;
        if (customIdentifier == null) {
            // Get the class that will be used to auto-detect the name
            StackWalker stackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

            stackWalker.walk(frames -> frames.collect(Collectors.toList()));

            Optional<StackWalker.StackFrame> stackFrame = stackWalker.walk(frames -> frames
                    .filter(frame -> (!frame.getDeclaringClass().getPackageName().startsWith("io.quarkus.devui.spi")
                            && !frame.getDeclaringClass().getPackageName().startsWith("io.quarkus.devui.deployment")
                            && !frame.getDeclaringClass().equals(MethodHandle.class)))
                    .findFirst());

            if (stackFrame.isPresent()) {
                this.callerClass = stackFrame.get().getDeclaringClass();
                if (this.callerClass == null)
                    this.extensionIdentifier = DEV_UI;
            } else {
                throw new RuntimeException("Could not detect extension identifier automatically");
            }
        } else {
            this.callerClass = null;
        }
    }

    public ArtifactKey getArtifactKey(CurateOutcomeBuildItem curateOutcomeBuildItem) {
        if (this.artifactKey == null) {
            if (callerClass != null) {
                Map.Entry<String, String> groupIdAndArtifactId = ArtifactInfoUtil.groupIdAndArtifactId(callerClass,
                        curateOutcomeBuildItem);
                this.artifactKey = ArtifactKey.ga(groupIdAndArtifactId.getKey(), groupIdAndArtifactId.getValue());
            }
        }
        return this.artifactKey;
    }

    public String getExtensionPathName(CurateOutcomeBuildItem curateOutcomeBuildItem) {
        if (this.extensionIdentifier == null) {
            ArtifactKey ak = getArtifactKey(curateOutcomeBuildItem);
            this.extensionIdentifier = ak.getArtifactId();
        }

        return this.extensionIdentifier;
    }

    public boolean isInternal() {
        return this.isInternal;
    }

    public static final String DEV_UI = "devui";
}
