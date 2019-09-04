package io.quarkus.arc.test.clientproxy.toString;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class ClientProxyToStringDelegatedTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(Foo.class);

    @Test
    public void testToStringIsDelegated() {
        Foo bean = Arc.container().instance(Foo.class).get();
        Assert.assertFalse(bean.toString().contains("_ClientProxy"));
    }
}
