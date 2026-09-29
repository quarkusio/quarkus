package io.quarkus.produi.runtime;

import java.util.Map;

/**
 * Decides whether a Prod UI page is shown, based on the operator's
 * {@code quarkus.prod-ui.pages.<id>.enabled} configuration.
 * <p>
 * Pages are shown by default; an operator hides one by setting its {@code enabled} flag to {@code false}. This keeps the
 * decision in one pure, testable place so both the built-in pages and extension-contributed pages gate identically.
 */
public final class ProdUIPageVisibility {

    /**
     * The shared namespace used by every built-in Prod UI page (Advisor, Configuration, Endpoints, Loggers,
     * Dependencies). Their JSON-RPC methods all live here and are gated per page at the UI level only.
     */
    public static final String BUILTIN_NAMESPACE = "quarkus-produi";

    private ProdUIPageVisibility() {
    }

    /**
     * @param pageId the page id (built-in {@code configuration}/{@code endpoints}/{@code loggers}/{@code dependencies},
     *        or an extension name such as {@code quarkus-cache})
     * @param enabledByPageId page id to enabled flag, typically derived from {@code quarkus.prod-ui.pages}
     * @return {@code true} unless the page has been explicitly disabled
     */
    public static boolean isVisible(String pageId, Map<String, Boolean> enabledByPageId) {
        if (pageId == null || enabledByPageId == null) {
            return true;
        }
        return enabledByPageId.getOrDefault(pageId, Boolean.TRUE);
    }

    /**
     * Decides whether a JSON-RPC provider namespace's methods should be exposed over the data plane (the
     * {@code json-rpc-ws} websocket), as opposed to merely being shown in the navigation.
     * <p>
     * An extension-contributed provider uses its extension name as the namespace, which maps 1:1 to a page; when that
     * page is hidden the whole namespace is dropped so its {@code <namespace>_*} methods become unreachable, not just
     * hidden from the nav. The {@link #BUILTIN_NAMESPACE built-in namespace} is shared by several built-in pages that
     * are each gated independently, so it is never dropped here - built-in pages are gated at the UI level only.
     *
     * @param namespace the provider namespace (an extension name such as {@code quarkus-cache}, or
     *        {@link #BUILTIN_NAMESPACE} for the built-ins)
     * @param enabledByPageId page id to enabled flag, typically derived from {@code quarkus.prod-ui.pages}
     * @return {@code true} unless the namespace belongs to an explicitly disabled extension page
     */
    public static boolean isNamespaceExposed(String namespace, Map<String, Boolean> enabledByPageId) {
        if (BUILTIN_NAMESPACE.equals(namespace)) {
            return true;
        }
        return isVisible(namespace, enabledByPageId);
    }
}
