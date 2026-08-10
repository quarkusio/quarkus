package io.quarkus.devshell.deployment.spi;

import java.util.List;
import java.util.Map;

/**
 * Interface for extensions to provide shell page data.
 * Extensions implement this as a CDI bean to define what data
 * should be displayed in the Dev Shell TUI.
 * <p>
 * Example:
 *
 * <pre>
 * &#64;ApplicationScoped
 * public class MyExtensionShellProvider implements ShellPageProvider {
 *     &#64;Inject
 *     MyService service;
 *
 *     &#64;Override
 *     public ShellPageData loadData() {
 *         return ShellPageData.builder()
 *                 .addSection("Status", List.of(
 *                         ShellPageData.Item.ok("Active", String.valueOf(service.isActive()))))
 *                 .build();
 *     }
 * }
 * </pre>
 */
public interface ShellPageProvider {

    ShellPageData loadData();

    default List<PageAction> getActions() {
        return List.of();
    }

    default String executeAction(String actionName, Map<String, Object> params) {
        return null;
    }

    enum ItemStyle {
        TEXT,
        CODE,
        STATUS_OK,
        STATUS_WARNING,
        STATUS_ERROR,
        LINK,
        HEADER
    }

    record PageAction(String name, String label, char shortcutKey) {
        public PageAction(String name, char shortcutKey) {
            this(name, name, shortcutKey);
        }
    }
}
