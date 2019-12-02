package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class IncludeTest {

    @Test
    public void testInclude() {
        Engine engine = Engine.builder().addSectionHelper(new IncludeSectionHelper.Factory())
                .addSectionHelper(new InsertSectionHelper.Factory())
                .addValueResolver(ValueResolvers.thisResolver())
                .build();

        engine.putTemplate("super", engine.parse("{this}: {#insert header}default header{/insert}"));
        assertEquals("HEADER: super header",
                engine.parse("{#include super}{#header}super header{/header}{/include}").render("HEADER"));
    }

    @Test
    public void testMultipleInserts() {
        Engine engine = Engine.builder().addSectionHelper(new IncludeSectionHelper.Factory())
                .addSectionHelper(new InsertSectionHelper.Factory())
                .addValueResolver(ValueResolvers.thisResolver())
                .build();

        engine.putTemplate("super",
                engine.parse("{#insert header}default header{/insert} AND {#insert content}default content{/insert}"));

        Template template = engine
                .parse("{#include super}{#header}super header{/header}  {#content}super content{/content} {/include}");
        assertEquals("super header AND super content", template.render(null));
    }

    @Test
    public void testIncludeSimpleData() {
        Engine engine = Engine.builder().addSectionHelper(new IncludeSectionHelper.Factory())
                .addSectionHelper(new InsertSectionHelper.Factory())
                .addValueResolver(ValueResolvers.mapResolver())
                .build();

        Map<String, String> data = new HashMap<>();
        data.put("name", "Al");
        data.put("price", "100");
        engine.putTemplate("detail", engine.parse("<strong>{name}</strong>:{price}"));
        assertEquals("<strong>Al</strong>:100",
                engine.parse("{#include detail/}").render(data));
    }

}
