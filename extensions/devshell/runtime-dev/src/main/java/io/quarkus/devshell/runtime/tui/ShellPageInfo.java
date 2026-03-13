package io.quarkus.devshell.runtime.tui;

/**
 * Runtime representation of shell page configuration.
 * This is passed from build time to runtime.
 */
public record ShellPageInfo(
        String id,
        String title,
        char shortcutKey,
        String jsonRpcNamespace,
        String providerClassName,
        String customPageClassName) {

    /**
     * Check if this page uses a ShellPageProvider.
     */
    public boolean hasProvider() {
        return providerClassName != null && !providerClassName.isEmpty();
    }

    /**
     * Check if this page uses a custom ExtensionPage implementation.
     */
    public boolean hasCustomPage() {
        return customPageClassName != null && !customPageClassName.isEmpty();
    }
}
