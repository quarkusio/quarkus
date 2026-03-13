package io.quarkus.devshell.runtime.spi;

import java.util.List;
import java.util.Map;

/**
 * Interface for extensions to provide shell page data.
 * Extensions implement this interface to define what data
 * should be displayed in the Dev Shell TUI.
 * <p>
 * This follows the Dev UI pattern where extension-specific code
 * lives in the extension's runtime-dev module. The provider is
 * a CDI bean that gets discovered and invoked by DevShell.
 * <p>
 * Example usage in an extension's runtime-dev module:
 *
 * <pre>
 * &#64;ApplicationScoped
 * public class MyExtensionShellProvider implements ShellPageProvider {
 *     &#64;Inject
 *     MyService service;
 *
 *     &#64;Override
 *     public ShellPageData loadData() {
 *         return new ShellPageData.Builder()
 *                 .addSection("Status", List.of(
 *                         new ShellPageData.Item("Active", String.valueOf(service.isActive()), ItemStyle.STATUS_OK)))
 *                 .build();
 *     }
 * }
 * </pre>
 */
public interface ShellPageProvider {

    /**
     * Load the data for this shell page.
     * This method is called when the page is displayed or refreshed.
     *
     * @return the page data to display
     */
    ShellPageData loadData();

    /**
     * Get available actions for this page.
     * Actions are displayed in the footer and can be triggered by keyboard shortcuts.
     *
     * @return list of actions the user can trigger
     */
    default List<PageAction> getActions() {
        return List.of();
    }

    /**
     * Execute an action by name.
     *
     * @param actionName the action to execute
     * @param params optional parameters for the action
     * @return result message or null
     */
    default String executeAction(String actionName, Map<String, Object> params) {
        return null;
    }

    /**
     * Item styling hints for the TUI renderer.
     */
    enum ItemStyle {
        /** Plain text */
        TEXT,
        /** Code/monospace */
        CODE,
        /** Success/green indicator */
        STATUS_OK,
        /** Warning/yellow indicator */
        STATUS_WARNING,
        /** Error/red indicator */
        STATUS_ERROR,
        /** Clickable link style */
        LINK,
        /** Header/title style */
        HEADER
    }

    /**
     * An action that can be triggered by the user.
     */
    record PageAction(
            String name,
            String label,
            char shortcutKey) {

        public PageAction(String name, char shortcutKey) {
            this(name, name, shortcutKey);
        }
    }
}
