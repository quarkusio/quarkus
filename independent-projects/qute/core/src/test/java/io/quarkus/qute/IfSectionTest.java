package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class IfSectionTest {

    @Test
    public void tesIfElse() {
        Engine engine = Engine.builder().addSectionHelper(new IfSectionHelper.Factory())
                .addValueResolver(ValueResolvers.mapResolver())
                .build();

        Template template = engine.parse("{#if isActive}ACTIVE{#else}INACTIVE{/if}");
        Map<String, Boolean> data = new HashMap<>();
        data.put("isActive", Boolean.FALSE);
        assertEquals("INACTIVE", template.render(data));

        template = engine.parse("{#if isActive}ACTIVE{#else if valid}VALID{#else}NULL{/if}");
        data.put("valid", Boolean.TRUE);
        assertEquals("VALID", template.render(data));
    }

    @Test
    public void tesIfOperator() {
        Engine engine = Engine.builder().addSectionHelper(new IfSectionHelper.Factory())
                .addValueResolver(ValueResolvers.mapResolver())
                .build();

        Map<String, Object> data = new HashMap<>();
        data.put("name", "foo");
        data.put("foo", "foo");
        data.put("one", "1");
        data.put("two", Integer.valueOf(2));

        assertEquals("ACTIVE", engine.parse("{#if name eq foo}ACTIVE{#else}INACTIVE{/if}").render(data));
        assertEquals("INACTIVE", engine.parse("{#if name != foo}ACTIVE{#else}INACTIVE{/if}").render(data));
        assertEquals("OK", engine.parse("{#if one < two}OK{/if}").render(data));
        assertEquals("OK", engine.parse("{#if one >= one}OK{/if}").render(data));
        assertEquals("OK", engine.parse("{#if one >= 0}OK{/if}").render(data));
        assertEquals("OK", engine.parse("{#if one == one}OK{/if}").render(data));
        assertEquals("OK", engine.parse("{#if one}NOK{#else if name eq foo}OK{/if}").render(data));
        assertEquals("OK", engine.parse("{#if name is foo}OK{/if}").render(data));
        assertEquals("OK", engine.parse("{#if two is 2}OK{/if}").render(data));
        assertEquals("OK", engine.parse("{#if name != null}OK{/if}").render(data));
        assertEquals("OK", engine.parse("{#if name is null}NOK{#else}OK{/if}").render(data));
        assertEquals("OK", engine.parse("{#if !false}OK{/if}").render(data));
    }

    @Test
    public void testNestedIf() {
        Engine engine = Engine.builder().addSectionHelper(new IfSectionHelper.Factory())
                .addValueResolver(ValueResolvers.mapResolver())
                .build();

        Map<String, Object> data = new HashMap<>();
        data.put("ok", true);
        data.put("nok", false);

        assertEquals("OK", engine.parse("{#if ok}{#if ok}OK{/}{#else}NOK{/if}").render(data));
    }

}
