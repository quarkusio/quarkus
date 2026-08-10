package io.quarkus.devshell.deployment.tui.screens;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import io.quarkus.devshell.deployment.tui.AppContext;
import io.quarkus.devshell.deployment.tui.KeyCode;
import io.quarkus.devshell.deployment.tui.Screen;
import io.quarkus.devshell.deployment.tui.widgets.TreeView;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

public class WorkspaceScreen implements Screen {

    private static final Style DIM = Style.EMPTY.gray();
    private static final Style NORMAL = Style.EMPTY.white();
    private static final Style CYAN = Style.EMPTY.cyan();
    private static final Style YELLOW = Style.EMPTY.yellow();

    private AppContext ctx;
    private volatile boolean loading = true;
    private volatile String errorMessage;

    private final List<WorkspaceItem> items = new ArrayList<>();
    private final TreeView<WorkspaceItem> treeView;

    private String selectedFilePath;
    private volatile String selectedFileContent;
    private volatile boolean loadingContent;
    private int contentScrollOffset = 0;

    private boolean editMode = false;
    private List<StringBuilder> editLines;
    private int cursorRow = 0;
    private int cursorCol = 0;
    private boolean modified = false;

    public WorkspaceScreen() {
        this.treeView = new TreeView<>(item -> item.name, item -> item.name);
    }

    @Override
    public String getTitle() {
        return "Workspace";
    }

    @Override
    public void onEnter(AppContext ctx) {
        this.ctx = ctx;
        loadDataAsync();
    }

    private void loadDataAsync() {
        loading = true;
        ctx.requestRedraw();

        CompletableFuture.runAsync(() -> {
            try {
                JsonNode result = ctx.getJsonRpcClient().call("devui-workspace", "getWorkspaceItems");
                items.clear();
                JsonNode arr = result.isArray() ? result : result.path("items");
                if (arr.isArray()) {
                    for (JsonNode item : arr) {
                        String name = item.path("name").asText(item.path("fileName").asText(""));
                        String path = item.path("path").asText(item.path("filePath").asText(""));
                        if (!name.isEmpty()) {
                            items.add(new WorkspaceItem(name, path));
                        }
                    }
                }
                treeView.setItems(items);
                treeView.expandAll();
                loading = false;
            } catch (Exception e) {
                errorMessage = e.getMessage();
                loading = false;
            }
            ctx.requestRedraw();
        });
    }

    private void loadFileContent(String path) {
        if (path == null) {
            return;
        }
        loadingContent = true;
        selectedFilePath = path;
        selectedFileContent = null;
        contentScrollOffset = 0;
        ctx.requestRedraw();

        CompletableFuture.runAsync(() -> {
            try {
                ObjectNode params = new ObjectMapper().createObjectNode();
                params.put("path", path);
                JsonNode result = ctx.getJsonRpcClient().call("devui-workspace", "getWorkspaceItemContent", params);

                boolean isBinary = result.path("isBinary").asBoolean(false);
                if (isBinary) {
                    selectedFileContent = "[Binary file]";
                } else {
                    String content = result.path("content").asText("");
                    selectedFileContent = content.isEmpty() ? "(empty)" : content;
                }
                loadingContent = false;
            } catch (Exception e) {
                selectedFileContent = "Error: " + e.getMessage();
                loadingContent = false;
            }
            ctx.requestRedraw();
        });
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        if (loading) {
            buffer.setString(area.x() + 2, area.y() + 1, "Loading workspace...", YELLOW);
            return;
        }
        if (errorMessage != null) {
            buffer.setString(area.x() + 2, area.y() + 1, "Error: " + errorMessage, Style.EMPTY.red());
            return;
        }
        if (items.isEmpty()) {
            buffer.setString(area.x() + 2, area.y() + 1, "No workspace items found", DIM);
            return;
        }

        int treeWidth = Math.min(50, area.width() / 2);
        int contentWidth = area.width() - treeWidth - 1;

        buffer.setString(area.x(), area.y(), " Files (" + items.size() + ")", CYAN.bold());

        treeView.setVisibleRows(area.height() - 3);
        treeView.setWidth(treeWidth - 1);
        treeView.render(buffer, area.y() + 1, area.x());

        for (int row = area.y() + 1; row < area.y() + area.height() - 1; row++) {
            buffer.setString(area.x() + treeWidth, row, "|", DIM);
        }

        renderContentPanel(buffer, area.x() + treeWidth + 1, area.y(), contentWidth, area.height());

        renderFooter(buffer, area.x(), area.y() + area.height() - 1, area.width());
    }

    private void renderContentPanel(Buffer buffer, int startCol, int startRow, int width, int height) {
        if (editMode) {
            renderEditPanel(buffer, startCol, startRow, width, height);
            return;
        }

        TreeView.TreeNode<WorkspaceItem> selected = treeView.getSelectedNode();

        if (selected == null || selected.isDirectory) {
            buffer.setString(startCol + 1, startRow + 1, "Select a file to preview", DIM);
            return;
        }

        buffer.setString(startCol + 1, startRow, selected.name, CYAN.bold());

        if (loadingContent) {
            buffer.setString(startCol + 1, startRow + 2, "Loading...", YELLOW);
            return;
        }

        if (selectedFileContent == null) {
            buffer.setString(startCol + 1, startRow + 2, "Press Enter to load file content", DIM);
            return;
        }

        String[] lines = selectedFileContent.split("\n", -1);
        int contentHeight = height - 3;
        int endLine = Math.min(contentScrollOffset + contentHeight, lines.length);

        for (int i = contentScrollOffset; i < endLine; i++) {
            int row = startRow + 2 + (i - contentScrollOffset);
            String lineNum = String.format("%3d ", i + 1);
            buffer.setString(startCol, row, lineNum, DIM);

            String line = lines[i];
            if (line.length() > width - 5) {
                line = line.substring(0, width - 8) + "...";
            }
            buffer.setString(startCol + 4, row, line, NORMAL);
        }

        if (lines.length > contentHeight) {
            int pct = (int) ((contentScrollOffset + contentHeight) * 100.0 / lines.length);
            buffer.setString(startCol + width - 5, startRow + height - 2, Math.min(pct, 100) + "%", DIM);
        }
    }

    private void renderEditPanel(Buffer buffer, int startCol, int startRow, int width, int height) {
        // Header: filename with [EDIT] tag and optional [MODIFIED] tag
        String fileName = selectedFilePath;
        int lastSlash = fileName.lastIndexOf('/');
        if (lastSlash >= 0) {
            fileName = fileName.substring(lastSlash + 1);
        }
        int col = startCol + 1;
        col += buffer.setString(col, startRow, fileName, CYAN.bold());
        col += buffer.setString(col, startRow, " [EDIT]", CYAN);
        if (modified) {
            buffer.setString(col, startRow, " [MODIFIED]", YELLOW);
        }

        // Cursor position indicator
        String posInfo = "Ln " + (cursorRow + 1) + ", Col " + (cursorCol + 1);
        buffer.setString(startCol + width - posInfo.length() - 1, startRow, posInfo, DIM);

        // Auto-scroll to keep cursor visible
        int contentHeight = height - 3;
        if (cursorRow < contentScrollOffset) {
            contentScrollOffset = cursorRow;
        } else if (cursorRow >= contentScrollOffset + contentHeight) {
            contentScrollOffset = cursorRow - contentHeight + 1;
        }

        int endLine = Math.min(contentScrollOffset + contentHeight, editLines.size());

        for (int i = contentScrollOffset; i < endLine; i++) {
            int row = startRow + 2 + (i - contentScrollOffset);
            String lineNum = String.format("%3d ", i + 1);
            buffer.setString(startCol, row, lineNum, DIM);

            StringBuilder lineBuilder = editLines.get(i);
            String line = lineBuilder.toString();
            int maxLineWidth = width - 5;

            if (i == cursorRow) {
                // Render line with cursor
                String beforeCursor = line.substring(0, Math.min(cursorCol, line.length()));
                if (beforeCursor.length() > maxLineWidth - 1) {
                    // Cursor is beyond visible area, truncate from left
                    beforeCursor = beforeCursor.substring(beforeCursor.length() - maxLineWidth + 1);
                }
                int x = startCol + 4;
                x += buffer.setString(x, row, beforeCursor, NORMAL);

                // Render cursor character with reversed style
                String cursorChar = cursorCol < line.length()
                        ? String.valueOf(line.charAt(cursorCol))
                        : " ";
                x += buffer.setString(x, row, cursorChar, NORMAL.reversed());

                // Render after cursor
                if (cursorCol < line.length()) {
                    String afterCursor = line.substring(cursorCol + 1);
                    int remaining = maxLineWidth - (beforeCursor.length() + 1);
                    if (afterCursor.length() > remaining) {
                        afterCursor = afterCursor.substring(0, Math.max(0, remaining - 3)) + "...";
                    }
                    buffer.setString(x, row, afterCursor, NORMAL);
                }
            } else {
                if (line.length() > maxLineWidth) {
                    line = line.substring(0, maxLineWidth - 3) + "...";
                }
                buffer.setString(startCol + 4, row, line, NORMAL);
            }
        }

        if (editLines.size() > contentHeight) {
            int pct = (int) ((contentScrollOffset + contentHeight) * 100.0 / editLines.size());
            buffer.setString(startCol + width - 5, startRow + height - 2, Math.min(pct, 100) + "%", DIM);
        }
    }

    private void renderFooter(Buffer buffer, int x, int y, int width) {
        if (editMode) {
            buffer.setString(x, y, " Ctrl+S: Save  ESC: Cancel  Tab: 4 spaces", DIM);
        } else {
            String footer = " Enter: Preview  E: Expand  C: Collapse  [/]: Scroll content  R: Refresh";
            if (selectedFileContent != null && !"[Binary file]".equals(selectedFileContent)) {
                footer += "  W: Edit";
            }
            footer += "  ESC: Back";
            buffer.setString(x, y, footer, DIM);
        }
    }

    @Override
    public boolean handleKey(int[] keys) {
        if (editMode) {
            return handleEditKey(keys);
        }

        if (loading) {
            return false;
        }

        int key = KeyCode.parse(keys);

        if (treeView.handleKey(key)) {
            TreeView.TreeNode<WorkspaceItem> selected = treeView.getSelectedNode();
            if (selected != null && !selected.isDirectory) {
                if (selected.data != null && !selected.data.path.equals(selectedFilePath)) {
                    selectedFileContent = null;
                    contentScrollOffset = 0;
                }
            }
            ctx.requestRedraw();
            return true;
        }

        switch (key) {
            case KeyCode.ENTER:
                TreeView.TreeNode<WorkspaceItem> selected = treeView.getSelectedNode();
                if (selected != null && !selected.isDirectory && selected.data != null) {
                    loadFileContent(selected.data.path);
                }
                return true;
            case 'e':
            case 'E':
                treeView.expandAll();
                ctx.requestRedraw();
                return true;
            case 'c':
            case 'C':
                treeView.collapseAll();
                ctx.requestRedraw();
                return true;
            case '[':
                if (selectedFileContent != null && contentScrollOffset > 0) {
                    contentScrollOffset--;
                    ctx.requestRedraw();
                }
                return true;
            case ']':
                if (selectedFileContent != null) {
                    contentScrollOffset++;
                    ctx.requestRedraw();
                }
                return true;
            case 'r':
            case 'R':
                loadDataAsync();
                return true;
            case 'w':
            case 'W':
                if (selectedFileContent != null && !"[Binary file]".equals(selectedFileContent)) {
                    enterEditMode();
                }
                return true;
        }

        return false;
    }

    private void enterEditMode() {
        String[] lines = selectedFileContent.split("\n", -1);
        editLines = new ArrayList<>();
        for (String line : lines) {
            editLines.add(new StringBuilder(line));
        }
        editMode = true;
        modified = false;
        cursorRow = 0;
        cursorCol = 0;
        ctx.requestRedraw();
    }

    private boolean handleEditKey(int[] keys) {
        int key = KeyCode.parse(keys);

        switch (key) {
            case KeyCode.ESCAPE:
                editMode = false;
                editLines = null;
                break;
            case KeyCode.CTRL_S:
                saveEditedContent();
                break;
            case KeyCode.ENTER:
                splitLineAtCursor();
                modified = true;
                break;
            case KeyCode.BACKSPACE:
                handleBackspace();
                break;
            case KeyCode.DELETE:
                handleDelete();
                break;
            case KeyCode.TAB:
                editLines.get(cursorRow).insert(cursorCol, "    ");
                cursorCol += 4;
                modified = true;
                break;
            case KeyCode.UP:
                if (cursorRow > 0) {
                    cursorRow--;
                    cursorCol = Math.min(cursorCol, editLines.get(cursorRow).length());
                }
                break;
            case KeyCode.DOWN:
                if (cursorRow < editLines.size() - 1) {
                    cursorRow++;
                    cursorCol = Math.min(cursorCol, editLines.get(cursorRow).length());
                }
                break;
            case KeyCode.LEFT:
                if (cursorCol > 0) {
                    cursorCol--;
                } else if (cursorRow > 0) {
                    cursorRow--;
                    cursorCol = editLines.get(cursorRow).length();
                }
                break;
            case KeyCode.RIGHT:
                if (cursorCol < editLines.get(cursorRow).length()) {
                    cursorCol++;
                } else if (cursorRow < editLines.size() - 1) {
                    cursorRow++;
                    cursorCol = 0;
                }
                break;
            case KeyCode.HOME:
                cursorCol = 0;
                break;
            case KeyCode.END:
                cursorCol = editLines.get(cursorRow).length();
                break;
            case KeyCode.PAGE_UP:
                cursorRow = Math.max(0, cursorRow - 20);
                cursorCol = Math.min(cursorCol, editLines.get(cursorRow).length());
                break;
            case KeyCode.PAGE_DOWN:
                cursorRow = Math.min(editLines.size() - 1, cursorRow + 20);
                cursorCol = Math.min(cursorCol, editLines.get(cursorRow).length());
                break;
            default:
                if (KeyCode.isPrintable(key)) {
                    editLines.get(cursorRow).insert(cursorCol, (char) key);
                    cursorCol++;
                    modified = true;
                }
                break;
        }
        ctx.requestRedraw();
        return true;
    }

    private void splitLineAtCursor() {
        StringBuilder currentLine = editLines.get(cursorRow);
        String afterCursor = currentLine.substring(cursorCol);
        currentLine.delete(cursorCol, currentLine.length());
        cursorRow++;
        cursorCol = 0;
        editLines.add(cursorRow, new StringBuilder(afterCursor));
    }

    private void handleBackspace() {
        if (cursorCol > 0) {
            editLines.get(cursorRow).deleteCharAt(cursorCol - 1);
            cursorCol--;
            modified = true;
        } else if (cursorRow > 0) {
            StringBuilder previousLine = editLines.get(cursorRow - 1);
            int mergePoint = previousLine.length();
            previousLine.append(editLines.get(cursorRow));
            editLines.remove(cursorRow);
            cursorRow--;
            cursorCol = mergePoint;
            modified = true;
        }
    }

    private void handleDelete() {
        StringBuilder currentLine = editLines.get(cursorRow);
        if (cursorCol < currentLine.length()) {
            currentLine.deleteCharAt(cursorCol);
            modified = true;
        } else if (cursorRow < editLines.size() - 1) {
            currentLine.append(editLines.get(cursorRow + 1));
            editLines.remove(cursorRow + 1);
            modified = true;
        }
    }

    private void saveEditedContent() {
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < editLines.size(); i++) {
            if (i > 0) {
                content.append('\n');
            }
            content.append(editLines.get(i));
        }
        ObjectNode params = new ObjectMapper().createObjectNode();
        params.put("path", selectedFilePath);
        params.put("content", content.toString());
        ctx.getJsonRpcClient().call("devui-workspace", "saveWorkspaceItemContent", params);
        selectedFileContent = content.toString();
        modified = false;
    }

    private record WorkspaceItem(String name, String path) {
    }
}
