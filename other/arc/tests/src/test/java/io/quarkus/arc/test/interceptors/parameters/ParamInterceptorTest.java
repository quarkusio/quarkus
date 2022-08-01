package io.quarkus.arc.test.interceptors.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ParamInterceptorTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(SimpleBean.class, Simple.class, ParamInterceptor.class);

    @Test
    public void testInterception() {

        ArcContainer arc = Arc.container();

        InstanceHandle<SimpleBean> handle = arc.instance(SimpleBean.class);
        SimpleBean simpleBean = handle.get();
        assertNull(simpleBean.getVal());

        simpleBean.setVal("intercept");
        assertEquals(String.class.getSimpleName(), simpleBean.getVal());

        simpleBean.setVal(null);
        assertNull(simpleBean.getVal());

        simpleBean.setVal(new StringBuilder("intercept"));
        assertEquals(StringBuilder.class.getSimpleName(), simpleBean.getVal());

        assertThrows(IllegalArgumentException.class, () -> {
            simpleBean.setStringBuilderVal(new StringBuilder("intercept"));
        });

        simpleBean.setPrimitiveIntVal(0);
        assertEquals("123456", simpleBean.getVal());

        simpleBean.setIntVal(1);
        assertEquals("123456", simpleBean.getVal());

        simpleBean.setNumberVal(2L);
        assertEquals("123456", simpleBean.getVal());

        handle.destroy();
    }
}
