package io.quarkus.hibernate.orm.runtime.customized;

import io.quarkus.runtime.configuration.ConfigurationException;

@Deprecated(forRemoval = true, since = "4.0")
public enum BuiltinFormatMapperBehaviour {
    /**
     * Currently the default one. The Quarkus preconfigured mappers are ignored and if there is no user provided one,
     * Hibernate ORM will create a mapper according to its own rules.
     *
     * @asciidoclet
     */
    IGNORE {
        @Override
        public void action() {
        }
    },
    /**
     * A no longer supported option. Using it will result in the application failing at build time.
     *
     * @asciidoclet
     */
    WARN {
        @Override
        public void action() {
            fail();
        }
    },
    /**
     * A no longer supported option. Using it will result in the application failing at build time.
     *
     * @asciidoclet
     */
    FAIL {
        @Override
        public void action() {
            fail();
        }
    };

    private static void fail() {
        throw new ConfigurationException(
                "The 'quarkus.hibernate-orm.mapping.format.global' configuration property is deprecated"
                        + " and only accepts 'ignore'. Quarkus no longer pre-builds format mappers."
                        + " Either define a custom `FormatMapper` bean or let Hibernate ORM create its own internally."
                        + " Refer to https://quarkus.io/guides/hibernate-orm#json_xml_serialization_deserialization for guidance."
                        + " Remove this property or set it to 'ignore'.");
    }

    public abstract void action();
}
