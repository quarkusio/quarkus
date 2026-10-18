package io.quarkus.arc.test.interceptors.limits;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.interceptors.Counter;
import io.quarkus.arc.test.interceptors.Simple;
import io.quarkus.arc.test.interceptors.SimpleInterceptor;

public class SubclassLimitsTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Simple.class, SimpleInterceptor.class, Counter.class,
            LargeBean.class);

    @Test
    public void testInterception() {
        ArcContainer container = Arc.container();
        LargeBean largeBean = container.instance(LargeBean.class).get();
        assertEquals("0fii1", largeBean.ping1("fii"));
        assertEquals("1fii2", largeBean.ping100("fii"));
    }

    // this could be used to generate a larger bean
    //    public static void main(String[] args) throws IOException {
    //        int count = 1500;
    //        StringBuilder builder = new StringBuilder();
    //        builder.append("package io.quarkus.arc.test.interceptors.limits;");
    //        builder.append("import jakarta.enterprise.context.ApplicationScoped;");
    //        builder.append("import io.quarkus.arc.test.interceptors.Simple;");
    //        builder.append("@Simple @ApplicationScoped public class LargeBean {");
    //        for (int i = 0; i < count; i++) {
    //            builder.append("public String ping").append(i).append("(String val) { return val; }");
    //        }
    //        builder.append("}");
    //        Files.write(new File("LargeBean.java").toPath(), builder.toString().getBytes());
    //    }

}
