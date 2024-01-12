package io.quarkus.smallrye.reactivemessaging.channels;

import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class EmitterWithMultipleInjectionPointsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ChannelEmitterWithMultipleDefinitions.class));

    @Inject
    ChannelEmitterWithMultipleDefinitions bean;

    @Test
    public void testEmitter() {
        bean.run();
        Assertions.assertThat(bean.list()).contains("a", "b", "c", "a2", "b2", "c2");
    }

}
