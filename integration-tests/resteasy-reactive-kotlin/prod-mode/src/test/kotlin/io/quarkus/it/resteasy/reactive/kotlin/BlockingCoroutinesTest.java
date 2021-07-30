package io.quarkus.it.resteasy.reactive.kotlin;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.function.Supplier;

import javax.ws.rs.Path;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;
import io.smallrye.common.annotation.Blocking;

public class BlockingCoroutinesTest {

    @RegisterExtension
    static QuarkusProdModeTest test = new QuarkusProdModeTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(TestEndpoint.class);
                }
            }).setExpectedException(IllegalStateException.class);

    @Test
    public void test() {
        fail("Should never be called");
    }

    @Path("test")
    static class TestEndpoint {

        @Path("hello")
        @Blocking
        public Object hello(kotlin.coroutines.Continuation<? super Object> cont) {
            throw new IllegalStateException("should never be called");
        }
    }
}
