package io.quarkus.deployment.configuration;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.spi.Converter;
import org.wildfly.common.Assert;
import org.wildfly.common.annotation.NotNull;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.runtime.configuration.ExpandingConfigSource;
import io.quarkus.runtime.configuration.NameIterator;
import io.smallrye.config.SmallRyeConfig;

/**
 * A node which contains a regular value. Leaf nodes can never be directly acquired.
 */
public abstract class LeafConfigType extends ConfigType {
    static final MethodDescriptor CU_CONVERT = MethodDescriptor.ofMethod(ConfigUtils.class, "convert", Object.class,
            SmallRyeConfig.class, String.class, Class.class, Class.class);
    static final MethodDescriptor CU_GET_VALUE = MethodDescriptor.ofMethod(ConfigUtils.class, "getValue", Object.class,
            SmallRyeConfig.class, String.class, Class.class, Class.class);
    static final MethodDescriptor CU_GET_OPT_VALUE = MethodDescriptor.ofMethod(ConfigUtils.class, "getOptionalValue",
            Optional.class, SmallRyeConfig.class, String.class, Class.class, Class.class);

    private final String javadocKey;
    private final String configKey;

    LeafConfigType(final String containingName, final CompoundConfigType container, final boolean consumeSegment,
            String javadocKey, String configKey) {
        super(containingName, container, consumeSegment);
        this.javadocKey = javadocKey;
        this.configKey = configKey;
    }

    /**
     *
     * @return the key that the javadoc was saved under
     */
    public String getJavadocKey() {
        return javadocKey;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void load() {
    }

    /**
     * Get the class of the individual item. This is the unwrapped type of {@code Optional}, {@code Collection}, etc.
     *
     * @return the item class (must not be {@code null})
     */
    public abstract Class<?> getItemClass();

    /**
     * Handle a configuration key from the input file.
     *
     * @param name the configuration property name
     * @param cache
     * @param config the source configuration
     */
    public abstract void acceptConfigurationValue(@NotNull NameIterator name, final ExpandingConfigSource.Cache cache,
            @NotNull SmallRyeConfig config);

    public abstract void generateAcceptConfigurationValue(BytecodeCreator body, ResultHandle name, final ResultHandle cache,
            ResultHandle config);

    abstract void acceptConfigurationValueIntoGroup(Object enclosing, Field field, NameIterator name, SmallRyeConfig config);

    abstract void generateAcceptConfigurationValueIntoGroup(BytecodeCreator body, ResultHandle enclosing,
            final MethodDescriptor setter, ResultHandle name, ResultHandle config);

    void acceptConfigurationValueIntoMap(Map<String, Object> enclosing, NameIterator name, SmallRyeConfig config) {
        // only non-primitives are supported
        throw Assert.unsupported();
    }

    void generateAcceptConfigurationValueIntoMap(BytecodeCreator body, ResultHandle enclosing,
            ResultHandle name, ResultHandle config) {
        throw Assert.unsupported();
    }

    public abstract String getDefaultValueString();

    public abstract Class<? extends Converter<?>> getConverterClass();

    protected final ResultHandle loadConverterClass(BytecodeCreator body) {
        Class<? extends Converter<?>> converterClass = getConverterClass();
        ResultHandle converter = body.loadNull();
        if (converterClass != null) {
            converter = body.loadClass(converterClass);
        }
        return converter;
    }
}
