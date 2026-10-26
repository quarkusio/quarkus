package io.quarkus.hibernate.orm.deployment.persistencexml;

import static io.quarkus.hibernate.orm.deployment.ProcessorTestStubs.emptyJpaModel;
import static io.quarkus.hibernate.orm.deployment.ProcessorTestStubs.hibernateORMConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.PersistenceUnitTransactionType;

import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.junit.jupiter.api.Test;

import io.quarkus.deployment.Capabilities;
import io.quarkus.hibernate.orm.deployment.JpaModelPerPersistenceUnitBuildItem;
import io.quarkus.hibernate.orm.deployment.PersistenceXmlDescriptorBuildItem;
import io.quarkus.hibernate.orm.deployment.model.JpaModelPersistenceUnitContributionBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.AdditionalPersistenceUnitBuildItem;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.runtime.util.Reason;

class PersistenceXmlProcessorTest {

    private static final String INTEGRATOR_SERVICE_FILE = "META-INF/services/org.hibernate.integrator.spi.Integrator";

    private final PersistenceXmlProcessor processor = new PersistenceXmlProcessor();

    @Test
    void defaultValues() {
        var watchedFiles = processor.hotDeploymentWatchedFiles(hibernateORMConfig(Map.of()));

        assertThat(watchedFiles).extracting(item -> item.getLocation())
                .containsExactly("META-INF/persistence.xml", INTEGRATOR_SERVICE_FILE);

        assertThat(watchedFiles).allSatisfy(item -> assertThat(item.isRestartNeeded()).isTrue());
    }

    @Test
    void stopsWatchingPersistenceXmlWhenItIsIgnoredByConfiguration() {
        var config = hibernateORMConfig(Map.of("quarkus.hibernate-orm.persistence-xml.ignore", "true"));

        var watchedFiles = processor.hotDeploymentWatchedFiles(config);

        assertThat(watchedFiles).extracting(item -> item.getLocation())
                .doesNotContain("META-INF/persistence.xml");
        assertThat(watchedFiles).extracting(item -> item.getLocation())
                .contains(INTEGRATOR_SERVICE_FILE);
    }

    @Test
    void stopsWatchingPersistenceXmlWhenItIsIgnoredByTheLegacySystemProperty() {
        String previous = System.getProperty("SKIP_PARSE_PERSISTENCE_XML");
        try {
            System.setProperty("SKIP_PARSE_PERSISTENCE_XML", "true");

            var watchedFiles = processor.hotDeploymentWatchedFiles(hibernateORMConfig(Map.of()));

            assertThat(watchedFiles).extracting(item -> item.getLocation()).containsExactly(INTEGRATOR_SERVICE_FILE);
        } finally {
            if (previous == null) {
                System.clearProperty("SKIP_PARSE_PERSISTENCE_XML");
            } else {
                System.setProperty("SKIP_PARSE_PERSISTENCE_XML", previous);
            }
        }
    }

    @Test
    void contributesEveryParsedDescriptorToJpaModelDiscovery() throws MalformedURLException {
        var inventory = xmlDescriptor("inventory", DefaultModel.class.getName(), "META-INF/inventory.orm.xml");
        var users = xmlDescriptor("users", UserModel.class.getName(), "META-INF/users.orm.xml");
        var contributions = new ArrayList<JpaModelPersistenceUnitContributionBuildItem>();

        processor.contributePersistenceXmlToJpaModel(contributions::add,
                List.of(new PersistenceXmlDescriptorBuildItem(inventory), new PersistenceXmlDescriptorBuildItem(users)));

        assertThat(contributions).hasSize(2);
        var inventoryContribution = contributionForPersistenceUnit(contributions, "inventory");
        assertThat(inventoryContribution.persistenceUnitRootURL).isEqualTo(inventory.getPersistenceUnitRootUrl());
        assertThat(inventoryContribution.explicitlyListedClassNames).containsExactly(DefaultModel.class.getName());
        assertThat(inventoryContribution.explicitlyListedMappingFiles).containsExactly("META-INF/inventory.orm.xml");

        var usersContribution = contributionForPersistenceUnit(contributions, "users");
        assertThat(usersContribution.persistenceUnitRootURL).isEqualTo(users.getPersistenceUnitRootUrl());
        assertThat(usersContribution.explicitlyListedClassNames).containsExactly(UserModel.class.getName());
        assertThat(usersContribution.explicitlyListedMappingFiles).containsExactly("META-INF/users.orm.xml");
    }

    @Test
    void rejectsAnExtensionContributedDefaultPersistenceUnit() {
        var additional = AdditionalPersistenceUnitBuildItem
                .builder(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME, new Reason("test")).build();

        assertThatThrownBy(() -> buildPersistenceUnits(List.of(), List.of(additional)))
                .isInstanceOf(ConfigurationException.class)
                .hasMessage("An extension attempted to contribute the default persistence unit through the SPI."
                        + " Contributed persistence units must use a non-default name.");
    }

    @Test
    void rejectsAnExtensionContributedPersistenceUnitThatDuplicatesPersistenceXml() throws MalformedURLException {
        var descriptor = new PersistenceXmlDescriptorBuildItem(
                xmlDescriptor("inventory", DefaultModel.class.getName(), "META-INF/inventory.orm.xml"));
        var additional = AdditionalPersistenceUnitBuildItem.builder("inventory", new Reason("test")).build();

        assertThatThrownBy(() -> buildPersistenceUnits(List.of(descriptor), List.of(additional)))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("Persistence unit 'inventory' is contributed by an extension")
                .hasMessageContaining("also configured by the application");
    }

    private void buildPersistenceUnits(List<PersistenceXmlDescriptorBuildItem> descriptors,
            List<AdditionalPersistenceUnitBuildItem> additional) {
        processor.buildBlockingPersistenceUnitFromPersistenceXml(hibernateORMConfig(Map.of()), descriptors, List.of(),
                additional, emptyJpaModel(), new JpaModelPerPersistenceUnitBuildItem(Map.of(), Set.of(), Set.of(), Set.of()),
                new Capabilities(Set.of()), ignored -> {
                }, List.of());
    }

    private static JpaModelPersistenceUnitContributionBuildItem contributionForPersistenceUnit(
            List<JpaModelPersistenceUnitContributionBuildItem> contributions, String persistenceUnitName) {
        return contributions.stream()
                .filter(item -> item.persistenceUnitName.equals(persistenceUnitName))
                .findFirst()
                .orElseThrow();
    }

    private static ParsedPersistenceXmlDescriptor xmlDescriptor(String name, String managedClass, String mappingFile)
            throws MalformedURLException {
        var descriptor = new ParsedPersistenceXmlDescriptor(URI.create("file:/" + name + "/").toURL());
        descriptor.setName(name);
        descriptor.setTransactionType(PersistenceUnitTransactionType.JTA);
        descriptor.addClasses(managedClass);
        descriptor.addMappingFiles(mappingFile);
        return descriptor;
    }

    static class DefaultModel {
    }

    static class UserModel {
    }
}
