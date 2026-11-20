package io.quarkus.deployment.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.deployment.pkg.TestNativeConfig;

/**
 * Since GraalVM 25.1, URL protocol handlers are enabled through reachability metadata instead of the deprecated
 * {@code --enable-url-protocols} native-image option.
 */
class NativeImageConfigBuildStepUrlProtocolsTest {

    private static final String HTTP_HANDLER = "sun.net.www.protocol.http.Handler";
    private static final String HTTPS_HANDLER = "sun.net.www.protocol.https.Handler";

    @Test
    void httpHandlerOnlyByDefault() {
        assertThat(register(true, false, false)).singleElement().satisfies(m -> assertNoArgConstructor(m, HTTP_HANDLER));
    }

    @Test
    void httpsHandlerWhenEnabledExplicitly() {
        List<ReflectiveMethodBuildItem> registered = register(true, true, false);

        assertThat(registered).hasSize(2);
        assertThat(registered).anySatisfy(m -> assertNoArgConstructor(m, HTTP_HANDLER));
        assertThat(registered).anySatisfy(m -> assertNoArgConstructor(m, HTTPS_HANDLER));
    }

    @Test
    void httpsHandlerWhenSslNativeEnabled() {
        assertThat(register(false, false, true)).singleElement().satisfies(m -> assertNoArgConstructor(m, HTTPS_HANDLER));
    }

    @Test
    void nothingWhenAllHandlersDisabled() {
        assertThat(register(false, false, false)).isEmpty();
    }

    private static void assertNoArgConstructor(ReflectiveMethodBuildItem method, String declaringClass) {
        assertThat(method.getDeclaringClass()).isEqualTo(declaringClass);
        assertThat(method.getName()).isEqualTo("<init>");
        assertThat(method.getParams()).isEmpty();
    }

    private static List<ReflectiveMethodBuildItem> register(boolean http, boolean https, boolean sslNativeEnabled) {
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
        NativeImageConfigBuildStep.registerUrlProtocolHandlers(nativeConfig, sslNativeEnabled, produced::add);
        return produced;
    }
}
