package io.quarkus.arc.test.profile;

import static org.junit.jupiter.api.Assertions.assertFalse;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.QuarkusUnitTest;

@Disabled("For some reason the indexing of AdditionalIfBuildProfileBean fails - the class resource cannot be found by the CL")
public class AdditionalBeanWithIfBuildProfileTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication()
            .addBuildChainCustomizer(b -> {
                b.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        context.produce(AdditionalBeanBuildItem
                                .unremovableOf(AdditionalIfBuildProfileBean.class));
                    }
                }).produces(AdditionalBeanBuildItem.class).build();

            });

    @Inject
    Instance<AdditionalIfBuildProfileBean> instance;

    @Test
    public void testBeans() {
        // The bean should be vetoed
        assertFalse(instance.isResolvable());
    }

}
