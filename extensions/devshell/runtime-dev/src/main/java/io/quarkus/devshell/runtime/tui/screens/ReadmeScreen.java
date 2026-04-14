package io.quarkus.devshell.runtime.tui.screens;

import java.util.Map;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.devshell.runtime.tui.AppContext;
import io.quarkus.devshell.runtime.tui.BufferHelper;
import io.quarkus.devshell.runtime.tui.KeyCode;
import io.quarkus.devshell.runtime.tui.Screen;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Screen for viewing the project README file.
 */
public class ReadmeScreen implements Screen {

    private AppContext ctx;
    private boolean loading = true;
    private String error = null;
    private String readmeContent = null;
    private String[] lines = new String[0];
    private int scrollOffset = 0;
    private String readmePath = null;

    @Override
    public String getTitle() {
        return "Readme";
    }

    @Override
    public void onEnter(AppContext ctx) {
        this.ctx = ctx;
        findAndLoadReadme();
    }

    @Override
    public void onLeave() {
        // Nothing to clean up
    }

    private void findAndLoadReadme() {
        loading = true;
        error = null;
        this.ctx.requestRedraw();

        try {
            Object workspaceItems = DevConsoleManager.invoke("devui-workspace_getWorkspaceItems");
            String foundPath = findReadmePath(workspaceItems);
            if (foundPath != null) {
                readmePath = foundPath;
                loadReadmeContent(foundPath);
            } else {
                loading = false;
                error = "No README.md file found in the project.";
                this.ctx.requestRedraw();
            }
        } catch (Exception ex) {
            loading = false;
            error = "Failed to search for README: " + ex.getMessage();
            this.ctx.requestRedraw();
        }
    }

    private String findReadmePath(Object workspaceItems) {
        try {
            if (workspaceItems == null) {
                return null;
            }

            JsonObject json = JsonObject.mapFrom(workspaceItems);
            JsonArray itemsArray = json.getJsonArray("items");
            if (itemsArray == null) {
                return null;
            }

            // Search for README.md (case-insensitive)
            for (int i = 0; i < itemsArray.size(); i++) {
                JsonObject item = itemsArray.getJsonObject(i);
                String name = item.getString("name");
                if (name != null && name.equalsIgnoreCase("README.md")) {
                    // Jackson serializes Path as a URI string already
                    return item.getString("path");
                }
            }
        } catch (Exception e) {
            // Ignore errors
        }
        return null;
    }

    private void loadReadmeContent(String path) {
        try {
            Object contentObj = DevConsoleManager.invoke("devui-workspace_getWorkspaceItemContent",
                    Map.of("path", path));
            loading = false;

            if (contentObj != null) {
                JsonObject json = JsonObject.mapFrom(contentObj);
                String content = json.getString("content");
                boolean isBinary = json.getBoolean("isBinary", false);

                if (isBinary) {
                    error = "README file appears to be binary.";
                } else if (content != null) {
                    readmeContent = content;
                    lines = content.split("\n", -1);
                } else {
                    error = "Could not read README content.";
                }
            } else {
                error = "README file is empty.";
            }

            this.ctx.requestRedraw();
        } catch (Exception ex) {
            loading = false;
            error = "Failed to load README: " + ex.getMessage();
            this.ctx.requestRedraw();
        }
    }

    @Override
    public void render(Frame frame) {
        Buffer buffer = frame.buffer();
        int width = this.ctx.getWidth();
        int height = this.ctx.getHeight();

        // Header
        renderHeader(buffer, width);

        if (loading) {
            buffer.setString(width / 2 - 5, height / 2, "Loading...", Style.create().yellow());
            return;
        }

        if (error != null) {
            buffer.setString(1, 4, error, Style.create().red());
            renderFooter(buffer, width, height);
            return;
        }

        if (readmeContent == null || lines.length == 0) {
            buffer.setString(1, 4, "No README content to display.", Style.create().gray());
            renderFooter(buffer, width, height);
            return;
        }

        // Render README content with basic markdown highlighting
        renderContent(buffer, width, height);

        // Footer
        renderFooter(buffer, width, height);
    }

    private void renderHeader(Buffer buffer, int width) {
        BufferHelper.writeHeader(buffer, " README ", width);

        // File info
        if (readmePath != null) {
            buffer.setString(1, 2, readmePath, Style.create().gray());
        }
        if (lines.length > 0) {
            buffer.setString(1, 3, lines.length + " lines", Style.create().gray());
        }

    }

    private void renderContent(Buffer buffer, int width, int height) {
        int contentHeight = height - 5;
        int contentWidth = width - 4;
        int endLine = Math.min(scrollOffset + contentHeight, lines.length);

        for (int i = scrollOffset; i < endLine; i++) {
            int row = 4 + (i - scrollOffset);

            String line = lines[i];
            String rendered = renderMarkdownLine(line, contentWidth);
            buffer.setString(1, row, rendered, Style.EMPTY);
        }

        // Scroll indicator
        if (lines.length > contentHeight) {
            int scrollPercent = (int) ((scrollOffset + contentHeight) * 100.0 / lines.length);
            buffer.setString(width - 10, height - 3, scrollPercent + "%", Style.create().gray());
        }
    }

    private String renderMarkdownLine(String line, int maxWidth) {
        if (line == null || line.isEmpty()) {
            return "";
        }

        // Truncate if too long
        if (line.length() > maxWidth) {
            line = line.substring(0, maxWidth - 3) + "...";
        }

        // Bullet points
        if (line.startsWith("- ") || line.startsWith("* ") || line.matches("^\\d+\\. .*")) {
            return "\u2022 " + line.substring(2);
        }

        // Blockquotes
        if (line.startsWith("> ")) {
            return "\u2502 " + line.substring(2);
        }

        // Horizontal rules
        if (line.matches("^[-*_]{3,}$")) {
            return "\u2500".repeat(Math.min(maxWidth, 40));
        }

        // Inline code - strip backticks
        if (line.contains("`")) {
            return stripBackticks(line);
        }

        return line;
    }

    private String stripBackticks(String line) {
        return line.replace("`", "");
    }

    private void renderFooter(Buffer buffer, int width, int height) {
        buffer.setString(1, height - 2, "[Up/Down] Scroll  [PgUp/PgDn] Page  [Home/End] Jump  [R] Refresh  [Esc] Back",
                Style.create().gray());
    }

    @Override
    public boolean handleKey(int key) {
        if (loading) {
            return true;
        }

        int contentHeight = this.ctx.getHeight() - 5;
        int maxScroll = Math.max(0, lines.length - contentHeight);

        switch (key) {
            case KeyCode.ESCAPE:
                this.ctx.goBack();
                return true;

            case KeyCode.UP:
            case 'k':
                if (scrollOffset > 0) {
                    scrollOffset--;
                    this.ctx.requestRedraw();
                }
                return true;

            case KeyCode.DOWN:
            case 'j':
                if (scrollOffset < maxScroll) {
                    scrollOffset++;
                    this.ctx.requestRedraw();
                }
                return true;

            case KeyCode.PAGE_UP:
                scrollOffset = Math.max(0, scrollOffset - contentHeight);
                this.ctx.requestRedraw();
                return true;

            case KeyCode.PAGE_DOWN:
                scrollOffset = Math.min(maxScroll, scrollOffset + contentHeight);
                this.ctx.requestRedraw();
                return true;

            case KeyCode.HOME:
                scrollOffset = 0;
                this.ctx.requestRedraw();
                return true;

            case KeyCode.END:
                scrollOffset = maxScroll;
                this.ctx.requestRedraw();
                return true;

            case 'r':
            case 'R':
                findAndLoadReadme();
                return true;

            default:
                return false;
        }
    }

    @Override
    public void onResize(int width, int height) {
        // Adjust scroll if needed
        int contentHeight = height - 5;
        int maxScroll = Math.max(0, lines.length - contentHeight);
        if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
        }
    }

}
