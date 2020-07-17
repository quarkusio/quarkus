package io.quarkus.security.test.cdi;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class InvalidClassBeanTestCase {
    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClass(InvalidBean.class);
                }
            }).assertException(new Consumer<Throwable>() {
                @Override
                public void accept(Throwable throwable) {
                    Assertions.assertEquals(IllegalStateException.class, throwable.getClass());
                }
            });

    @Test
    public void testRejected() {
        Assertions.fail();
    }
}
