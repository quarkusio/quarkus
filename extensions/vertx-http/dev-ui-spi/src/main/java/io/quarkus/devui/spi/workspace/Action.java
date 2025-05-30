package io.quarkus.devui.spi.workspace;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Define an action in Dev UI Workspace.
 *
 * @param <T> The input
 * @param <R> The response
 */
public class Action<T, R> {
    private final String label; // This is the label in the dropdown
    private final String namespace; // The namespace of the extension
    private final Function<T, R> function; // The function to call when the user select it
    private final BiFunction<Object, T, R> assistantFunction; // The assistant function to call when the user select it
    private final Optional<Pattern> filter; // Filter to only apply on certain files
    private final Display display; // Response display for the UI
    private final DisplayType displayType; // Response display type
    private final Function<Path, Path> pathConverter; // Convert the input path to something else. By default it will just stay the same

    protected Action(
            String label,
            String namespace,
            Function<T, R> function,
            BiFunction<Object, T, R> assistantFunction,
            Optional<Pattern> filter,
            Display display,
            DisplayType displayType,
            Function<Path, Path> pathConverter) {

        this.label = label;
        this.namespace = namespace;
        this.function = function;
        this.assistantFunction = assistantFunction;
        this.filter = filter;
        this.display = display;
        this.displayType = displayType;
        this.pathConverter = pathConverter;
    }

    public String getId() {
        String id = this.label.toLowerCase().replaceAll(SPACE, DASH);
        try {
            id = URLEncoder.encode(id, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }

        if (this.namespace != null) {
            id = this.namespace.toLowerCase() + SLASH + id;
        }
        return id;
    }

    public String getNamespace() {
        return this.namespace;
    }

    public String getLabel() {
        return this.label;
    }

    public Function<T, R> getFunction() {
        return this.function;
    }

    public BiFunction<Object, T, R> getAssistantFunction() {
        return this.assistantFunction;
    }

    public Optional<Pattern> getFilter() {
        return this.filter;
    }

    public Display getDisplay() {
        return this.display;
    }

    public DisplayType getDisplayType() {
        return this.displayType;
    }

    public Function<Path, Path> getPathConverter() {
        return this.pathConverter;
    }

    public boolean isAssistant() {
        return this.assistantFunction != null;
    }

    @Override
    public String toString() {
        return "Action {\n\tid=" + getId()
                + ", \n\tlabel=" + label
                + ", \n\tnamespace=" + namespace
                + ", \n\tfilter=" + filter
                + ", \n\tdisplay=" + display
                + ", \n\tdisplayType=" + displayType
                + "\n}";
    }

    public static ActionBuilder actionBuilder() {
        return new ActionBuilder();
    }

    private static final String SPACE = " ";
    private static final String DASH = "-";
    private static final String SLASH = "/";
}
