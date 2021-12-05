package io.quarkus.smallrye.reactivemessaging.mutiny;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class UniTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(StringProducer.class, UniBean.class));

    @Inject
    UniBean uniBean;

    @Test
    public void shouldGetTextFromUniBean() {
        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> uniBean.getStrings().size() > 2);
        assertThat(uniBean.getStrings()).containsExactly("some!", "other!", "text!");
    }
}
