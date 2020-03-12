package io.quarkus.arc.test;

import java.util.function.Consumer;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class SplitPackageTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setAllowTestClassOutsideDeployment(true)
            .assertException(new Consumer<Throwable>() {
                @Override
                public void accept(Throwable throwable) {
                    Assertions.assertTrue(throwable.toString().contains("Split package"));
                }
            })
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(SubclassBean.class)
                    .addAsResource(new StringAsset("quarkus.arc.remove-unused-beans=none"), "application.properties"));

    @Test
    public void testSimpleBean() {
        Assertions.fail();
    }

}
