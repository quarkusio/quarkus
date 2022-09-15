package io.quarkus.arc.test.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.Priority;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class PriorityTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Converter.class, AlphaConverter.class, BravoConverter.class,
            Converters.class);

    @Test
    public void testPriority() {
        InjectableInstance<Converter> instance = Arc.container().select(Converter.class);
        String val = "ok";
        for (Converter converter : instance) {
            val = converter.convert(val);
        }
        assertEquals("ok:charlie:alpha:bravo", val);
    }

    interface Converter {

        String convert(String val);

    }

    @jakarta.annotation.Priority(5) // this priority takes precedence
    @Priority(50)
    @Singleton
    static class AlphaConverter implements Converter {

        @Override
        public String convert(String val) {
            return val + ":alpha";
        }

    }

    @Singleton
    static class BravoConverter implements Converter {

        @Override
        public String convert(String val) {
            return val + ":bravo";
        }

    }

    @Singleton
    static class Converters {

        @Produces
        @Priority(10)
        Converter charlie() {
            return new Converter() {

                @Override
                public String convert(String val) {
                    return val + ":charlie";
                }
            };
        }

    }

}
