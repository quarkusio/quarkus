package io.quarkus.extest;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.builditem.UserProjectRootBuildItem;
import io.quarkus.test.QuarkusUnitTest;

import java.nio.file.Files;

public class UserProjectRootDirTest {

    @RegisterExtension
    static QuarkusUnitTest quarkusUnitTest = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> ShrinkWrap.create(JavaArchive.class))
            .addBuildChainCustomizer(buildChain -> {
                buildChain.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        UserProjectRootBuildItem consume = context.consume(UserProjectRootBuildItem.class);
                        Assertions.assertThat(consume.rootDir()).isNotEmpty();
                        Assertions.assertThat(Files.isDirectory(consume.rootDir().get()));
                    }
                }).consumes(UserProjectRootBuildItem.class)
                        .produces(EmptyItem.class)
                        .build();
            });

    @Test
    void test() {
        System.out.println("hello");
    }

    public static final class EmptyItem extends SimpleBuildItem {
    }
}
