package io.quarkus.arc.config;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.quarkus.runtime.util.StringUtil;

/**
 * Allow configuration properties with a common prefix to be grouped into a single class
 *
 * @deprecated Please, use {@link io.smallrye.config.ConfigMapping} instead. This will be removed in a future Quarkus
 *             version.
 */
@Deprecated
@Target({ TYPE })
@Retention(RUNTIME)
public @interface ConfigProperties {

    String UNSET_PREFIX = "<< unset >>";
    boolean DEFAULT_FAIL_ON_MISMATCHING_MEMBER = true;

    /**
     * If the default is used, the class name will be used to determine the proper prefix
     */
    String prefix() default UNSET_PREFIX;

    /**
     * The naming strategy to use for the corresponding property. This only matters for fields or method names that contain
     * both lower case and upper case characters.
     *
     * {@code NamingStrategy.VERBATIM} means that whatever the name of the field / method is, that will be the name of the
     * property.
     * {@code NamingStrategy.KEBAB_CASE} means that the name of property is derived by replacing case changes with a dash.
     * For a example:
     *
     * /**
     * 
     * <pre>
     * &#64;ConfigProperties(prefix="whatever")
     * public class SomeConfig {
     *   public fooBar;
     * }
     * </pre>
     *
     * Then to set the {@code fooBar} field, the corresponding property would be {@code whatever.fooBar}.
     * If {@code namingStrategy=NamingStrategy.KEBAB_CASE} were being used, then the corresponding property would be
     * {@code whatever.foo-bar}
     *
     * When this field is not set, then the default strategy will be determined by the value of
     * quarkus.arc.config-properties-default-naming-strategy
     */
    NamingStrategy namingStrategy() default NamingStrategy.FROM_CONFIG;

    /**
     * Whether or not to fail when a non-public field of a class doesn't have a corresponding setter
     */
    boolean failOnMismatchingMember() default DEFAULT_FAIL_ON_MISMATCHING_MEMBER;

    enum NamingStrategy {
        FROM_CONFIG {
            @Override
            public String getName(String name) {
                throw new IllegalStateException("The naming strategy needs to substituted with the configured naming strategy");
            }
        },
        VERBATIM {
            @Override
            public String getName(String name) {
                return name;
            }
        },
        KEBAB_CASE {
            @Override
            public String getName(String name) {
                return StringUtil.hyphenate(name);
            }
        };

        public abstract String getName(String name);
    }

}
