package io.quarkus.runtime.init;

import static io.smallrye.config.Converters.newEmptyValueConverter;

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
    // TODO - radcortez - Make SR Config built-in converters public to be used directly
    class BooleanConverter implements Converter<Boolean> {
        static final Converter<Boolean> BOOLEAN_CONVERTER = Converters
                .newTrimmingConverter(newEmptyValueConverter(new Converter<Boolean>() {
                    @Override
                    public Boolean convert(final String value) throws IllegalArgumentException, NullPointerException {
                        return Boolean.valueOf(
                                "TRUE".equalsIgnoreCase(value)
                                        || "1".equalsIgnoreCase(value)
                                        || "YES".equalsIgnoreCase(value)
                                        || "Y".equalsIgnoreCase(value)
                                        || "ON".equalsIgnoreCase(value)
                                        || "JA".equalsIgnoreCase(value)
                                        || "J".equalsIgnoreCase(value)
                                        || "SI".equalsIgnoreCase(value)
                                        || "SIM".equalsIgnoreCase(value)
                                        || "OUI".equalsIgnoreCase(value));
                    }
                }));

        @Override
        public Boolean convert(final String value) throws IllegalArgumentException, NullPointerException {
            return BOOLEAN_CONVERTER.convert(value);
        }
    }
}
