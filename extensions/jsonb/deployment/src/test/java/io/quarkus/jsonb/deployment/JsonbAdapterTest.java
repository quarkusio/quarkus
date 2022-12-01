package io.quarkus.jsonb.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.adapter.JsonbAdapter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class JsonbAdapterTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Alpha.class, AlphaAdapter.class, Bravo.class));

    @Inject
    Jsonb jsonb;

    @Inject
    Instance<JsonbAdapter<?, ?>> adapters;

    @Test
    public void testAdapterInjection() {
        assertFalse(adapters.isUnsatisfied());
        assertEquals("\"foo_bravo\"", jsonb.toJson(new Alpha("foo")));
        assertEquals("bar_bravo", jsonb.fromJson("\"bar\"", Alpha.class).getName());
    }

}
