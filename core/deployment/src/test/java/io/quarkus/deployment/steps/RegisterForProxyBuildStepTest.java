package io.quarkus.deployment.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;

import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.runtime.annotations.RegisterForProxy;

class RegisterForProxyBuildStepTest {

    interface Alpha {
    }

    interface Beta extends Alpha {
    }

    // Repeated @RegisterForProxy: the compiler wraps these in a generated @RegisterForProxy.List container.
    @RegisterForProxy(targets = Alpha.class)
    @RegisterForProxy(targets = Beta.class)
    interface RepeatedTargets {
    }

    @RegisterForProxy(targets = { Alpha.class, Beta.class })
    interface CombinedTargets {
    }

    @RegisterForProxy
    interface NoTargets extends Alpha {
    }

    @Test
    void registersEveryRepeatedAnnotation() throws IOException {
        assertThat(runBuildStep(RepeatedTargets.class))
                .containsExactlyInAnyOrder(
                        List.of(Alpha.class.getName()),
                        List.of(Beta.class.getName()));
    }

    @Test
    void registersMultipleTargetsAsSingleProxy() throws IOException {
        assertThat(runBuildStep(CombinedTargets.class))
                .containsExactly(List.of(Alpha.class.getName(), Beta.class.getName()));
    }

    @Test
    void registersAnnotatedTypeWithItsSuperInterfaces() throws IOException {
        assertThat(runBuildStep(NoTargets.class))
                .containsExactly(List.of(NoTargets.class.getName(), Alpha.class.getName()));
    }

    private static List<List<String>> runBuildStep(Class<?>... classes) throws IOException {
        Index index = Index.of(classes);
        List<NativeImageProxyDefinitionBuildItem> produced = new ArrayList<>();
        new RegisterForProxyBuildStep().build(new CombinedIndexBuildItem(index, index), produced::add);
        return produced.stream()
                .map(NativeImageProxyDefinitionBuildItem::getClasses)
                .collect(Collectors.toList());
    }
}
