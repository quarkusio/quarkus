package io.quarkus.oidc.client;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Method;
import java.util.function.Consumer;

import jakarta.enterprise.context.ApplicationScoped;

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
import io.restassured.RestAssured;

@QuarkusTestResource(KeycloakRealmClientCredentialsJwtSecretManager.class)
public class OidcClientCredentialsJwtSecretTestCase {

    private static Class<?>[] testClasses = {
            OidcClientsResource.class,
            ProtectedResource.class,
            RuntimeSecretProvider.class,
            TestRecorder.class,
            OidcClientCredentialsJwtSecretTestCase.class
    };

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addAsResource("application-oidc-client-credentials-jwt-secret.properties", "application.properties"))
            .addBuildChainCustomizer(buildCustomizer());

    @Test
    public void testGetTokenJwtClient() {
        String token = RestAssured.when().get("/clients/token/jwt").body().asString();
        RestAssured.given().auth().oauth2(token)
                .when().get("/protected")
                .then()
                .statusCode(200)
                .body(equalTo("service-account-quarkus-app"));
    }

    @Test
    public void testGetTokensJwtClient() {
        String[] tokens = RestAssured.when().get("/clients/tokens/jwt").body().asString().split(" ");
        assertTokensNotNull(tokens);

        RestAssured.given().auth().oauth2(tokens[0])
                .when().get("/protected")
                .then()
                .statusCode(200)
                .body(equalTo("service-account-quarkus-app"));
    }

    private static void assertTokensNotNull(String[] tokens) {
        assertEquals(2, tokens.length);
        assertNotNull(tokens[0]);
        assertEquals("null", tokens[1]);
    }

    @Recorder
    public static class TestRecorder {

        public RuntimeSecretProvider createRuntimeSecretProvider() {
            return new RuntimeSecretProvider();
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
}
