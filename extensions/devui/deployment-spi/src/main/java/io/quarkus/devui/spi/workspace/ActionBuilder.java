package io.quarkus.devui.spi.workspace;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;

public class ActionBuilder<T, R> {

    private String label = null;
    private String namespace = null;
    private Function<T, R> function;
    private BiFunction<Object, T, R> assistantFunction;
    private Optional<Pattern> filter = Optional.empty();
    private Display display = Display.dialog;
    private DisplayType displayType = DisplayType.code;
    private Function<Path, Path> pathConverter = (p) -> p;

    public ActionBuilder label(String label) {
        this.label = label;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public ActionBuilder namespace(String namespace) {
        if (this.namespace == null) {
            this.namespace = namespace;
        }
        return this;
    }

    public ActionBuilder function(Function<T, R> function) {
        this.function = function;
        return this;
    }

    public ActionBuilder assistantFunction(BiFunction<Object, T, R> assistantFunction) {
        this.assistantFunction = assistantFunction;
        return this;
    }

    public ActionBuilder filter(Pattern filter) {
        this.filter = Optional.of(filter);
        return this;
    }

    public ActionBuilder display(Display display) {
        this.display = display;
        return this;
    }

    public ActionBuilder displayType(DisplayType displayType) {
        this.displayType = displayType;
        return this;
    }

    public ActionBuilder pathConverter(Function<Path, Path> pathConverter) {
        this.pathConverter = pathConverter;
        return this;
    }

    public Action build() {
        if (this.label == null || (this.function == null && this.assistantFunction == null)) {
            throw new RuntimeException(
                    "Not enough information to build the action. Set at least label and function/assistantFunction");
        }
        return new Action(label, namespace, function, assistantFunction, filter, display, displayType, pathConverter);
    }
}
