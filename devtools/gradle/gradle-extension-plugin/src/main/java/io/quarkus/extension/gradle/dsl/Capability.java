package io.quarkus.extension.gradle.dsl;

import java.util.ArrayList;
import java.util.List;

/**
 * One provided or required Quarkus extension capability declaration.
 * <p>
 * A capability may be conditional on deployment-side {@link java.util.function.BooleanSupplier} implementations.
 * Conditions are emitted into the generated extension descriptor with the capability.
 */
public class Capability {

    private String name;

    private List<String> onlyIf = new ArrayList<>(0);

    private List<String> onlyIfNot = new ArrayList<>(0);

    /**
     * Creates a capability declaration.
     *
     * @param name the capability name
     */
    public Capability(String name) {
        this.name = name;
    }

    /**
     * Returns the capability name.
     *
     * @return the capability name
     */
    public String getName() {
        return name;
    }

    /**
     * Appends condition classes whose boolean suppliers must return {@code true}.
     *
     * @param conditions fully qualified boolean-supplier class names
     * @return this declaration
     */
    public Capability onlyIf(List<String> conditions) {
        onlyIf.addAll(conditions);
        return this;
    }

    /**
     * Returns positive boolean-supplier conditions.
     *
     * @return the mutable conditions in declaration order
     */
    public List<String> getOnlyIf() {
        return onlyIf;
    }

    /**
     * Appends condition classes whose boolean suppliers must return {@code false}.
     *
     * @param conditions fully qualified boolean-supplier class names
     * @return this declaration
     */
    public Capability onlyIfNot(List<String> conditions) {
        onlyIfNot.addAll(conditions);
        return this;
    }

    /**
     * Returns negative boolean-supplier conditions.
     *
     * @return the mutable conditions in declaration order
     */
    public List<String> getOnlyIfNot() {
        return onlyIfNot;
    }
}
