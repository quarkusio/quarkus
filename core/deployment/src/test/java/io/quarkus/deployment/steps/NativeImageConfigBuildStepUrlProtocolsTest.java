package io.quarkus.deployment.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.SslNativeConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.deployment.pkg.TestNativeConfig;

/**
 * URL protocol handlers are enabled through reachability metadata instead of the deprecated
 * {@code --enable-url-protocols} native-image option.
 */
class NativeImageConfigBuildStepUrlProtocolsTest {

    private static final String HTTP_HANDLER = "sun.net.www.protocol.http.Handler";
    private static final String HTTPS_HANDLER = "sun.net.www.protocol.https.Handler";

    @Test
    void httpHandlerOnlyByDefault() {
        List<ReflectiveMethodBuildItem> registered = register(true, false, Optional.empty(), List.of());

        assertThat(registered).singleElement().satisfies(m -> assertNoArgConstructor(m, HTTP_HANDLER));
    }

    @Test
    void httpsHandlerWhenEnabledExplicitly() {
        List<ReflectiveMethodBuildItem> registered = register(true, true, Optional.empty(), List.of());

        assertThat(registered).hasSize(2);
        assertThat(registered).anySatisfy(m -> assertNoArgConstructor(m, HTTP_HANDLER));
        assertThat(registered).anySatisfy(m -> assertNoArgConstructor(m, HTTPS_HANDLER));
    }

    @Test
    void httpsHandlerWhenSslNativeEnabled() {
        List<ReflectiveMethodBuildItem> registered = register(false, false, Optional.of(true), List.of());

        assertThat(registered).singleElement().satisfies(m -> assertNoArgConstructor(m, HTTPS_HANDLER));
    }

    @Test
    void httpsHandlerWhenAnExtensionRequiresSsl() {
        List<ReflectiveMethodBuildItem> registered = register(false, false, Optional.empty(),
                List.of(new ExtensionSslNativeSupportBuildItem("some-extension")));

        assertThat(registered).singleElement().satisfies(m -> assertNoArgConstructor(m, HTTPS_HANDLER));
    }

    @Test
    void noHttpsHandlerWhenSslNativeExplicitlyDisabled() {
        List<ReflectiveMethodBuildItem> registered = register(true, false, Optional.of(false),
                List.of(new ExtensionSslNativeSupportBuildItem("some-extension")));

        assertThat(registered).singleElement().satisfies(m -> assertNoArgConstructor(m, HTTP_HANDLER));
    }

    @Test
    void nothingWhenAllHandlersDisabled() {
        assertThat(register(false, false, Optional.of(false), List.of())).isEmpty();
    }

    private static void assertNoArgConstructor(ReflectiveMethodBuildItem method, String declaringClass) {
        assertThat(method.getDeclaringClass()).isEqualTo(declaringClass);
        assertThat(method.getName()).isEqualTo("<init>");
        assertThat(method.getParams()).isEmpty();
    }

    private static List<ReflectiveMethodBuildItem> register(boolean http, boolean https, Optional<Boolean> sslNative,
            List<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport) {
        TestNativeConfig nativeConfig = new TestNativeConfig("mandrel") {
            @Override
            public boolean enableHttpUrlHandler() {
                return http;
            }

            @Override
            public boolean enableHttpsUrlHandler() {
                return https;
            }
        };
        List<ReflectiveMethodBuildItem> produced = new ArrayList<>();
        new NativeImageConfigBuildStep().registerUrlProtocolHandlers(nativeConfig, new SslNativeConfigBuildItem(sslNative),
                extensionSslNativeSupport, produced::add);
        return produced;
    }
}
