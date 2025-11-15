package io.quarkus.jwt.test.dev;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.jwt.test.GreetingResource;
import io.quarkus.smallrye.jwt.deployment.GenerateEncryptedDevModeJwtKeysBuildItem;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.smallrye.jwt.build.Jwt;

public class SmallryeJwtPersistentColdStartupEncryptedTest {

    @RegisterExtension
    static QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(GreetingResource.class,
                            SmallryeJwtPersistentColdStartupSignedTest.PersistentJwtColdStartupChainBuilder.class))
            .addBuildChainCustomizer(new SmallryeJwtPersistentColdStartupSignedTest.PersistentJwtColdStartupChainBuilder() {
                @Override
                public void accept(BuildChainBuilder chain) {
                    super.accept(chain);
                    chain.addBuildStep(new BuildStep() {
                        @Override
                        public void execute(BuildContext context) {
                            context.produce(new GenerateEncryptedDevModeJwtKeysBuildItem());
                        }
                    })
                            .produces(GenerateEncryptedDevModeJwtKeysBuildItem.class)
                            .build();
                }
            });

    @Test
    void canBeEncrypted() {
        // make sure we can sign JWT tokens recognised by the server, since they use the same config
        String token = Jwt.upn("jdoe@quarkus.io")
                .groups("User")
                .innerSign().encrypt();
        RestAssured.given()
                .header(new Header("Authorization", "Bearer " + token))
                .get("/only-user")
                .then().assertThat().statusCode(200);
    }
}
