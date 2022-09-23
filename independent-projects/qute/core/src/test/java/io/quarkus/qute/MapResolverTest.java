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
        map.put("foo and bar", "Bug");

        Engine engine = Engine.builder().addDefaults().build();

        assertEquals("Lu,Lu,3,false,true,[name,foo.bar,foo and bar],Ondrej,Ondrej,Bug",
                engine.parse(
                        "{this.name},"
                                + "{this['name']},"
                                + "{this.size},"
                                + "{this.empty},"
                                + "{this.containsKey('name')},"
                                + "[{#each this.keys}{it}{#if it_hasNext},{/if}{/each}],"
                                + "{this.get('foo.bar')},"
                                + "{this['foo.bar']},"
                                + "{this['foo and bar']}")
                        .render(map));
    }

}
