package io.quarkus.deployment.execannotations;

import java.util.Objects;
import java.util.function.Predicate;

import org.jboss.jandex.MethodInfo;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Carries a predicate that identifies methods that can have annotations which affect
 * the execution model ({@code @Blocking}, {@code @NonBlocking}, {@code @RunOnVirtualThread}).
 * <p>
 * Used to detect wrong usage of these annotations, as they are implemented directly
 * by the various frameworks and may only be put on "entrypoint" methods. Placing these
 * annotations on methods that can only be invoked by application code is always wrong.
 */
public final class ExecutionModelAnnotationsAllowedBuildItem extends MultiBuildItem {
    private final Predicate<MethodInfo> predicate;

    public ExecutionModelAnnotationsAllowedBuildItem(Predicate<MethodInfo> predicate) {
        this.predicate = Objects.requireNonNull(predicate);
    }

    public boolean matches(MethodInfo method) {
        return predicate.test(method);
    }
}
