package io.quarkus.arc.test.interceptors.methodargs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.interceptors.Counter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class MethodArgsInterceptionTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Simple.class, SimpleInterceptor.class, ExampleResource.class,
            CustomExecutor.class, Counter.class);

    @Test
    public void testInterception() {
        ArcContainer container = Arc.container();
        Counter counter = container.instance(Counter.class).get();

        counter.reset();
        ExampleResource exampleResource = container.instance(ExampleResource.class).get();

        assertEquals("first,second", exampleResource.create(List.of("first", "second")));
        assertEquals(1, counter.get());

        assertEquals("PackagePrivate{}", exampleResource.otherCreate(new ExampleResource.PackagePrivate()));
        assertEquals(2, counter.get());

        CustomExecutor customThreadExecutor = container.instance(CustomExecutor.class).get();
        assertEquals("run", customThreadExecutor.run());
        assertEquals(3, counter.get());
    }

}
