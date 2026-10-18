package io.quarkus.qute.deployment;

import java.util.Objects;
import java.util.function.Consumer;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.qute.ParserHelper;

/**
 * This build item can be used to hook into the parser logic during validation at build time.
 * <p>
 * Validation parser hooks are never used at runtime.
 */
public final class ValidationParserHookBuildItem extends MultiBuildItem {

    private final Consumer<ParserHelper> hook;

    public ValidationParserHookBuildItem(Consumer<ParserHelper> hook) {
        this.hook = Objects.requireNonNull(hook);
    }

    public Consumer<ParserHelper> getHook() {
        return hook;
    }

    public void accept(ParserHelper helper) {
        hook.accept(helper);
    }

}
