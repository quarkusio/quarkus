package io.quarkus.oidc.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.function.Consumer;

import jakarta.enterprise.context.ApplicationScoped;

import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.credentials.CredentialsProvider;
import io.quarkus.deployment.builditem.MainBytecodeRecorderBuildItem;
import io.quarkus.deployment.recording.BytecodeRecorderImpl;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.keycloak.server.KeycloakTestResourceLifecycleManager;

@QuarkusTestResource(value = KeycloakTestResourceLifecycleManager.class)
public class CodeFlowRuntimeCredentialsProviderTest {

    private static final Class<?>[] TEST_CLASSES = {
            ProtectedResource.class,
            RuntimeSecretProvider.class,
            CodeFlowRuntimeCredentialsProviderTest.class,
            TestRecorder.class
    };

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TEST_CLASSES)
                    .addAsResource("application-runtime-cred-provider.properties", "application.properties"))
            .addBuildChainCustomizer(buildCustomizer());

    @Test
    public void testRuntimeCredentials() throws IOException, InterruptedException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/protected");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getButtonByName("login").click();

            assertEquals("alice", page.getBody().asNormalizedText());
        }
    }

    private static Consumer<BuildChainBuilder> buildCustomizer() {
        // whole purpose of this step is to have a bean recorded during runtime init
        return new Consumer<BuildChainBuilder>() {

            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        BytecodeRecorderImpl bytecodeRecorder = new BytecodeRecorderImpl(false,
                                TestRecorder.class.getSimpleName(), "createRuntimeSecretProvider",
                                "" + TestRecorder.class.hashCode(), true, s -> null);
                        context.produce(new MainBytecodeRecorderBuildItem(bytecodeRecorder));

                        // We need to use reflection due to some class loading problems
                        Object recorderProxy = bytecodeRecorder.getRecordingProxy(TestRecorder.class);
                        try {
                            Method creator = recorderProxy.getClass().getDeclaredMethod("createRuntimeSecretProvider");
                            Object proxy1 = creator.invoke(recorderProxy, new Object[] {});

                            context.produce(SyntheticBeanBuildItem
                                    .configure(RuntimeSecretProvider.class)
                                    .types(CredentialsProvider.class)
                                    .scope(ApplicationScoped.class)
                                    .named("runtime-vault-secret-provider")
                                    .setRuntimeInit()
                                    .unremovable()
                                    .runtimeProxy(proxy1)
                                    .done());

                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }).produces(MainBytecodeRecorderBuildItem.class).produces(SyntheticBeanBuildItem.class).build();
            }
        };
    }

    private static WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }

    @Recorder
    public static class TestRecorder {

        public RuntimeSecretProvider createRuntimeSecretProvider() {
            return new RuntimeSecretProvider();
        }

    }
}
