package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class CollectionResolverTest {

    @Test
    public void testResolver() {
        List<String> list = new ArrayList<>();
        list.add("Lu");

        Engine engine = Engine.builder()
                .addSectionHelper(new LoopSectionHelper.Factory())
                .addValueResolver(ValueResolvers.thisResolver())
                .addValueResolver(ValueResolvers.collectionResolver())
                .build();

        assertEquals("1,false,true",
                engine.parse("{this.size},{this.isEmpty},{this.contains('Lu')}").render(list));
    }

    @Test
    public void testListTake() {
        List<String> list = new ArrayList<>();
        list.add("Lu");
        list.add("Roman");
        list.add("Matej");

        Engine engine = Engine.builder().addDefaults().build();

        assertEquals("Lu,",
                engine.parse("{#each list.take(1)}{it},{/each}").data("list", list).render());
        assertEquals("Roman,Matej,",
                engine.parse("{#each list.takeLast(2)}{it},{/each}").data("list", list).render());
        try {
            assertEquals("3",
                    engine.parse("{list.take(12).size}").data("list", list).render());
            fail();
        } catch (IndexOutOfBoundsException expected) {
        }
        try {
            assertEquals("3",
                    engine.parse("{list.take(-1).size}").data("list", list).render());
            fail();
        } catch (IndexOutOfBoundsException expected) {
        }
    }

}
