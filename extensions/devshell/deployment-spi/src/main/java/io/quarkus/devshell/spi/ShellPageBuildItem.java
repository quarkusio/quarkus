package io.quarkus.devshell.spi;

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
 * <li>Implement {@code ShellPageProvider} as a CDI bean (recommended)</li>
 * <li>Provide a custom page class name (for advanced customization)</li>
 * </ol>
 * <p>
 * When no explicit id is provided, the extension identifier is auto-detected
 * from the calling class's Maven artifact, similar to Dev UI's CardPageBuildItem.
 *
 * @see io.quarkus.devshell.runtime.spi.ShellPageProvider
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

    /**
     * Create a shell page using a ShellPageProvider CDI bean.
     * The extension identifier is auto-detected from the calling extension's artifact.
     *
     * @param title display title in the navigation bar
     * @param providerClass the ShellPageProvider implementation class (must be a CDI bean)
     */
    public static ShellPageBuildItem withProvider(String title, Class<?> providerClass) {
        return new ShellPageBuildItem(null, title, '\0', null, providerClass.getName(), null, null);
    }

    /**
     * Create a shell page using a ShellPageProvider CDI bean with a shortcut key.
     * The extension identifier is auto-detected from the calling extension's artifact.
     *
     * @param title display title in the navigation bar
     * @param shortcutKey keyboard shortcut to access this page
     * @param providerClass the ShellPageProvider implementation class (must be a CDI bean)
     */
    public static ShellPageBuildItem withProvider(String title, char shortcutKey, Class<?> providerClass) {
        return new ShellPageBuildItem(null, title, shortcutKey, null, providerClass.getName(), null, null);
    }

    /**
     * Create a shell page using a custom ExtensionPage implementation.
     * The extension identifier is auto-detected from the calling extension's artifact.
     *
     * @param title display title in the navigation bar
     * @param pageClass the ExtensionPage implementation class
     */
    public static ShellPageBuildItem withCustomPage(String title, Class<?> pageClass) {
        return new ShellPageBuildItem(null, title, '\0', null, null, pageClass.getName(), pageClass);
    }

    /**
     * Create a shell page using a custom ExtensionPage implementation with a shortcut key.
     * The extension identifier is auto-detected from the calling extension's artifact.
     *
     * @param title display title in the navigation bar
     * @param shortcutKey keyboard shortcut to access this page
     * @param pageClass the ExtensionPage implementation class
     */
    public static ShellPageBuildItem withCustomPage(String title, char shortcutKey, Class<?> pageClass) {
        return new ShellPageBuildItem(null, title, shortcutKey, null, null, pageClass.getName(), pageClass);
    }

    /**
     * Create a shell page using a custom ExtensionPage implementation specified by class name.
     * The extension identifier is auto-detected from the calling extension's artifact.
     * Use this when the page class is in runtime-dev and cannot be directly referenced from deployment.
     *
     * @param title display title in the navigation bar
     * @param pageClassName fully qualified class name of the ExtensionPage implementation
     */
    public static ShellPageBuildItem withCustomPage(String title, String pageClassName) {
        return new ShellPageBuildItem(null, title, '\0', null, null, pageClassName, null);
    }

    /**
     * Create a shell page using a custom ExtensionPage implementation specified by class name with a shortcut key.
     * The extension identifier is auto-detected from the calling extension's artifact.
     * Use this when the page class is in runtime-dev and cannot be directly referenced from deployment.
     *
     * @param title display title in the navigation bar
     * @param shortcutKey keyboard shortcut to access this page
     * @param pageClassName fully qualified class name of the ExtensionPage implementation
     */
    public static ShellPageBuildItem withCustomPage(String title, char shortcutKey, String pageClassName) {
        return new ShellPageBuildItem(null, title, shortcutKey, null, null, pageClassName, null);
    }

    // ---- Factory methods WITH explicit id (for internal pages or custom namespaces) ----

    /**
     * Create a shell page using a ShellPageProvider CDI bean.
     *
     * @param id unique identifier for the page
     * @param title display title in the navigation bar
     * @param providerClass the ShellPageProvider implementation class (must be a CDI bean)
     */
    public static ShellPageBuildItem withProvider(String id, String title, Class<?> providerClass) {
        return new ShellPageBuildItem(id, title, '\0', null, providerClass.getName(), null, null);
    }

    /**
     * Create a shell page using a ShellPageProvider CDI bean with a shortcut key.
     *
     * @param id unique identifier for the page
     * @param title display title in the navigation bar
     * @param shortcutKey keyboard shortcut to access this page
     * @param providerClass the ShellPageProvider implementation class (must be a CDI bean)
     */
    public static ShellPageBuildItem withProvider(String id, String title, char shortcutKey, Class<?> providerClass) {
        return new ShellPageBuildItem(id, title, shortcutKey, null, providerClass.getName(), null, null);
    }

    /**
     * Create a shell page using a custom ExtensionPage implementation.
     *
     * @param id unique identifier for the page
     * @param title display title in the navigation bar
     * @param pageClass the ExtensionPage implementation class
     */
    public static ShellPageBuildItem withCustomPage(String id, String title, Class<?> pageClass) {
        return new ShellPageBuildItem(id, title, '\0', null, null, pageClass.getName(), pageClass);
    }

    /**
     * Create a shell page using a custom ExtensionPage implementation with a shortcut key.
     *
     * @param id unique identifier for the page
     * @param title display title in the navigation bar
     * @param shortcutKey keyboard shortcut to access this page
     * @param pageClass the ExtensionPage implementation class
     */
    public static ShellPageBuildItem withCustomPage(String id, String title, char shortcutKey, Class<?> pageClass) {
        return new ShellPageBuildItem(id, title, shortcutKey, null, null, pageClass.getName(), pageClass);
    }

    /**
     * Create a shell page using a custom ExtensionPage implementation specified by class name.
     * Use this when the page class is in runtime-dev and cannot be directly referenced from deployment.
     *
     * @param id unique identifier for the page
     * @param title display title in the navigation bar
     * @param pageClassName fully qualified class name of the ExtensionPage implementation
     */
    public static ShellPageBuildItem withCustomPage(String id, String title, String pageClassName) {
        return new ShellPageBuildItem(id, title, '\0', null, null, pageClassName, null);
    }

    /**
     * Create a shell page using a custom ExtensionPage implementation specified by class name with a shortcut key.
     * Use this when the page class is in runtime-dev and cannot be directly referenced from deployment.
     *
     * @param id unique identifier for the page
     * @param title display title in the navigation bar
     * @param shortcutKey keyboard shortcut to access this page
     * @param pageClassName fully qualified class name of the ExtensionPage implementation
     */
    public static ShellPageBuildItem withCustomPage(String id, String title, char shortcutKey, String pageClassName) {
        return new ShellPageBuildItem(id, title, shortcutKey, null, null, pageClassName, null);
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
                    .filter(frame -> (!frame.getDeclaringClass().getPackageName().startsWith("io.quarkus.devshell.spi")
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

    /**
     * Get the unique identifier for this page.
     * If no explicit id was provided, this lazily resolves it from the caller's
     * Maven artifact, stripping the {@code quarkus-} prefix and {@code -deployment} suffix.
     *
     * @param curateOutcomeBuildItem the curate outcome for artifact resolution
     */
    public String getId(CurateOutcomeBuildItem curateOutcomeBuildItem) {
        if (this.extensionIdentifier == null && this.callerClass != null) {
            Map.Entry<String, String> groupIdAndArtifactId = ArtifactInfoUtil.groupIdAndArtifactId(callerClass,
                    curateOutcomeBuildItem);
            String artifactId = groupIdAndArtifactId.getValue();
            if (artifactId.startsWith("quarkus-")) {
                artifactId = artifactId.substring("quarkus-".length());
            }
            if (artifactId.endsWith("-deployment")) {
                artifactId = artifactId.substring(0, artifactId.length() - "-deployment".length());
            }
            this.extensionIdentifier = artifactId;
        }
        return this.extensionIdentifier;
    }

    /**
     * Get the display title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Get the keyboard shortcut key.
     */
    public char getShortcutKey() {
        return shortcutKey;
    }

    /**
     * Get the JSON-RPC namespace for this extension.
     */
    public String getJsonRpcNamespace() {
        return jsonRpcNamespace;
    }

    /**
     * Get the ShellPageProvider class name, or null if not using a provider.
     */
    public String getProviderClassName() {
        return providerClassName;
    }

    /**
     * Get the custom page class name, or null if using default rendering.
     */
    public String getPageClassName() {
        return customPageClassName;
    }

    /**
     * Get the custom page Class object, or null if created from a class name string.
     */
    public Class<?> getPageClass() {
        return customPageClass;
    }

    /**
     * Check if this page uses a ShellPageProvider.
     */
    public boolean hasProvider() {
        return providerClassName != null && !providerClassName.isEmpty();
    }

    /**
     * Check if this page has a custom implementation.
     */
    public boolean hasCustomPage() {
        return customPageClassName != null && !customPageClassName.isEmpty();
    }
}
