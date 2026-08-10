package io.quarkus.devshell.deployment.tui.screens;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import io.quarkus.devshell.deployment.tui.AppContext;
import io.quarkus.devshell.deployment.tui.KeyCode;
import io.quarkus.devshell.deployment.tui.Screen;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

public class ConfigDetailScreen implements Screen {

    private static final Style LABEL = Style.EMPTY.cyan();
    private static final Style VALUE = Style.EMPTY.white();
    private static final Style VALUE_SET = Style.EMPTY.green();
    private static final Style VALUE_UNSET = Style.EMPTY.gray();
    private static final Style DIM = Style.EMPTY.gray();
    private static final Style WARN = Style.EMPTY.yellow();
    private static final Style ERROR = Style.EMPTY.red();
    private static final Style EDIT_STYLE = Style.EMPTY.onWhite().black();

    private enum Mode {
        VIEW,
        EDIT,
        DELETE_CONFIRM
    }

    private final ConfigurationScreen.ConfigItem item;
    private AppContext ctx;
    private Mode mode = Mode.VIEW;

    private String editBuffer = "";
    private int cursorPos = 0;
    private String statusMessage;
    private Style statusStyle = DIM;

    private int descriptionScrollOffset = 0;
    private List<String> wrappedDescription = List.of();

    public ConfigDetailScreen(ConfigurationScreen.ConfigItem item) {
        this.item = item;
    }

    @Override
    public String getTitle() {
        return "Config: " + item.name();
    }

    @Override
    public void onEnter(AppContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        var areas = Layout.vertical()
                .constraints(Constraint.length(8), Constraint.fill(), Constraint.length(1))
                .split(area);

        renderProperties(areas.get(0), buffer);
        renderDescription(areas.get(1), buffer);
        renderFooter(areas.get(2), buffer);
    }

    private void renderProperties(Rect area, Buffer buffer) {
        int x = area.x() + 2;
        int y = area.y();

        buffer.setString(x, y, "Name:", LABEL);
        buffer.setString(x + 14, y, item.name(), VALUE);
        y++;

        buffer.setString(x, y, "Value:", LABEL);
        if (mode == Mode.EDIT) {
            String display = editBuffer + " ";
            buffer.setString(x + 14, y, display, EDIT_STYLE);
            if (cursorPos < display.length()) {
                buffer.setString(x + 14 + cursorPos, y, String.valueOf(display.charAt(cursorPos)),
                        Style.EMPTY.onCyan().black());
            }
        } else {
            if (item.value().isEmpty()) {
                buffer.setString(x + 14, y, "(not set)", VALUE_UNSET);
            } else {
                buffer.setString(x + 14, y, item.value(), VALUE_SET);
            }
        }
        y++;

        buffer.setString(x, y, "Default:", LABEL);
        String defaultVal = item.defaultValue().isEmpty() ? "(none)" : item.defaultValue();
        buffer.setString(x + 14, y, defaultVal, DIM);
        y++;

        buffer.setString(x, y, "Phase:", LABEL);
        String phaseLabel = formatPhase(item.configPhase());
        Style phaseStyle = phaseStyle(item.configPhase());
        buffer.setString(x + 14, y, phaseLabel, phaseStyle);
        String phaseDesc = phaseDescription(item.configPhase());
        buffer.setString(x + 14 + phaseLabel.length() + 2, y, phaseDesc, DIM);
        y++;

        if (!item.sourceName().isEmpty()) {
            buffer.setString(x, y, "Source:", LABEL);
            buffer.setString(x + 14, y, item.sourceName(), DIM);
            y++;
        }

        if (mode == Mode.DELETE_CONFIRM) {
            y++;
            buffer.setString(x, y, "Delete this property? (Y/N)", WARN);
        }

        if (statusMessage != null) {
            y++;
            buffer.setString(x, y, statusMessage, statusStyle);
        }
    }

    private void renderDescription(Rect area, Buffer buffer) {
        int x = area.x() + 2;
        int y = area.y();
        int width = area.width() - 4;

        buffer.setString(x, y, "Description:", LABEL);
        y++;

        String desc = item.description();
        if (desc == null || desc.isEmpty()) {
            buffer.setString(x, y, "(no description available)", DIM);
            return;
        }

        wrappedDescription = wrapText(desc, Math.max(1, width));
        int visibleRows = area.height() - 1;

        for (int i = 0; i < visibleRows && (descriptionScrollOffset + i) < wrappedDescription.size(); i++) {
            buffer.setString(x, y + i, wrappedDescription.get(descriptionScrollOffset + i), VALUE);
        }

        if (wrappedDescription.size() > visibleRows) {
            int pct = (int) ((descriptionScrollOffset + visibleRows) * 100.0 / wrappedDescription.size());
            buffer.setString(area.x() + area.width() - 5, area.y() + area.height() - 1,
                    Math.min(pct, 100) + "%", DIM);
        }
    }

    private void renderFooter(Rect area, Buffer buffer) {
        if (mode == Mode.EDIT) {
            buffer.setLine(area.x(), area.y(), Line.from(
                    Span.styled(" Enter", Style.EMPTY.cyan()), Span.styled(" Save  ", DIM),
                    Span.styled("ESC", Style.EMPTY.cyan()), Span.styled(" Cancel", DIM)));
        } else if (mode == Mode.DELETE_CONFIRM) {
            buffer.setLine(area.x(), area.y(), Line.from(
                    Span.styled(" Y", Style.EMPTY.cyan()), Span.styled(" Confirm  ", DIM),
                    Span.styled("N", Style.EMPTY.cyan()), Span.styled(" Cancel", DIM)));
        } else {
            buffer.setLine(area.x(), area.y(), Line.from(
                    Span.styled(" E", Style.EMPTY.cyan()), Span.styled(" Edit  ", DIM),
                    Span.styled("D", Style.EMPTY.cyan()), Span.styled(" Delete  ", DIM),
                    Span.styled("R", Style.EMPTY.cyan()), Span.styled(" Refresh  ", DIM),
                    Span.styled("j/k", Style.EMPTY.cyan()), Span.styled(" Scroll  ", DIM),
                    Span.styled("ESC", Style.EMPTY.cyan()), Span.styled(" Back", DIM)));
        }
    }

    @Override
    public boolean handleKey(int[] keys) {
        int key = KeyCode.parse(keys);

        switch (mode) {
            case EDIT:
                return handleEditKey(key);
            case DELETE_CONFIRM:
                return handleDeleteConfirmKey(key);
            default:
                return handleViewKey(key);
        }
    }

    private boolean handleViewKey(int key) {
        switch (key) {
            case 'e':
            case 'E':
                mode = Mode.EDIT;
                editBuffer = item.value();
                cursorPos = editBuffer.length();
                statusMessage = null;
                ctx.requestRedraw();
                return true;
            case 'd':
            case 'D':
                mode = Mode.DELETE_CONFIRM;
                statusMessage = null;
                ctx.requestRedraw();
                return true;
            case 'r':
            case 'R':
                statusMessage = null;
                ctx.requestRedraw();
                return true;
            case 'j':
            case KeyCode.DOWN:
                return scrollDown();
            case 'k':
            case KeyCode.UP:
                return scrollUp();
            case KeyCode.ESCAPE:
                return false;
        }
        return false;
    }

    private boolean handleEditKey(int key) {
        switch (key) {
            case KeyCode.ENTER:
            case KeyCode.NEWLINE:
                saveProperty();
                return true;
            case KeyCode.ESCAPE:
                mode = Mode.VIEW;
                ctx.requestRedraw();
                return true;
            case KeyCode.BACKSPACE:
            case 8:
                if (cursorPos > 0) {
                    editBuffer = editBuffer.substring(0, cursorPos - 1) + editBuffer.substring(cursorPos);
                    cursorPos--;
                    ctx.requestRedraw();
                }
                return true;
            case KeyCode.LEFT:
                if (cursorPos > 0) {
                    cursorPos--;
                    ctx.requestRedraw();
                }
                return true;
            case KeyCode.RIGHT:
                if (cursorPos < editBuffer.length()) {
                    cursorPos++;
                    ctx.requestRedraw();
                }
                return true;
            case KeyCode.HOME:
                cursorPos = 0;
                ctx.requestRedraw();
                return true;
            case KeyCode.END:
                cursorPos = editBuffer.length();
                ctx.requestRedraw();
                return true;
            default:
                if (KeyCode.isPrintable(key)) {
                    editBuffer = editBuffer.substring(0, cursorPos) + (char) key + editBuffer.substring(cursorPos);
                    cursorPos++;
                    ctx.requestRedraw();
                }
                return true;
        }
    }

    private boolean handleDeleteConfirmKey(int key) {
        switch (key) {
            case 'y':
            case 'Y':
                deleteProperty();
                return true;
            case 'n':
            case 'N':
            case KeyCode.ESCAPE:
                mode = Mode.VIEW;
                ctx.requestRedraw();
                return true;
        }
        return true;
    }

    private void saveProperty() {
        mode = Mode.VIEW;
        statusMessage = "Saving...";
        statusStyle = WARN;
        ctx.requestRedraw();

        CompletableFuture.runAsync(() -> {
            try {
                ObjectMapper mapper = new ObjectMapper();
                ObjectNode params = mapper.createObjectNode();
                params.put("name", item.name());
                params.put("value", editBuffer);
                ctx.getJsonRpcClient().call("devui-configuration", "updateProperty", params);

                String phaseHint = "";
                if ("BUILD_TIME".equals(item.configPhase())) {
                    phaseHint = " (rebuild required for build-time property)";
                } else if ("BUILD_AND_RUN_TIME_FIXED".equals(item.configPhase())) {
                    phaseHint = " (restart required for build+run-time property)";
                }
                statusMessage = "Saved successfully" + phaseHint;
                statusStyle = VALUE_SET;
            } catch (Exception e) {
                statusMessage = "Save failed: " + e.getMessage();
                statusStyle = ERROR;
            }
            ctx.requestRedraw();
        });
    }

    private void deleteProperty() {
        mode = Mode.VIEW;
        statusMessage = "Deleting...";
        statusStyle = WARN;
        ctx.requestRedraw();

        CompletableFuture.runAsync(() -> {
            try {
                ObjectMapper mapper = new ObjectMapper();
                ObjectNode params = mapper.createObjectNode();
                params.put("name", item.name());
                ctx.getJsonRpcClient().call("devui-configuration", "removeProperty", params);
                statusMessage = "Property removed";
                statusStyle = VALUE_SET;
            } catch (Exception e) {
                statusMessage = "Delete failed: " + e.getMessage();
                statusStyle = ERROR;
            }
            ctx.requestRedraw();
        });
    }

    private boolean scrollUp() {
        if (descriptionScrollOffset > 0) {
            descriptionScrollOffset--;
            ctx.requestRedraw();
            return true;
        }
        return true;
    }

    private boolean scrollDown() {
        if (descriptionScrollOffset < wrappedDescription.size() - 1) {
            descriptionScrollOffset++;
            ctx.requestRedraw();
            return true;
        }
        return true;
    }

    private static String formatPhase(String phase) {
        return switch (phase) {
            case "BUILD_TIME" -> "BUILD_TIME";
            case "BUILD_AND_RUN_TIME_FIXED" -> "BUILD+RUN";
            case "RUN_TIME" -> "RUN_TIME";
            default -> phase;
        };
    }

    private static String phaseDescription(String phase) {
        return switch (phase) {
            case "BUILD_TIME" -> "Fixed at build time, requires rebuild";
            case "BUILD_AND_RUN_TIME_FIXED" -> "Set at build time, requires restart";
            case "RUN_TIME" -> "Can be changed at runtime";
            default -> "";
        };
    }

    private static Style phaseStyle(String phase) {
        return switch (phase) {
            case "BUILD_TIME" -> Style.EMPTY.red();
            case "BUILD_AND_RUN_TIME_FIXED" -> Style.EMPTY.yellow();
            case "RUN_TIME" -> Style.EMPTY.green();
            default -> Style.EMPTY.white();
        };
    }

    private static List<String> wrapText(String text, int width) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty() || width <= 0) {
            return lines;
        }
        for (String paragraph : text.split("\n")) {
            if (paragraph.isEmpty()) {
                lines.add("");
                continue;
            }
            String[] words = paragraph.split(" ");
            StringBuilder current = new StringBuilder();
            for (String word : words) {
                if (current.length() + word.length() + 1 > width && current.length() > 0) {
                    lines.add(current.toString());
                    current = new StringBuilder();
                }
                if (current.length() > 0) {
                    current.append(' ');
                }
                current.append(word);
            }
            if (current.length() > 0) {
                lines.add(current.toString());
            }
        }
        return lines;
    }
}
