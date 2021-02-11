package io.quarkus.arc.test.clientproxy.toString;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ClientProxyToStringDelegatedTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Foo.class);

    @Test
    public void testToStringIsDelegated() {
        Foo bean = Arc.container().instance(Foo.class).get();
        Assertions.assertFalse(bean.toString().contains("_ClientProxy"));
    }
}
