package io.quarkus.hibernate.orm.deployment.spi;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Requests the creation of a (static) Hibernate ORM persistence unit that is <em>not</em> declared in
 * user-provided configuration, i.e. neither through {@code quarkus.hibernate-orm.*} properties nor through
 * a {@code persistence.xml} file.
 * <p>
 * This is the supported way for an extension to contribute a persistence unit of its own: the extension
 * declares its intent (name, datasource, managed classes, mapping files and a few extra properties) and
 * the Hibernate ORM extension takes care of the rest (datasource and dialect resolution, descriptor
 * recording, CDI bean generation).
 */
public final class AdditionalPersistenceUnitBuildItem extends MultiBuildItem {

    private final String persistenceUnitName;
    private final Optional<String> dataSourceName;
    private final Optional<String> explicitDialect;
    private final Set<String> managedClassNames;
    private final Set<String> mappingFileNames;
    private final Map<String, String> properties;

    private AdditionalPersistenceUnitBuildItem(Builder builder) {
        this.persistenceUnitName = builder.persistenceUnitName;
        this.dataSourceName = builder.dataSourceName;
        this.explicitDialect = builder.explicitDialect;
        this.managedClassNames = Collections.unmodifiableSet(new LinkedHashSet<>(builder.managedClassNames));
        this.mappingFileNames = Collections.unmodifiableSet(new LinkedHashSet<>(builder.mappingFileNames));
        this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(builder.properties));
    }

    public String getPersistenceUnitName() {
        return persistenceUnitName;
    }

    public Optional<String> getDataSourceName() {
        return dataSourceName;
    }

    public Optional<String> getExplicitDialect() {
        return explicitDialect;
    }

    public Set<String> getManagedClassNames() {
        return managedClassNames;
    }

    public Set<String> getMappingFileNames() {
        return mappingFileNames;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public static Builder builder(String persistenceUnitName) {
        return new Builder(persistenceUnitName);
    }

    public static final class Builder {

        private final String persistenceUnitName;
        private Optional<String> dataSourceName = Optional.empty();
        private Optional<String> explicitDialect = Optional.empty();
        private final Set<String> managedClassNames = new LinkedHashSet<>();
        private final Set<String> mappingFileNames = new LinkedHashSet<>();
        private final Map<String, String> properties = new LinkedHashMap<>();

        private Builder(String persistenceUnitName) {
            Objects.requireNonNull(persistenceUnitName, "persistenceUnitName must not be null");
            if (persistenceUnitName.isBlank()) {
                throw new IllegalArgumentException("persistenceUnitName must not be blank");
            }
            this.persistenceUnitName = persistenceUnitName;
        }

        /**
         * Sets the datasource backing this persistence unit. When not set (or set to {@code null}), the default
         * datasource is used.
         *
         * @param dataSourceName The name of the datasource backing this persistence unit.
         * @return This builder.
         */
        public Builder dataSourceName(String dataSourceName) {
            this.dataSourceName = Optional.ofNullable(dataSourceName);
            return this;
        }

        /**
         * Sets the Hibernate ORM dialect for this persistence unit. When not set, the dialect is auto-detected
         * from the datasource's database kind.
         *
         * @param dialect The fully-qualified class name of the Hibernate dialect.
         * @return This builder.
         */
        public Builder dialect(String dialect) {
            this.explicitDialect = Optional.ofNullable(dialect);
            return this;
        }

        /**
         * Adds a managed class to this persistence unit. The class is automatically added to the JPA model and
         * assigned to this persistence unit by the Hibernate ORM extension.
         *
         * @param className The fully-qualified name of the managed class.
         * @return This builder.
         */
        public Builder managedClass(String className) {
            this.managedClassNames.add(Objects.requireNonNull(className, "className must not be null"));
            return this;
        }

        /**
         * Adds managed classes to this persistence unit. Each class is automatically added to the JPA model and
         * assigned to this persistence unit by the Hibernate ORM extension.
         *
         * @param classNames The fully-qualified names of the managed classes.
         * @return This builder.
         */
        public Builder managedClasses(Collection<String> classNames) {
            this.managedClassNames.addAll(Objects.requireNonNull(classNames, "classNames must not be null"));
            return this;
        }

        /**
         * Adds a mapping file to this persistence unit.
         *
         * @param mappingFileName The name of the mapping file resource.
         * @return This builder.
         */
        public Builder mappingFile(String mappingFileName) {
            this.mappingFileNames.add(Objects.requireNonNull(mappingFileName, "mappingFileName must not be null"));
            return this;
        }

        /**
         * Adds mapping files to this persistence unit.
         *
         * @param mappingFileNames The names of the mapping file resources.
         * @return This builder.
         */
        public Builder mappingFiles(Collection<String> mappingFileNames) {
            this.mappingFileNames.addAll(Objects.requireNonNull(mappingFileNames, "mappingFileNames must not be null"));
            return this;
        }

        /**
         * Sets an additional Hibernate ORM property on this persistence unit.
         *
         * @param key The property key.
         * @param value The property value.
         * @return This builder.
         */
        public Builder property(String key, String value) {
            this.properties.put(Objects.requireNonNull(key, "key must not be null"), value);
            return this;
        }

        /**
         * Sets additional Hibernate ORM properties on this persistence unit.
         *
         * @param properties The properties to set.
         * @return This builder.
         */
        public Builder properties(Map<String, String> properties) {
            this.properties.putAll(Objects.requireNonNull(properties, "properties must not be null"));
            return this;
        }

        public AdditionalPersistenceUnitBuildItem build() {
            return new AdditionalPersistenceUnitBuildItem(this);
        }
    }
}
