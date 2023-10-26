package io.quarkus.hibernate.orm.runtime.boot;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.spi.ClassTransformer;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

import io.quarkus.runtime.annotations.RecordableConstructor;

public final class RuntimePersistenceUnitDescriptor implements PersistenceUnitDescriptor {

    private final String name;
    private final String configurationName;
    private final String providerClassName;
    private final boolean useQuotedIdentifiers;
    private final PersistenceUnitTransactionType transactionType;
    private final ValidationMode validationMode;
    private final SharedCacheMode sharedCacheMode;
    private final List<String> managedClassNames;
    private final Properties properties;

    /**
     * @deprecated Do not use directly: this should be considered an internal constructor,
     *             as we're trusting all parameters.
     *             Useful for serialization to bytecode (which requires the constructor to be public).
     */
    @Deprecated
    @RecordableConstructor
    public RuntimePersistenceUnitDescriptor(String name, String configurationName,
            String providerClassName, boolean useQuotedIdentifiers,
            PersistenceUnitTransactionType transactionType,
            ValidationMode validationMode, SharedCacheMode sharedCacheMode, List<String> managedClassNames,
            Properties properties) {
        this.name = name;
        this.configurationName = configurationName;
        this.providerClassName = providerClassName;
        this.useQuotedIdentifiers = useQuotedIdentifiers;
        this.transactionType = transactionType;
        this.validationMode = validationMode;
        this.sharedCacheMode = sharedCacheMode;
        this.managedClassNames = managedClassNames;
        this.properties = properties;
    }

    /**
     * Converts a generic PersistenceUnitDescriptor into one of this specific type, and validates that
     * several options that Quarkus does not support are not set.
     *
     * @param toClone the descriptor to clone
     * @param configurationName the name of this PU in Quarkus configuration
     * @return a new instance of LightPersistenceXmlDescriptor
     * @throws UnsupportedOperationException on unsupported configurations
     */
    @SuppressWarnings("deprecated")
    public static RuntimePersistenceUnitDescriptor validateAndReadFrom(PersistenceUnitDescriptor toClone,
            String configurationName) {
        Objects.requireNonNull(toClone);
        verifyIgnoredFields(toClone);
        return new RuntimePersistenceUnitDescriptor(toClone.getName(), configurationName, toClone.getProviderClassName(),
                toClone.isUseQuotedIdentifiers(),
                toClone.getTransactionType(), toClone.getValidationMode(), toClone.getSharedCacheMode(),
                Collections.unmodifiableList(toClone.getManagedClassNames()), toClone.getProperties());
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
    public PersistenceUnitTransactionType getTransactionType() {
        return transactionType;
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
        return "PersistenceUnitDescriptor{" + "name='" + name + '\'' + ", providerClassName='" + providerClassName
                + '\'' + ", useQuotedIdentifiers=" + useQuotedIdentifiers + ", transactionType=" + transactionType
                + ", validationMode=" + validationMode + ", sharedCachemode=" + sharedCacheMode + ", managedClassNames="
                + managedClassNames + ", properties=" + properties + '}';
    }

    @Override
    public ClassTransformer getClassTransformer() {
        // We transform classes during the build, not on bootstrap.
        return null;
    }
}
