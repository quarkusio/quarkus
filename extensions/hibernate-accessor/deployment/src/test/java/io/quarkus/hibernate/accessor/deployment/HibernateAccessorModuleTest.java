package io.quarkus.hibernate.accessor.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.quarkus.hibernate.accessor.deployment.HibernateAccessorRegistrationBuildItem.Builder;
import io.quarkus.hibernate.accessor.runtime.HibernateAccessorStrategy;
import io.quarkus.hibernate.accessor.runtime.QuarkusAccessContext;
import io.quarkus.modular.spi.items.ClassAccessBuildItem;
import io.quarkus.modular.spi.items.GeneratedClassPlacementBuildItem;

class HibernateAccessorModuleTest {

    @Test
    void generatedAccessTargetsBridgeAndPlacesItWithItsHost() throws IOException {
        var host = Index.of(Host.class).getClassByName(Host.class);
        var registration = new Builder(host).addFieldReader(host.field("value")).build();
        var registrations = List.of(registration, registration);
        var placements = placements(HibernateAccessorStrategy.GENERATED, registrations);
        String bridge = registration.getType().dispatchTarget();
        assertThat(bridge).isNotEqualTo(Host.class.getName());
        assertThat(placements).filteredOn(item -> item.generatedClass().equals(bridge)).singleElement()
                .extracting(GeneratedClassPlacementBuildItem::anchorClass).isEqualTo(Host.class.getName());
        assertThat(placements).filteredOn(item -> !item.generatedClass().equals(bridge))
                .hasSize(5).allSatisfy(item -> assertThat(item.anchorClass()).isEqualTo(QuarkusAccessContext.class.getName()));
        assertThat(accesses(HibernateAccessorStrategy.GENERATED, registrations)).hasSize(4).allSatisfy(item -> {
            assertThat(item.targetClass()).isEqualTo(bridge);
            assertThat(item.kind()).isEqualTo(ClassAccessBuildItem.Kind.DIRECT);
        });
    }

    @Test
    void publicHostDoesNotNeedBridgePlacement() throws IOException {
        var host = Index.of(PublicHost.class).getClassByName(PublicHost.class);
        var registration = new Builder(host).addFieldReader(host.field("value")).build();
        assertThat(placements(HibernateAccessorStrategy.GENERATED, List.of(registration)))
                .allSatisfy(item -> assertThat(item.anchorClass()).isEqualTo(QuarkusAccessContext.class.getName()));
        assertThat(accesses(HibernateAccessorStrategy.GENERATED, List.of(registration))).hasSize(4)
                .allSatisfy(item -> assertThat(item.targetClass()).isEqualTo(PublicHost.class.getName()));
    }

    @Test
    void reflectionTargetsMemberDeclaringClassAndGeneratesNoPlacement() throws IOException {
        var index = Index.of(Host.class, Declaring.class);
        var registration = new Builder(index.getClassByName(Host.class))
                .addGetter(index.getClassByName(Declaring.class).firstMethod("getValue")).build();
        var registrations = List.of(registration, registration);
        assertThat(placements(HibernateAccessorStrategy.REFLECTION, registrations)).isEmpty();
        assertThat(accesses(HibernateAccessorStrategy.REFLECTION, registrations)).singleElement().satisfies(item -> {
            assertThat(item.callerClass()).isEqualTo(QuarkusAccessContext.class.getName());
            assertThat(item.targetClass()).isEqualTo(Declaring.class.getName());
            assertThat(item.kind()).isEqualTo(ClassAccessBuildItem.Kind.REFLECTIVE);
        });
    }

    @Test
    void injectedDispatchCanAccessInheritedMemberDeclaringClass() throws IOException {
        var index = Index.of(Host.class, Declaring.class);
        var registration = new Builder(index.getClassByName(Host.class))
                .addGetter(index.getClassByName(Declaring.class).firstMethod("getValue")).build();
        var accesses = accesses(HibernateAccessorStrategy.GENERATED, List.of(registration));
        assertThat(accesses).hasSize(5).allSatisfy(item -> assertThat(item.kind()).isEqualTo(ClassAccessBuildItem.Kind.DIRECT));
        assertThat(accesses).filteredOn(item -> item.callerClass().equals(Host.class.getName())).singleElement()
                .extracting(ClassAccessBuildItem::targetClass).isEqualTo(Declaring.class.getName());
    }

    @Test
    void fallbackDeclaresBothDirectAndReflectiveAccess() throws IOException {
        var host = Index.of(Host.class).getClassByName(Host.class);
        var registration = new Builder(host).addFieldReader(host.field("value"))
                .addFieldWriter(host.field("value")).addDefaultConstructor().build();
        var accesses = accesses(HibernateAccessorStrategy.GENERATED_WITH_FALLBACK, List.of(registration));
        assertThat(accesses).filteredOn(item -> item.kind() == ClassAccessBuildItem.Kind.DIRECT).hasSize(4);
        assertThat(accesses).filteredOn(item -> item.kind() == ClassAccessBuildItem.Kind.REFLECTIVE).singleElement()
                .extracting(ClassAccessBuildItem::targetClass).isEqualTo(Host.class.getName());
    }

    @Test
    void emptyRegistrationStillPlacesSharedGeneratedClasses() {
        assertThat(placements(HibernateAccessorStrategy.GENERATED, List.of())).hasSize(5);
        assertThat(accesses(HibernateAccessorStrategy.GENERATED_WITH_FALLBACK, List.of())).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(HibernateAccessorStrategy.class)
    void mergedContributionsShareDeclaringClassesAcrossAccessKindsAndHosts(HibernateAccessorStrategy strategy)
            throws IOException {
        var index = Index.of(Host.class, OtherHost.class, Declaring.class);
        var host = index.getClassByName(Host.class);
        var declaring = index.getClassByName(Declaring.class);
        var reader = new Builder(host).addFieldReader(host.field("value"))
                .addGetter(declaring.firstMethod("getValue")).build();
        var writer = new Builder(host).addFieldWriter(host.field("value"))
                .addSetter(declaring.firstMethod("setValue")).addDefaultConstructor().build();
        var other = new Builder(index.getClassByName(OtherHost.class))
                .addGetter(declaring.firstMethod("getValue")).build();
        var processed = new HibernateAccessorProcessor().processRegistrations(List.of(reader, writer, reader, other));
        List<ClassAccessBuildItem> accesses = new ArrayList<>();
        HibernateAccessorProcessor.declareAccess(strategy, processed, accesses::add);
        var direct = accesses.stream().filter(item -> item.kind() == ClassAccessBuildItem.Kind.DIRECT).toList();
        var reflective = accesses.stream().filter(item -> item.kind() == ClassAccessBuildItem.Kind.REFLECTIVE).toList();
        if (strategy == HibernateAccessorStrategy.REFLECTION) {
            assertThat(direct).isEmpty();
        } else {
            assertThat(direct).hasSize(10);
            assertThat(direct).filteredOn(item -> item.targetClass().equals(Declaring.class.getName()))
                    .extracting(ClassAccessBuildItem::callerClass)
                    .containsExactlyInAnyOrder(Host.class.getName(), OtherHost.class.getName());
        }
        if (strategy == HibernateAccessorStrategy.GENERATED) {
            assertThat(reflective).isEmpty();
        } else {
            assertThat(reflective).extracting(ClassAccessBuildItem::targetClass)
                    .containsExactlyInAnyOrder(Host.class.getName(), Declaring.class.getName());
            assertThat(reflective).allSatisfy(item -> assertThat(item.callerClass())
                    .isEqualTo(QuarkusAccessContext.class.getName()));
        }
    }

    private static List<GeneratedClassPlacementBuildItem> placements(HibernateAccessorStrategy strategy,
            List<HibernateAccessorRegistrationBuildItem> registrations) {
        List<GeneratedClassPlacementBuildItem> result = new ArrayList<>();
        HibernateAccessorProcessor.placeGeneratedClasses(strategy,
                new HibernateAccessorProcessor().processRegistrations(registrations), result::add);
        return result;
    }

    private static List<ClassAccessBuildItem> accesses(HibernateAccessorStrategy strategy,
            List<HibernateAccessorRegistrationBuildItem> registrations) {
        List<ClassAccessBuildItem> result = new ArrayList<>();
        HibernateAccessorProcessor.declareAccess(strategy,
                new HibernateAccessorProcessor().processRegistrations(registrations), result::add);
        return result;
    }

    static class Host extends Declaring {
        private String value;
    }

    public static class PublicHost {
        private String value;
    }

    static class OtherHost extends Declaring {
    }

    static class Declaring {
        String getValue() {
            return "value";
        }

        void setValue(String value) {
        }
    }
}
