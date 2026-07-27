package io.quarkus.core.deployment.action.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.deployment.builditem.MainBytecodeRecorderBuildItem;
import io.quarkus.deployment.builditem.StaticBytecodeRecorderBuildItem;
import io.quarkus.deployment.recording.BytecodeRecorderImpl;
import io.quarkus.runtime.RuntimeValue;

/**
 * Tests for the service-to-recorder bridge methods in {@link ActionBuilderImpl}.
 * <p>
 * Only the {@code serviceAsRuntimeValue} family is tested here because
 * {@code serviceAsRecorderValue} requires the deployment classloader
 * ({@code visibleDefineClass}) for {@link io.quarkus.deployment.proxy.ProxyFactory}.
 * Full end-to-end testing of {@code serviceAsRecorderValue} is covered by
 * integration tests during extension migration.
 */
class ActionBuilderImplTest {

    @Test
    void serviceAsRuntimeValueReturnsReturnedProxy() {
        List<MainBytecodeRecorderBuildItem> recorderItems = new ArrayList<>();
        ActionBuilderImpl builder = createBuilder(recorderItems, new ArrayList<>());

        RuntimeValue<CharSequence> rv = builder.serviceAsRuntimeValue(CharSequence.class);

        assertThat(rv).isInstanceOf(BytecodeRecorderImpl.ReturnedProxy.class);
        BytecodeRecorderImpl.ReturnedProxy proxy = (BytecodeRecorderImpl.ReturnedProxy) rv;
        assertThat(proxy.__returned$proxy$key()).isEqualTo("runtimevalue:java.lang.CharSequence:");
        assertThat(proxy.__static$$init()).isFalse();
    }

    @Test
    void serviceAsRuntimeValueProducesWrapperAction() {
        List<MainBytecodeRecorderBuildItem> recorderItems = new ArrayList<>();
        ActionBuilderImpl builder = createBuilder(recorderItems, new ArrayList<>());

        builder.serviceAsRuntimeValue(CharSequence.class);

        assertThat(recorderItems).hasSize(1);
        TransliteratedAction ta = recorderItems.get(0).getTransliteratedAction();
        assertThat(ta).isInstanceOf(TransliteratedAction.RuntimeValueWrapper.class);
        TransliteratedAction.RuntimeValueWrapper rvw = (TransliteratedAction.RuntimeValueWrapper) ta;
        assertThat(rvw.sourceServiceKey()).isEqualTo("java.lang.CharSequence:");
        assertThat(rvw.rvKey()).isEqualTo("runtimevalue:java.lang.CharSequence:");
        assertThat(rvw.staticInit()).isFalse();
    }

    @Test
    void namedServiceAsRuntimeValueUsesNameInKey() {
        List<MainBytecodeRecorderBuildItem> recorderItems = new ArrayList<>();
        ActionBuilderImpl builder = createBuilder(recorderItems, new ArrayList<>());

        RuntimeValue<CharSequence> rv = builder.serviceAsRuntimeValue(CharSequence.class, "primary");

        BytecodeRecorderImpl.ReturnedProxy proxy = (BytecodeRecorderImpl.ReturnedProxy) rv;
        assertThat(proxy.__returned$proxy$key()).isEqualTo("runtimevalue:java.lang.CharSequence:primary");
    }

    @Test
    void staticInitServiceAsRuntimeValueSetsStaticInitFlag() {
        List<StaticBytecodeRecorderBuildItem> staticItems = new ArrayList<>();
        ActionBuilderImpl builder = createBuilder(new ArrayList<>(), staticItems);

        RuntimeValue<CharSequence> rv = builder.staticInitServiceAsRuntimeValue(CharSequence.class);

        BytecodeRecorderImpl.ReturnedProxy proxy = (BytecodeRecorderImpl.ReturnedProxy) rv;
        assertThat(proxy.__static$$init()).isTrue();

        assertThat(staticItems).hasSize(1);
        TransliteratedAction ta = staticItems.get(0).getTransliteratedAction();
        assertThat(ta).isInstanceOf(TransliteratedAction.RuntimeValueWrapper.class);
        assertThat(ta.staticInit()).isTrue();
    }

    @Test
    void namedStaticInitServiceAsRuntimeValueUsesNameInKey() {
        List<StaticBytecodeRecorderBuildItem> staticItems = new ArrayList<>();
        ActionBuilderImpl builder = createBuilder(new ArrayList<>(), staticItems);

        RuntimeValue<CharSequence> rv = builder.staticInitServiceAsRuntimeValue(CharSequence.class, "named");

        BytecodeRecorderImpl.ReturnedProxy proxy = (BytecodeRecorderImpl.ReturnedProxy) rv;
        assertThat(proxy.__returned$proxy$key()).isEqualTo("runtimevalue:java.lang.CharSequence:named");
        assertThat(proxy.__static$$init()).isTrue();
    }

    // ── Helpers ──

    /**
     * Create an {@link ActionBuilderImpl} with capturing lists for produced build items.
     *
     * @param recorderItems list to capture runtime recorder items
     * @param staticItems list to capture static-init recorder items
     * @return the action builder
     */
    private static ActionBuilderImpl createBuilder(
            List<MainBytecodeRecorderBuildItem> recorderItems,
            List<StaticBytecodeRecorderBuildItem> staticItems) {
        return new ActionBuilderImpl(
                recorderItems::add,
                staticItems::add,
                m -> {
                },
                m -> {
                },
                m -> {
                },
                "testBuildStep");
    }
}
