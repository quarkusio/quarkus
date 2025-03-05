package io.quarkus.runtime.init;

import org.eclipse.microprofile.config.spi.Converter;

import io.quarkus.runtime.annotations.ConfigDocPrefix;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.Converters;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;

/**
 * Initialization
 */
@ConfigMapping(prefix = "quarkus")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigDocPrefix("quarkus.init")
public interface InitRuntimeConfig {
    /**
     * true to quit exit right after the initialization.
     * The option is not meant be used directly by users.
     *
     */
    @WithDefault("false")
    @WithConverter(BooleanConverter.class)
    boolean initAndExit();

    // Because of https://github.com/eclipse/microprofile-config/issues/708
    Converter<Boolean> BOOLEAN_CONVERTER = Converters.getImplicitConverter(Boolean.class);

    class BooleanConverter implements Converter<Boolean> {
        @Override
        public Boolean convert(final String value) throws IllegalArgumentException, NullPointerException {
            return BOOLEAN_CONVERTER.convert(value);
        }
    }
}
