package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class MapResolverTest {

    @Test
    public void tesMapResolver() {
        Map<String, String> map = new HashMap<>();
        map.put("name", "Lu");

        Engine engine = Engine.builder()
                .addSectionHelper(new LoopSectionHelper.Factory())
                .addDefaultValueResolvers()
                .build();

        assertEquals("Lu,1,false,true,name",
                engine.parse("{this.name},{this.size},{this.empty},{this.containsKey('name')},{#each this.keys}{it}{/each}")
                        .render(map));
    }

}
