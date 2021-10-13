package io.quarkus.qute;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ArrayResolverTest {

    @Test
    public void testArrays() {
        Engine engine = Engine.builder().addDefaults().build();

        int[] int1 = { 1, 2, 3 };

        assertEquals("3", engine.parse("{array.length}").data("array", int1).render());
        assertEquals("2", engine.parse("{array.1}").data("array", int1).render());
        assertEquals("3", engine.parse("{array.get(2)}").data("array", int1).render());

        assertThatExceptionOfType(TemplateException.class)
                .isThrownBy(() -> engine.parse("{array.get('foo')}").data("array", int1).render())
                .withMessage(
                        "Method \"get('foo')\" not found on the base object \"[I\" in expression {array.get('foo')} in template 4 on line 1");

        assertEquals("3", engine.parse("{array.get(last)}").data("array", int1, "last", 2).render());
        assertEquals("1", engine.parse("{array[0]}").data("array", int1).render());

        long[][] long2 = { { 1L, 2L }, { 3L, 4L }, {} };

        assertEquals("3", engine.parse("{array.length}").data("array", long2).render());
        assertEquals("2", engine.parse("{array.1.length}").data("array", long2).render());
        assertEquals("2", engine.parse("{array.get(0).get(1)}").data("array", long2).render());
        assertEquals("1", engine.parse("{array[0][0]}").data("array", long2).render());
        assertEquals("0", engine.parse("{array[2].length}").data("array", long2).render());

        assertThatExceptionOfType(ArrayIndexOutOfBoundsException.class)
                .isThrownBy(() -> engine.parse("{array.get(10)}").data("array", long2).render());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> engine.parse("{array.get()}").data("array", long2).render());
    }

    @Test
    public void testTake() {
        String[] array = new String[] { "Lu", "Roman", "Matej" };

        Engine engine = Engine.builder().addDefaults().build();

        assertEquals("Lu,",
                engine.parse("{#each array.take(1)}{it},{/each}").data("array", array).render());
        assertEquals("Roman,Matej,",
                engine.parse("{#each array.takeLast(2)}{it},{/each}").data("array", array).render());

        assertThatExceptionOfType(IndexOutOfBoundsException.class)
                .isThrownBy(() -> engine.parse("{array.take(12).size}").data("array", array).render());

        assertThatExceptionOfType(IndexOutOfBoundsException.class)
                .isThrownBy(() -> engine.parse("{array.take(-1).size}").data("array", array).render());
    }

}
