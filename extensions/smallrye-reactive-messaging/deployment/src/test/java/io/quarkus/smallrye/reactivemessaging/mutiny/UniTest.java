package io.quarkus.smallrye.reactivemessaging.mutiny;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.timeouts.TestTimeouts;

public class UniTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(StringProducer.class, UniBean.class));

    @Inject
    UniBean uniBean;

    @Test
    public void shouldGetTextFromUniBean() {
        await().atMost(TestTimeouts.get())
                .until(() -> uniBean.getStrings().size() > 2);
        assertThat(uniBean.getStrings()).containsExactly("some!", "other!", "text!");
    }
}
