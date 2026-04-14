package io.quarkus.devshell.runtime.tui.widgets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.style.Style;
import io.quarkus.devshell.runtime.tui.KeyCode;

/**
 * A tree view widget for displaying hierarchical data.
 *
 * @param <T> the type of data items
 */
public class TreeView<T> {

    private final List<TreeNode<T>> rootNodes = new ArrayList<>();
    private final Map<String, TreeNode<T>> nodeMap = new HashMap<>();
    private final List<TreeNode<T>> flattenedNodes = new ArrayList<>();

    private final Function<T, String> labelExtractor;
    private final Function<T, String> pathExtractor;

    private int selectedIndex = 0;
    private int scrollOffset = 0;
    private int visibleRows = 20;
    private int width = 60;

    // Tree drawing characters
    private static final String FOLDER_OPEN = "\u25BC "; // open triangle
    private static final String FOLDER_CLOSED = "\u25B6 "; // closed triangle
    private static final String FILE_ICON = "  ";

    // Cached styles
    private static final Style STYLE_SELECTED = Style.create().white().onBlue();
    private static final Style STYLE_DIRECTORY = Style.create().cyan().bold();
    private static final Style STYLE_GRAY = Style.create().gray();
    private static final Style STYLE_JAVA = Style.create().green();
    private static final Style STYLE_CONFIG = Style.create().yellow();
    private static final Style STYLE_PROPERTIES = Style.create().magenta();
    private static final Style STYLE_WEB = Style.create().cyan();
    private static final Style STYLE_DEFAULT = Style.create().white();

    public TreeView(Function<T, String> labelExtractor, Function<T, String> pathExtractor) {
        this.labelExtractor = labelExtractor;
        this.pathExtractor = pathExtractor;
    }

    /**
     * Build tree from a list of items with paths.
     * Paths should use '/' as separator.
     */
    public void setItems(List<T> items) {
        rootNodes.clear();
        nodeMap.clear();

        for (T item : items) {
            String path = pathExtractor.apply(item);
            addToTree(path, item);
        }

        // Sort nodes
        sortNodes(rootNodes);

        // Flatten for display
        rebuildFlatList();
    }

    private void addToTree(String path, T item) {
        String[] parts = path.split("/");
        List<TreeNode<T>> currentLevel = rootNodes;
        StringBuilder currentPath = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (currentPath.length() > 0) {
                currentPath.append("/");
            }
            currentPath.append(part);

            String pathKey = currentPath.toString();
            TreeNode<T> node = nodeMap.get(pathKey);

            if (node == null) {
                boolean isLeaf = (i == parts.length - 1);
                node = new TreeNode<>(part, pathKey, isLeaf ? item : null, !isLeaf);
                nodeMap.put(pathKey, node);
                currentLevel.add(node);
            }

            currentLevel = node.children;
        }
    }

    private void sortNodes(List<TreeNode<T>> nodes) {
        // Sort: directories first, then alphabetically
        nodes.sort((a, b) -> {
            if (a.isDirectory && !b.isDirectory)
                return -1;
            if (!a.isDirectory && b.isDirectory)
                return 1;
            return a.name.compareToIgnoreCase(b.name);
        });

        for (TreeNode<T> node : nodes) {
            if (!node.children.isEmpty()) {
                sortNodes(node.children);
            }
        }
    }

    private void rebuildFlatList() {
        flattenedNodes.clear();
        flattenNodes(rootNodes, 0);

        if (selectedIndex >= flattenedNodes.size()) {
            selectedIndex = Math.max(0, flattenedNodes.size() - 1);
        }
        adjustScroll();
    }

    private void flattenNodes(List<TreeNode<T>> nodes, int depth) {
        for (TreeNode<T> node : nodes) {
            node.depth = depth;
            flattenedNodes.add(node);

            if (node.isDirectory && node.expanded && !node.children.isEmpty()) {
                flattenNodes(node.children, depth + 1);
            }
        }
    }

    public void setVisibleRows(int rows) {
        this.visibleRows = Math.max(1, rows);
        adjustScroll();
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public TreeNode<T> getSelectedNode() {
        if (flattenedNodes.isEmpty() || selectedIndex < 0 || selectedIndex >= flattenedNodes.size()) {
            return null;
        }
        return flattenedNodes.get(selectedIndex);
    }

    public T getSelectedItem() {
        TreeNode<T> node = getSelectedNode();
        return node != null ? node.data : null;
    }

    /**
     * Render the tree view into the buffer.
     *
     * @param buffer the buffer to render into
     * @param startRow starting row (0-based)
     * @param startCol starting column (0-based)
     */
    public void render(Buffer buffer, int startRow, int startCol) {
        if (flattenedNodes.isEmpty()) {
            buffer.setString(startCol, startRow, "(empty)", STYLE_GRAY);
            return;
        }

        int endIdx = Math.min(scrollOffset + visibleRows, flattenedNodes.size());

        for (int i = scrollOffset; i < endIdx; i++) {
            TreeNode<T> node = flattenedNodes.get(i);
            int row = startRow + (i - scrollOffset);

            boolean selected = (i == selectedIndex);
            Style nameStyle;

            // Build the line
            StringBuilder line = new StringBuilder();

            // Indentation
            line.append("  ".repeat(node.depth));

            // Icon
            if (node.isDirectory) {
                line.append(node.expanded ? FOLDER_OPEN : FOLDER_CLOSED);
            } else {
                line.append(FILE_ICON);
            }

            // Name
            line.append(node.name);

            // Determine style
            if (selected) {
                nameStyle = STYLE_SELECTED;
            } else if (node.isDirectory) {
                nameStyle = STYLE_DIRECTORY;
            } else {
                nameStyle = getFileStyle(node.name);
            }

            // Pad to width for selection highlight
            String lineStr = line.toString();
            if (lineStr.length() < width) {
                lineStr = lineStr + " ".repeat(width - lineStr.length());
            } else if (lineStr.length() > width) {
                lineStr = lineStr.substring(0, width);
            }

            buffer.setString(startCol, row, lineStr, nameStyle);
        }

        // Scroll indicators
        if (scrollOffset > 0) {
            buffer.setString(startCol + width - 2, startRow, "\u25B2", STYLE_GRAY);
        }
        if (endIdx < flattenedNodes.size()) {
            buffer.setString(startCol + width - 2, startRow + visibleRows - 1, "\u25BC", STYLE_GRAY);
        }
    }

    private Style getFileStyle(String name) {
        if (name.endsWith(".java")) {
            return STYLE_JAVA;
        } else if (name.endsWith(".xml") || name.endsWith(".yaml") || name.endsWith(".yml") || name.endsWith(".json")) {
            return STYLE_CONFIG;
        } else if (name.endsWith(".properties") || name.endsWith(".conf")) {
            return STYLE_PROPERTIES;
        } else if (name.endsWith(".html") || name.endsWith(".css") || name.endsWith(".js") || name.endsWith(".ts")) {
            return STYLE_WEB;
        }
        return STYLE_DEFAULT;
    }

    public boolean handleKey(int key) {
        switch (key) {
            case KeyCode.UP:
            case 'k':
                if (selectedIndex > 0) {
                    selectedIndex--;
                    adjustScroll();
                    return true;
                }
                break;

            case KeyCode.DOWN:
            case 'j':
                if (selectedIndex < flattenedNodes.size() - 1) {
                    selectedIndex++;
                    adjustScroll();
                    return true;
                }
                break;

            case KeyCode.LEFT:
            case 'h':
                // Collapse directory or go to parent
                TreeNode<T> current = getSelectedNode();
                if (current != null) {
                    if (current.isDirectory && current.expanded) {
                        current.expanded = false;
                        rebuildFlatList();
                        return true;
                    }
                }
                break;

            case KeyCode.RIGHT:
            case 'l':
                // Expand directory
                TreeNode<T> node = getSelectedNode();
                if (node != null && node.isDirectory && !node.expanded) {
                    node.expanded = true;
                    rebuildFlatList();
                    return true;
                }
                break;

            case KeyCode.ENTER:
            case ' ':
                // Toggle expand/collapse for directories
                TreeNode<T> sel = getSelectedNode();
                if (sel != null && sel.isDirectory) {
                    sel.expanded = !sel.expanded;
                    rebuildFlatList();
                    return true;
                }
                break;

            case KeyCode.PAGE_UP:
                selectedIndex = Math.max(0, selectedIndex - visibleRows);
                adjustScroll();
                return true;

            case KeyCode.PAGE_DOWN:
                selectedIndex = Math.min(flattenedNodes.size() - 1, selectedIndex + visibleRows);
                adjustScroll();
                return true;

            case KeyCode.HOME:
                selectedIndex = 0;
                adjustScroll();
                return true;

            case KeyCode.END:
                selectedIndex = flattenedNodes.size() - 1;
                adjustScroll();
                return true;
        }

        return false;
    }

    private void adjustScroll() {
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + visibleRows) {
            scrollOffset = selectedIndex - visibleRows + 1;
        }
    }

    /**
     * Expand all directories.
     */
    public void expandAll() {
        for (TreeNode<T> node : nodeMap.values()) {
            if (node.isDirectory) {
                node.expanded = true;
            }
        }
        rebuildFlatList();
    }

    /**
     * Collapse all directories.
     */
    public void collapseAll() {
        for (TreeNode<T> node : nodeMap.values()) {
            if (node.isDirectory) {
                node.expanded = false;
            }
        }
        rebuildFlatList();
    }

    /**
     * Tree node class.
     */
    public static class TreeNode<T> {
        public final String name;
        public final String path;
        public final T data;
        public final boolean isDirectory;
        public final List<TreeNode<T>> children = new ArrayList<>();
        public boolean expanded = false;
        public int depth = 0;

        public TreeNode(String name, String path, T data, boolean isDirectory) {
            this.name = name;
            this.path = path;
            this.data = data;
            this.isDirectory = isDirectory;
        }
    }
}
