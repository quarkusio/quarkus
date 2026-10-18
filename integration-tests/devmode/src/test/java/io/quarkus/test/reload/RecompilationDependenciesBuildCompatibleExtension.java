package io.quarkus.test.reload;

import java.util.Map;
import java.util.Set;

import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.Discovery;

import org.jboss.jandex.DotName;

import io.quarkus.deployment.dev.RuntimeUpdatesProcessor;

public class RecompilationDependenciesBuildCompatibleExtension implements BuildCompatibleExtension {
    @Discovery
    void x() {
        RuntimeUpdatesProcessor.INSTANCE.setClassToRecompilationTargets(Map.of(//
                DotName.createSimple(AddressData.class), Set.of(DotName.createSimple(AddressMapper.class)), //
                DotName.createSimple(ContactData.class), Set.of(DotName.createSimple(AddressMapper.class))//
        ));
    }
}
