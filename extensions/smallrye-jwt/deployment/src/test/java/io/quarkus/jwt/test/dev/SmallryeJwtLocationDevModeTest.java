package io.quarkus.jwt.test.dev;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.function.Consumer;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.test.QuarkusUnitTest;

public class SmallryeJwtLocationDevModeTest {

    @RegisterExtension
    static QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset(
                            """
                                    smallrye.jwt.encrypt.key.location=/publicKey.pem
                                    mp.jwt.decrypt.key.location=/privateKey.pem
                                    """), "application.properties")
                    .addAsResource("publicKey.pem")
                    .addAsResource("privateKey.pem"))
            .addBuildChainCustomizer(new Consumer<BuildChainBuilder>() {
                @Override
                public void accept(BuildChainBuilder chain) {
                    chain.addBuildStep(new BuildStep() {
                        @Override
                        public void execute(BuildContext context) {
                            List<DevServicesResultBuildItem> buildItems = context
                                    .consumeMulti(DevServicesResultBuildItem.class);
                            assertThat(buildItems).filteredOn(item -> item.getName().equals("smallrye-jwt"))
                                    .first()
                                    .satisfies(item -> {
                                        assertThat(item.getConfig())
                                                .containsEntry("mp.jwt.verify.publickey", "NONE")
                                                .containsEntry("smallrye.jwt.sign.key", "NONE");
                                    });
                            context.produce(new FeatureBuildItem("dummy"));
                        }
                    })
                            .consumes(DevServicesResultBuildItem.class)
                            .produces(FeatureBuildItem.class)
                            .build();
                }
            });

    @Test
    void shouldNotConfigureAutomatically() {
        assertThat(true).isTrue();
    }

}
