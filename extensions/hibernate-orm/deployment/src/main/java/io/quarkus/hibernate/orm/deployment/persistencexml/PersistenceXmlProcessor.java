package io.quarkus.hibernate.orm.deployment.persistencexml;

import static io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil.isHibernateValidatorPresent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.boot.spi.PersistenceXmlParser;
import org.jboss.logging.Logger;

import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.deployment.spi.DefaultDataSourceDbVersionBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfig;
import io.quarkus.hibernate.orm.deployment.HibernateOrmEnabled;
import io.quarkus.hibernate.orm.deployment.JpaModelPerPersistenceUnitBuildItem;
import io.quarkus.hibernate.orm.deployment.JpaPersistenceUnitModel;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDescriptorBuildItem;
import io.quarkus.hibernate.orm.deployment.PersistenceXmlDescriptorBuildItem;
import io.quarkus.hibernate.orm.deployment.model.JpaModelPersistenceUnitContributionBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.AdditionalPersistenceUnitBuildItem;
import io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.hibernate.orm.runtime.boot.QuarkusPersistenceUnitDescriptor;
import io.quarkus.hibernate.orm.runtime.recording.RecordedConfig;
import io.quarkus.hibernate.orm.runtime.service.FlatClassLoaderService;
import io.quarkus.runtime.configuration.ConfigurationException;

@BuildSteps(onlyIf = HibernateOrmEnabled.class)
final class PersistenceXmlProcessor {

    private static final Logger LOG = Logger.getLogger(PersistenceXmlProcessor.class);

    private static final String INTEGRATOR_SERVICE_FILE = "META-INF/services/org.hibernate.integrator.spi.Integrator";

    @BuildStep
    List<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles(HibernateOrmConfig config) {
        List<HotDeploymentWatchedFileBuildItem> watchedFiles = new ArrayList<>();
        if (!shouldIgnorePersistenceXmlResources(config)) {
            watchedFiles.add(new HotDeploymentWatchedFileBuildItem("META-INF/persistence.xml"));
        }
        watchedFiles.add(new HotDeploymentWatchedFileBuildItem(INTEGRATOR_SERVICE_FILE));

        // SQL load scripts are handled when assembling the Quarkus-configured persistence units

        return watchedFiles;
    }

    //Integration point: allow other extensions to define additional PersistenceXmlDescriptorBuildItem
    @BuildStep
    public void parsePersistenceXmlDescriptors(HibernateOrmConfig config,
            BuildProducer<PersistenceXmlDescriptorBuildItem> persistenceXmlDescriptorBuildItemBuildProducer) {
        if (!shouldIgnorePersistenceXmlResources(config)) {
            var parser = PersistenceXmlParser.create(Map.of(), null, FlatClassLoaderService.INSTANCE);
            var urls = parser.getClassLoaderService().locateResources("META-INF/persistence.xml");
            if (urls.isEmpty()) {
                return;
            }
            for (var desc : parser.parse(urls).values()) {
                persistenceXmlDescriptorBuildItemBuildProducer.produce(new PersistenceXmlDescriptorBuildItem(desc));
            }
        }
    }

    @BuildStep
    public void contributePersistenceXmlToJpaModel(
            BuildProducer<JpaModelPersistenceUnitContributionBuildItem> jpaModelPuContributions,
            List<PersistenceXmlDescriptorBuildItem> persistenceXmlDescriptors) {
        for (PersistenceXmlDescriptorBuildItem persistenceXmlDescriptor : persistenceXmlDescriptors) {
            org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor descriptor = persistenceXmlDescriptor.getDescriptor();
            jpaModelPuContributions.produce(new JpaModelPersistenceUnitContributionBuildItem(
                    descriptor.getName(), descriptor.getPersistenceUnitRootUrl(), descriptor.getManagedClassNames(),
                    descriptor.getMappingFileNames()));
        }
    }

    @BuildStep
    public void buildBlockingPersistenceUnitFromPersistenceXml(
            HibernateOrmConfig hibernateOrmConfig,
            List<PersistenceXmlDescriptorBuildItem> persistenceXmlDescriptors,
            List<JdbcDataSourceBuildItem> jdbcDataSources,
            List<AdditionalPersistenceUnitBuildItem> additionalPersistenceUnits,
            JpaModelPerPersistenceUnitBuildItem jpaModel,
            Capabilities capabilities,
            BuildProducer<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptors,
            List<DefaultDataSourceDbVersionBuildItem> defaultDbVersions) {
        // TODO move this validation to a dedicated method, preferably very early in the build?
        //   See also a conceptually similar check in contributeQuarkusConfigToJpaModel
        if (!additionalPersistenceUnits.isEmpty()) {
            Set<String> userConfiguredPersistenceUnitNames = new HashSet<>(hibernateOrmConfig.namedPersistenceUnits().keySet());
            for (PersistenceXmlDescriptorBuildItem persistenceXmlDescriptor : persistenceXmlDescriptors) {
                userConfiguredPersistenceUnitNames.add(persistenceXmlDescriptor.getDescriptor().getName());
            }
            for (AdditionalPersistenceUnitBuildItem additionalPersistenceUnit : additionalPersistenceUnits) {
                String persistenceUnitName = additionalPersistenceUnit.getPersistenceUnitName();
                if (PersistenceUnitUtil.isDefaultPersistenceUnit(persistenceUnitName)) {
                    throw new ConfigurationException(
                            "An extension attempted to contribute the default persistence unit through the SPI."
                                    + " Contributed persistence units must use a non-default name.");
                }
                if (userConfiguredPersistenceUnitNames.contains(persistenceUnitName)) {
                    throw new ConfigurationException(String.format(Locale.ROOT,
                            "Persistence unit '%s' is contributed by an extension but is also configured by the application."
                                    + " A persistence unit contributed through the SPI must use a name that is not already"
                                    + " configured through Quarkus configuration or a persistence.xml file.",
                            persistenceUnitName));
                }
            }
        }

        // Produce the PUs having a persistence.xml: these are not reactive, as we don't allow using a persistence.xml for them.
        for (PersistenceXmlDescriptorBuildItem persistenceXmlDescriptorBuildItem : persistenceXmlDescriptors) {
            PersistenceUnitDescriptor xmlDescriptor = persistenceXmlDescriptorBuildItem.getDescriptor();
            String puName = xmlDescriptor.getName();
            Optional<JdbcDataSourceBuildItem> jdbcDataSource = findDefaultDataSource(jdbcDataSources);
            var model = jpaModel.getModelPerPersistenceUnit().get(puName);
            if (model == null) {
                model = new JpaPersistenceUnitModel();
            }
            collectDialectConfigForPersistenceXml(puName, xmlDescriptor, defaultDbVersions);
            persistenceUnitDescriptors
                    .produce(new PersistenceUnitDescriptorBuildItem(
                            QuarkusPersistenceUnitDescriptor.validateAndReadFrom(xmlDescriptor),
                            new RecordedConfig(
                                    Optional.of(DataSourceUtil.DEFAULT_DATASOURCE_NAME),
                                    jdbcDataSource.map(JdbcDataSourceBuildItem::getDbKind),
                                    Optional.empty(),
                                    jdbcDataSource.flatMap(JdbcDataSourceBuildItem::getDbVersion),
                                    jdbcDataSource.map(JdbcDataSourceBuildItem::isDbVersionUserSpecified).orElse(false),
                                    Optional.ofNullable(xmlDescriptor.getProperties().getProperty(AvailableSettings.DIALECT)),
                                    Set.of(), // Not relevant for persistence.xml, because such a PU never gets deactivated.
                                    HibernateProcessorUtil.getMultiTenancyStrategy(
                                            Optional.ofNullable(persistenceXmlDescriptorBuildItem.getDescriptor()
                                                    .getProperties().getProperty("hibernate.multiTenancy"))), //FIXME this property is meaningless in Hibernate ORM 6
                                    hibernateOrmConfig.database().ormCompatibilityVersion(),
                                    Collections.emptyMap()),
                            model.xmlMappings(),
                            true, isHibernateValidatorPresent(capabilities)));
        }
    }

    private static Optional<JdbcDataSourceBuildItem> findDefaultDataSource(
            List<JdbcDataSourceBuildItem> jdbcDataSources) {
        return jdbcDataSources.stream()
                .filter(JdbcDataSourceBuildItem::isDefault)
                .findFirst();
    }

    private static void collectDialectConfigForPersistenceXml(String persistenceUnitName,
            PersistenceUnitDescriptor puDescriptor, List<DefaultDataSourceDbVersionBuildItem> defaultDbVersions) {
        Properties properties = puDescriptor.getProperties();
        String dialect = puDescriptor.getProperties().getProperty(AvailableSettings.DIALECT);
        // Legacy behavior: we used to do this through a custom DialectSelector,
        // but we might as well do it at build time.
        if (("H2".equals(dialect) || "org.hibernate.dialect.H2Dialect".equals(dialect))
                && !properties.containsKey(AvailableSettings.JAKARTA_HBM2DDL_DB_MAJOR_VERSION)
                && !properties.containsKey(AvailableSettings.JAKARTA_HBM2DDL_DB_MINOR_VERSION)
                && !properties.containsKey(AvailableSettings.JAKARTA_HBM2DDL_DB_VERSION)) {
            Optional<String> defaultH2Version = DefaultDataSourceDbVersionBuildItem.resolveDefaultDbVersion("h2",
                    defaultDbVersions);
            if (defaultH2Version.isPresent()) {
                LOG.infof("Persistence unit '%1$s': Enforcing Quarkus defaults for dialect 'org.hibernate.dialect.H2Dialect'"
                        + " by automatically setting '%2$s=%3$s'.",
                        persistenceUnitName, AvailableSettings.JAKARTA_HBM2DDL_DB_VERSION, defaultH2Version.get());
                properties.setProperty(AvailableSettings.JAKARTA_HBM2DDL_DB_VERSION, defaultH2Version.get());
            }
        }
    }

    /**
     * Checks whether we should ignore {@code persistence.xml} files.
     * <p>
     * The main way to ignore {@code persistence.xml} files is to set the configuration property
     * {@code quarkus.hibernate-orm.persistence-xml.ignore}.
     * <p>
     * But there is also an undocumented feature: we allow setting the System property
     * "SKIP_PARSE_PERSISTENCE_XML" to ignore any {@code persistence.xml} resource.
     *
     * @return true if we're expected to ignore them
     */
    private boolean shouldIgnorePersistenceXmlResources(HibernateOrmConfig config) {
        return config.persistenceXml().ignore() || Boolean.getBoolean("SKIP_PARSE_PERSISTENCE_XML");
    }

}
