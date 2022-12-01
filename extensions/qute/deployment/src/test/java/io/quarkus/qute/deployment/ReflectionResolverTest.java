package io.quarkus.qute.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class ReflectionResolverTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HelloReflect.class)
                    .addAsResource(
                            new StringAsset(
                                    "{age}:{ping}:{noMatch ?: 'NOT_FOUND'}:{active}:{isActive}:{hasItem}:{item}:{age2}"),
                            "templates/reflect.txt"));

    @Inject
    Template reflect;

    @Test
    public void testInjection() {
        assertEquals("10:pong:NOT_FOUND:true:true:false:false:10", reflect.render(new HelloReflect()));
    }

}
