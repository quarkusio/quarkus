package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class MapResolverTest {

    @Test
    public void tesMapResolver() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("name", "Lu");
        map.put("foo.bar", "Ondrej");

        Engine engine = Engine.builder()
                .addSectionHelper(new LoopSectionHelper.Factory())
                .addDefaultValueResolvers()
                .build();

        assertEquals("Lu,Lu,2,false,true,namefoo.bar::Ondrej,Ondrej",
                engine.parse(
                        "{this.name},{this['name']},{this.size},{this.empty},{this.containsKey('name')},"
                                + "{#each this.keys}{it}{/each}"
                                + "::{this.get('foo.bar')},{this['foo.bar']}")
                        .render(map));
    }

}
