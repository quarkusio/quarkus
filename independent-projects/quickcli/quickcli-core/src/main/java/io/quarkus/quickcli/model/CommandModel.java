package io.quarkus.quickcli.model;

import io.quarkus.quickcli.CommandSpec;
import io.quarkus.quickcli.ParseResult;

import java.util.List;

/**
 * Interface for generated command model classes. The annotation processor generates
 * an implementation of this interface for each @Command-annotated class, containing
 * all metadata that would otherwise require runtime reflection to discover.
 *
 * <p>Generated implementations self-register with {@link CommandModelRegistry}
 * in a static initializer, eliminating any need for ServiceLoader or classpath scanning.</p>
 */
public interface CommandModel {

    /**
     * Returns the command class this model describes.
     */
    Class<?> commandClass();

    /**
     * Creates a new instance of the command class using a direct {@code new} call.
     * No reflection is used.
     */
    Object createInstance();

    /**
     * Builds the command specification with all options, parameters, and subcommand
     * metadata. This is called once during initialization — all metadata is hardcoded
     * in the generated source, so no reflection is needed.
     */
    CommandSpec buildSpec();

    /**
     * Applies parsed values to the command instance. Uses direct field access for
     * non-private fields, and setter methods for private fields. No reflection is used.
     *
     * @param instance the command instance to populate
     * @param result the parse result containing option/parameter values
     */
    void applyValues(Object instance, ParseResult result);

    /**
     * Sets the parent command reference on the instance, if the command has a
     * {@code @ParentCommand} annotated field.
     *
     * @param instance the command instance
     * @param parent the parent command instance
     */
    default void setParentCommand(Object instance, Object parent) {
        // Default no-op; overridden when @ParentCommand is present
    }

    /**
     * Creates and populates mixin instances on the command, if the command has
     * {@code @Mixin} annotated fields.
     *
     * @param instance the command instance
     * @param factory factory for creating mixin instances
     */
    default void initMixins(Object instance, io.quarkus.quickcli.Factory factory) throws Exception {
        // Default no-op; overridden when @Mixin fields are present
    }

    /**
     * Injects the CommandSpec into the instance, if the command has a
     * {@code @Spec} annotated field.
     *
     * @param instance the command instance
     * @param spec the command specification
     */
    default void injectSpec(Object instance, CommandSpec spec) {
        // Default no-op; overridden when @Spec is present
    }

    /**
     * Sets unmatched arguments on the instance, if the command has an
     * {@code @Unmatched} annotated field.
     *
     * @param instance the command instance
     * @param unmatched the list of unmatched arguments
     */
    default void setUnmatched(Object instance, List<String> unmatched) {
        // Default no-op; overridden when @Unmatched is present
    }

    /**
     * Initializes ArgGroup fields on the command instance.
     *
     * @param instance the command instance
     * @param factory factory for creating group instances
     */
    default void initArgGroups(Object instance, io.quarkus.quickcli.Factory factory) throws Exception {
        // Default no-op; overridden when @ArgGroup fields are present
    }
}
