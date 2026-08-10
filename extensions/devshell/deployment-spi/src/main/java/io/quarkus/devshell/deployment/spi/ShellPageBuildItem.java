package io.quarkus.devshell.deployment.spi;

import java.lang.invoke.MethodHandle;
import java.util.Map;
import java.util.Optional;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.util.ArtifactInfoUtil;

/**
 * Build item for extensions to register pages in the Dev Shell TUI.
 * This is the CLI equivalent of CardPageBuildItem for Dev UI.
 * <p>
 * Extensions can provide shell pages in two ways:
 * <ol>
 * <li>Implement {@code ShellPageProvider} as a CDI bean (recommended for simple pages)</li>
 * <li>Provide a custom page class name (for advanced customization)</li>
 * </ol>
 * <p>
 * When no explicit id is provided, the extension identifier is auto-detected
 * from the calling class's Maven artifact, similar to Dev UI's CardPageBuildItem.
 */
public final class ShellPageBuildItem extends MultiBuildItem {

    private final String title;
    private final char shortcutKey;
    private final String jsonRpcNamespace;
    private final String providerClassName;
    private final String customPageClassName;
    private final Class<?> customPageClass;

    private final Class<?> callerClass;
    private String extensionIdentifier;

    // ---- Factory methods WITHOUT explicit id (auto-detect from artifact) ----

    public static ShellPageBuildItem withProvider(String title, Class<?> providerClass) {
        return new ShellPageBuildItem(null, title, '\0', null, providerClass.getName(), null, null);
    }

    public static ShellPageBuildItem withProvider(String title, char shortcutKey, Class<?> providerClass) {
        return new ShellPageBuildItem(null, title, shortcutKey, null, providerClass.getName(), null, null);
    }

    public static ShellPageBuildItem withCustomPage(String title, Class<?> pageClass) {
        return new ShellPageBuildItem(null, title, '\0', null, null, pageClass.getName(), pageClass);
    }

    public static ShellPageBuildItem withCustomPage(String title, char shortcutKey, Class<?> pageClass) {
        return new ShellPageBuildItem(null, title, shortcutKey, null, null, pageClass.getName(), pageClass);
    }

    public static ShellPageBuildItem withCustomPage(String title, String pageClassName) {
        return new ShellPageBuildItem(null, title, '\0', null, null, pageClassName, null);
    }

    public static ShellPageBuildItem withCustomPage(String title, char shortcutKey, String pageClassName) {
        return new ShellPageBuildItem(null, title, shortcutKey, null, null, pageClassName, null);
    }

    // ---- Factory methods WITH explicit id ----

    public static ShellPageBuildItem withProvider(String id, String title, Class<?> providerClass) {
        return new ShellPageBuildItem(id, title, '\0', null, providerClass.getName(), null, null);
    }

    public static ShellPageBuildItem withProvider(String id, String title, char shortcutKey, Class<?> providerClass) {
        return new ShellPageBuildItem(id, title, shortcutKey, null, providerClass.getName(), null, null);
    }

    public static ShellPageBuildItem withCustomPage(String id, String title, Class<?> pageClass) {
        return new ShellPageBuildItem(id, title, '\0', null, null, pageClass.getName(), pageClass);
    }

    public static ShellPageBuildItem withCustomPage(String id, String title, char shortcutKey, Class<?> pageClass) {
        return new ShellPageBuildItem(id, title, shortcutKey, null, null, pageClass.getName(), pageClass);
    }

    private ShellPageBuildItem(String id, String title, char shortcutKey, String jsonRpcNamespace,
            String providerClassName, String customPageClassName, Class<?> customPageClass) {
        this.extensionIdentifier = id;
        this.title = title;
        this.shortcutKey = shortcutKey;
        this.jsonRpcNamespace = jsonRpcNamespace;
        this.providerClassName = providerClassName;
        this.customPageClassName = customPageClassName;
        this.customPageClass = customPageClass;

        if (id == null) {
            StackWalker stackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
            Optional<StackWalker.StackFrame> stackFrame = stackWalker.walk(frames -> frames
                    .filter(frame -> (!frame.getDeclaringClass().getPackageName()
                            .startsWith("io.quarkus.devshell.deployment.spi")
                            && !frame.getDeclaringClass().equals(MethodHandle.class)))
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

    public String getId(CurateOutcomeBuildItem curateOutcomeBuildItem) {
        if (this.extensionIdentifier == null && this.callerClass != null) {
            Map.Entry<String, String> groupIdAndArtifactId = ArtifactInfoUtil.groupIdAndArtifactId(callerClass,
                    curateOutcomeBuildItem);
            String artifactId = groupIdAndArtifactId.getValue();
            if (artifactId.endsWith("-deployment")) {
                artifactId = artifactId.substring(0, artifactId.length() - "-deployment".length());
            }
            this.extensionIdentifier = artifactId;
        }
        return this.extensionIdentifier;
    }

    public String getTitle() {
        return title;
    }

    public char getShortcutKey() {
        return shortcutKey;
    }

    public String getJsonRpcNamespace() {
        return jsonRpcNamespace;
    }

    public String getProviderClassName() {
        return providerClassName;
    }

    public String getPageClassName() {
        return customPageClassName;
    }

    public Class<?> getPageClass() {
        return customPageClass;
    }

    public boolean hasProvider() {
        return providerClassName != null && !providerClassName.isEmpty();
    }

    public boolean hasCustomPage() {
        return customPageClassName != null && !customPageClassName.isEmpty();
    }
}
