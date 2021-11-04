package io.quarkus.smallrye.reactivemessaging.mutiny;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class MultiTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MultiIntBean.class, MultiStringBean.class, StringProducer.class));

    @Inject
    MultiIntBean multiIntBean;
    @Inject
    MultiStringBean multiStringBean;

    @Test
    public void shouldGatherEvenFromMultiBean() {
        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> multiIntBean.getEvenNumbers().size() > 5);

        assertThat(multiIntBean.getEvenNumbers()).contains(2, 4, 6, 8, 10, 12);
    }

    @Test
    public void shouldGetHelloFromMultiBean() {
        List<String> strings = multiStringBean.getStrings(Duration.ofSeconds(5));
        assertThat(strings).containsExactly("hello", "world", "from", "smallrye", "reactive", "messaging");
    }
}
