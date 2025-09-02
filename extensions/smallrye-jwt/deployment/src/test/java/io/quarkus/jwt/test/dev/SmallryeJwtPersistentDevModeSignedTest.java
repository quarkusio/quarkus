package io.quarkus.jwt.test.dev;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.jwt.test.GreetingResource;
import io.quarkus.smallrye.jwt.deployment.GeneratePersistentDevModeJwtKeysBuildItem;
import io.quarkus.smallrye.jwt.deployment.SmallryeJwtDevModeProcessor;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.smallrye.jwt.build.Jwt;

public class SmallryeJwtPersistentDevModeSignedTest {

    @RegisterExtension
    static QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(GreetingResource.class))
            .addBuildChainCustomizer(new PersistentJwtChainBuilder());

    static class PersistentJwtChainBuilder implements Consumer<BuildChainBuilder> {
        @Override
        public void accept(BuildChainBuilder chain) {
            chain.addBuildStep(new BuildStep() {
                @Override
                public void execute(BuildContext context) {
                    CurateOutcomeBuildItem curateOutcomeBuildItem = context.consume(CurateOutcomeBuildItem.class);
                    File buildDir = SmallryeJwtDevModeProcessor.getBuildDir(curateOutcomeBuildItem);
                    File privateKeyFile = new File(buildDir, SmallryeJwtDevModeProcessor.DEV_PRIVATE_KEY_PEM);
                    File publicKeyFile = new File(buildDir, SmallryeJwtDevModeProcessor.DEV_PUBLIC_KEY_PEM);
                    // make sure the files were created
                    assertThat(privateKeyFile).exists();
                    assertThat(publicKeyFile).exists();
                    try {
                        // extract their keys
                        String publicKey = Files.readAllLines(publicKeyFile.toPath())
                                .stream().filter(l -> !l.startsWith("----"))
                                .collect(Collectors.joining());
                        String privateKey = Files.readAllLines(privateKeyFile.toPath())
                                .stream().filter(l -> !l.startsWith("----"))
                                .collect(Collectors.joining());
                        List<RunTimeConfigurationDefaultBuildItem> buildItems = context
                                .consumeMulti(RunTimeConfigurationDefaultBuildItem.class);
                        // make sure we used them for configuration
                        assertThat(buildItems)
                                .filteredOn(
                                        item -> item.getKey()
                                                .equals(SmallryeJwtDevModeProcessor.MP_JWT_VERIFY_PUBLIC_KEY))
                                .first()
                                .extracting(s -> s.getValue())
                                .isEqualTo(publicKey);
                        assertThat(buildItems)
                                .filteredOn(
                                        item -> item.getKey().equals(SmallryeJwtDevModeProcessor.SMALLRYE_JWT_SIGN_KEY))
                                .first()
                                .extracting(s -> s.getValue())
                                .isEqualTo(privateKey);
                        context.produce(new FeatureBuildItem("dummy"));
                    } catch (IOException x) {
                        throw new UncheckedIOException(x);
                    }
                }
            })
                    .consumes(RunTimeConfigurationDefaultBuildItem.class)
                    .consumes(CurateOutcomeBuildItem.class)
                    .produces(FeatureBuildItem.class)
                    .build();
            chain.addBuildStep(new BuildStep() {
                @Override
                public void execute(BuildContext context) {
                    // this is called before the JWT dev mode processor, so we can clean up here
                    CurateOutcomeBuildItem curateOutcomeBuildItem = context.consume(CurateOutcomeBuildItem.class);
                    File buildDir = SmallryeJwtDevModeProcessor.getBuildDir(curateOutcomeBuildItem);
                    new File(buildDir, SmallryeJwtDevModeProcessor.DEV_PRIVATE_KEY_PEM).delete();
                    new File(buildDir, SmallryeJwtDevModeProcessor.DEV_PUBLIC_KEY_PEM).delete();
                    context.produce(new GeneratePersistentDevModeJwtKeysBuildItem());
                }
            })
                    .produces(GeneratePersistentDevModeJwtKeysBuildItem.class)
                    .consumes(CurateOutcomeBuildItem.class)
                    .build();
        }
    }

    @Test
    void canBeSigned() {
        // make sure we can sign JWT tokens recognised by the server, since they use the same config
        String token = Jwt.upn("jdoe@quarkus.io")
                .groups("User")
                .sign();
        RestAssured.given()
                .header(new Header("Authorization", "Bearer " + token))
                .get("/only-user")
                .then().assertThat().statusCode(200);
    }
}
