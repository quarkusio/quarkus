package io.quarkus.hibernate.orm.runtime.boot;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;

import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.spi.ClassTransformer;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.internal.util.PersistenceUnitTransactionTypeHelper;

import io.quarkus.runtime.annotations.RecordableConstructor;

public final class QuarkusPersistenceUnitDescriptor implements PersistenceUnitDescriptor {

    private final String name;
    // The default PU in Hibernate Reactive is named "default-reactive" instead of "<default>",
    // but everything related to configuration (e.g. getAllPersistenceUnitConfigsAsMap() still
    // use the name "<default>", so we need to convert between those.
    private final String configurationName;
    private final String providerClassName;
    private final boolean useQuotedIdentifiers;
    private final PersistenceUnitTransactionType persistenceUnitTransactionType;
    private final ValidationMode validationMode;
    private final SharedCacheMode sharedCacheMode;
    private final List<String> managedClassNames;
    private final Properties properties;
    private final boolean reactive;

    public QuarkusPersistenceUnitDescriptor(String name, String configurationName,
            PersistenceUnitTransactionType persistenceUnitTransactionType,
            List<String> managedClassNames,
            Properties properties, boolean reactive) {
        this.name = name;
        this.configurationName = configurationName;
        this.providerClassName = null;
        this.useQuotedIdentifiers = false;
        this.persistenceUnitTransactionType = persistenceUnitTransactionType;
        this.validationMode = null;
        this.sharedCacheMode = null;
        this.managedClassNames = managedClassNames;
        this.properties = properties;
        this.reactive = reactive;
    }

    /**
     * @deprecated Do not use directly: this should be considered an internal constructor,
     *             as we're trusting all parameters.
     *             Useful for serialization to bytecode (which requires the constructor to be public).
     */
    @Deprecated
    @RecordableConstructor
    public QuarkusPersistenceUnitDescriptor(String name, String configurationName,
            String providerClassName, boolean useQuotedIdentifiers,
            PersistenceUnitTransactionType persistenceUnitTransactionType,
            ValidationMode validationMode, SharedCacheMode sharedCacheMode, List<String> managedClassNames,
            Properties properties, boolean reactive) {
        this.name = name;
        this.configurationName = configurationName;
        this.providerClassName = providerClassName;
        this.useQuotedIdentifiers = useQuotedIdentifiers;
        this.persistenceUnitTransactionType = persistenceUnitTransactionType;
        this.validationMode = validationMode;
        this.sharedCacheMode = sharedCacheMode;
        this.managedClassNames = managedClassNames;
        this.properties = properties;
        this.reactive = reactive;
    }

    /**
     * Converts a generic PersistenceUnitDescriptor into one of this specific type, and validates that
     * several options that Quarkus does not support are not set.
     *
     * @param toClone the descriptor to clone
     * @return a new instance of LightPersistenceXmlDescriptor
     * @throws UnsupportedOperationException on unsupported configurations
     */
    @SuppressWarnings("deprecated")
    public static QuarkusPersistenceUnitDescriptor validateAndReadFrom(PersistenceUnitDescriptor toClone) {
        if (toClone instanceof QuarkusPersistenceUnitDescriptor) {
            return (QuarkusPersistenceUnitDescriptor) toClone;
        }
        Objects.requireNonNull(toClone);
        verifyIgnoredFields(toClone);
        return new QuarkusPersistenceUnitDescriptor(toClone.getName(), toClone.getName(), toClone.getProviderClassName(),
                toClone.isUseQuotedIdentifiers(),
                toClone.getPersistenceUnitTransactionType(), toClone.getValidationMode(), toClone.getSharedCacheMode(),
                Collections.unmodifiableList(toClone.getManagedClassNames()), toClone.getProperties(), false);
    }

    @Override
    public URL getPersistenceUnitRootUrl() {
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getConfigurationName() {
        return configurationName;
    }

    @Override
    public String getProviderClassName() {
        return providerClassName;
    }

    @Override
    public boolean isUseQuotedIdentifiers() {
        return useQuotedIdentifiers;
    }

    @Override
    public boolean isExcludeUnlistedClasses() {
        // enforced
        return true;
    }

    @Override
    public PersistenceUnitTransactionType getPersistenceUnitTransactionType() {
        return persistenceUnitTransactionType;
    }

    @Override
    @Deprecated
    @SuppressWarnings("removal")
    public jakarta.persistence.spi.PersistenceUnitTransactionType getTransactionType() {
        return PersistenceUnitTransactionTypeHelper.toDeprecatedForm(getPersistenceUnitTransactionType());
    }

    @Override
    public ValidationMode getValidationMode() {
        return validationMode;
    }

    @Override
    public SharedCacheMode getSharedCacheMode() {
        return sharedCacheMode;
    }

    @Override
    public List<String> getManagedClassNames() {
        return managedClassNames;
    }

    @Override
    public List<String> getMappingFileNames() {
        // Mapping files can safely be ignored, see verifyIgnoredFields().
        return Collections.emptyList();
    }

    @Override
    public List<URL> getJarFileUrls() {
        return Collections.emptyList();
    }

    @Override
    public Object getNonJtaDataSource() {
        return null;
    }

    @Override
    public Object getJtaDataSource() {
        // TODO: we should include the name of the datasource
        return null;
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

    @Override
    public ClassLoader getClassLoader() {
        // enforced
        return null;
    }

    @Override
    public ClassLoader getTempClassLoader() {
        // enforced
        return null;
    }

    public boolean isReactive() {
        return reactive;
    }

    @Override
    public void pushClassTransformer(final EnhancementContext enhancementContext) {
        // has never been supported
    }

    private static void verifyIgnoredFields(final PersistenceUnitDescriptor toClone) {
        // This one needs to be ignored:
        // if ( toClone.getPersistenceUnitRootUrl() != null ) {
        // throw new UnsupportedOperationException( "Value found for
        // #getPersistenceUnitRootUrl : not supported yet" );
        // }

        // getMappingFiles() is ignored and replaced with an empty list,
        // because we don't need Hibernate ORM to parse the mappings files:
        // they are parsed at compile time and side-loaded during static init.

        if (toClone.getJarFileUrls() != null && !toClone.getJarFileUrls().isEmpty()) {
            throw new UnsupportedOperationException("Value found for #getJarFileUrls : not supported yet");
        }
        if (toClone.getJtaDataSource() != null) {
            throw new UnsupportedOperationException("Value found for #getJtaDataSource : not supported yet");
        }
        if (toClone.getNonJtaDataSource() != null) {
            throw new UnsupportedOperationException("Value found for #getNonJtaDataSource : not supported");
        }
    }

    @Override
    public String toString() {
        return "QuarkusPersistenceUnitDescriptor{" +
                "name='" + name + '\'' +
                ", configurationName='" + configurationName + '\'' +
                ", providerClassName='" + providerClassName + '\'' +
                ", useQuotedIdentifiers=" + useQuotedIdentifiers +
                ", transactionType=" + persistenceUnitTransactionType +
                ", validationMode=" + validationMode +
                ", sharedCacheMode=" + sharedCacheMode +
                ", managedClassNames=" + managedClassNames +
                ", properties=" + properties +
                ", isReactive=" + reactive +
                '}';
    }

    @Override
    public ClassTransformer getClassTransformer() {
        // We transform classes during the build, not on bootstrap.
        return null;
    }
}
