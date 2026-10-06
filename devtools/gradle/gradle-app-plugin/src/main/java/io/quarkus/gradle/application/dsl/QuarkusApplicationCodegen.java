package io.quarkus.gradle.application.dsl;

import java.util.List;

import org.gradle.api.provider.ListProperty;

/**
 * Configures source discovery for the standalone application's main and test code-generation tasks.
 * <p>
 * Both lists are provider-backed task inputs. Their conventions recognize the Quarkus code-generation providers
 * {@code grpc}, {@code avdl}, {@code avpr}, and {@code avsc}, and the source-directory names {@code proto} and
 * {@code avro}. Replacing either property replaces that convention; adding entries extends it.
 */
public abstract class QuarkusApplicationCodegen {

    static final List<String> DEFAULT_PROVIDERS = List.of("grpc", "avdl", "avpr", "avsc");
    static final List<String> DEFAULT_INPUT_NAMES = List.of("proto", "avro");

    /**
     * Creates code-generation configuration with the standard provider and input-directory conventions.
     */
    public QuarkusApplicationCodegen() {
        getProviders().convention(DEFAULT_PROVIDERS);
        getInputNames().convention(DEFAULT_INPUT_NAMES);
    }

    /**
     * Returns the provider identifiers whose input directories code generation discovers.
     *
     * @return the lazily configurable provider identifiers
     */
    public abstract ListProperty<String> getProviders();

    /**
     * Returns the directory names searched beneath each source root for code-generation inputs.
     *
     * @return the lazily configurable input-directory names
     */
    public abstract ListProperty<String> getInputNames();
}
