package io.quarkus.devui.spi;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.util.ArtifactInfoUtil;

/**
 * For All DEV UI Build Item, we need to distinguish between the extensions, and the internal usage of Dev UI
 */
public abstract class AbstractDevUIBuildItem extends MultiBuildItem {

    private final Class<?> callerClass;
    private String extensionIdentifier = null;

    private static final String DOT = ".";
    private final String customIdentifier;

    public AbstractDevUIBuildItem() {
        this(null);
    }

    public AbstractDevUIBuildItem(String customIdentifier) {
        this.customIdentifier = customIdentifier;

        if (this.customIdentifier == null) {
            // Get the class that will be used to auto-detect the name
            StackWalker stackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

            List<StackWalker.StackFrame> stackFrames = stackWalker.walk(frames -> frames.collect(Collectors.toList()));

            Optional<StackWalker.StackFrame> stackFrame = stackWalker.walk(frames -> frames
                    .filter(frame -> (!frame.getDeclaringClass().getPackageName().startsWith("io.quarkus.devui.spi")
                            && !frame.getDeclaringClass().getPackageName().startsWith("io.quarkus.devui.deployment")))
                    .findFirst());

            if (stackFrame.isPresent()) {
                this.callerClass = stackFrame.get().getDeclaringClass();
            } else {
                throw new RuntimeException("Could not detect extension identifier automatically");
            }
        } else {
            this.callerClass = null;
        }
    }

    public String getExtensionPathName(CurateOutcomeBuildItem curateOutcomeBuildItem) {
        if (this.customIdentifier != null) {
            return customIdentifier;
        }
        if (this.callerClass == null) {
            return DEV_UI;
        }

        if (this.extensionIdentifier == null) {

            Map.Entry<String, String> groupIdAndArtifactId = ArtifactInfoUtil.groupIdAndArtifactId(callerClass,
                    curateOutcomeBuildItem);
            this.extensionIdentifier = groupIdAndArtifactId.getKey() + DOT + groupIdAndArtifactId.getValue();
        }

        return this.extensionIdentifier;
    }

    public boolean isInternal() {
        return this.customIdentifier != null;
    }

    public static final String DEV_UI = "devui";
}
