package io.quarkus.smallrye.reactivemessaging.hotreload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.vertx.core.json.JsonArray;

public class CodeChangeTest {

    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClasses(SomeSource.class, SomeSink.class, SomeProcessor.class));

    @Test
    public void testUpdatingCode() {
        await().until(() -> get().size() > 5);
        assertThat(get()).contains("-1", "-2", "-3", "-4");

        // Update processor
        TEST.modifySourceFile("SomeProcessor.java", s -> s.replace("* -1", "* -2"));
        await().until(() -> get().size() > 5);
        assertThat(get()).contains("-2", "-4", "-6").doesNotContain("-1", "-3");

        // Update source
        TEST.modifySourceFile("SomeSource.java", s -> s.replace("+ 1", "+ 100"));
        await().until(() -> get().size() > 5);
        assertThat(get()).contains("-200", "-202", "-204", "-206");

        // Update sink
        TEST.modifySourceFile("SomeSink.java", s -> s.replace("items.add(l)", "items.add(l+ \"foo\")"));
        await().until(() -> get().size() > 5);
        assertThat(get()).contains("-200foo", "-202foo", "-204foo", "-206foo");

    }

    @Test
    public void testUpdatingAnnotations() {
        await().until(() -> get().size() > 5);
        assertThat(get()).contains("-1", "-2", "-3", "-4");

        TEST.modifySourceFile("SomeProcessor.java", s -> s.replace("my-source", "my-source-2"));
        TEST.modifySourceFile("SomeSource.java", s -> s.replace("my-source", "my-source-2"));
        TEST.modifySourceFile("SomeSink.java", s -> s.replace("my-sink", "my-sink-2"));

        JsonArray array = get();
        Assertions.assertTrue(array.isEmpty());
        TEST.modifySourceFile("SomeProcessor.java", s -> s.replace("my-sink", "my-sink-2"));

        await().until(() -> get().size() > 5);
        assertThat(get()).contains("-1", "-2", "-3", "-4");
    }

    static JsonArray get() {
        return new JsonArray(RestAssured.get().asString());
    }

}
