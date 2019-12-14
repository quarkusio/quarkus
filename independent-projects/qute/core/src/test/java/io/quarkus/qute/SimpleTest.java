package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.qute.Results.Result;
import io.quarkus.qute.TemplateNode.Origin;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;

public class SimpleTest {

    @Test
    public void testSimpleTemplate() {
        Map<String, String> item = new HashMap<>();
        item.put("name", "Lu");

        Map<String, Object> data = new HashMap<>();
        data.put("name", "world");
        data.put("test", Boolean.TRUE);
        data.put("list", ImmutableList.of(item));

        Engine engine = Engine.builder().addDefaultSectionHelpers().addDefaultValueResolvers()
                .build();

        Template template = engine.parse("{#if test}Hello {name}!{/}\n\n{#for item in list}{item.name}{/}");
        assertEquals("Hello world!\n\nLu", template.render(data));
    }

    @Test
    public void tesCustomValueResolver() {
        Engine engine = Engine.builder().addValueResolver(ValueResolvers.thisResolver()).addValueResolver(new ValueResolver() {

            @Override
            public boolean appliesTo(EvalContext context) {
                return context.getBase() instanceof List && context.getName().equals("get") && context.getParams().size() == 1;
            }

            @Override
            public CompletionStage<Object> resolve(EvalContext context) {
                List<?> list = (List<?>) context.getBase();
                return CompletableFuture.completedFuture(list.get(Integer.valueOf(context.getParams().get(0))));
            }

        }).build();

        Template template = engine.parse("{this.get(0)}");
        assertEquals("moon", template.render(ImmutableList.of("moon")));
    }

    @Test
    public void testDataNamespace() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "world");
        data.put("test", Boolean.TRUE);

        Engine engine = Engine.builder().addSectionHelper("if", new IfSectionHelper.Factory())
                .addValueResolver(ValueResolvers.mapResolver())
                .build();

        Template template = engine.parse("{#if test}{data:name}{/if}");
        assertEquals("world", template.render(data));
    }

    @Test
    public void testOrElseResolver() {
        Engine engine = Engine.builder().addValueResolver(ValueResolvers.mapResolver())
                .addValueResolver(ValueResolvers.orResolver())
                .build();
        Map<String, Object> data = new HashMap<>();
        data.put("surname", "Bug");
        data.put("foo", null);
        assertEquals("John Bug", engine.parse("{name.or('John')} {surname.or('John')}").render(data));
        assertEquals("John Bug", engine.parse("{name ?: 'John'} {surname or 'John'}").render(data));
        assertEquals("John Bug", engine.parse("{name ?: \"John Bug\"}").render(data));
        assertEquals("Is null", engine.parse("{foo ?: 'Is null'}").render(data));
    }

    @Test
    public void testTernaryOperator() {
        Engine engine = Engine.builder()
                .addValueResolvers(ValueResolvers.mapResolver(), ValueResolvers.trueResolver(),
                        ValueResolvers.orResolver())
                .build();

        Template template = engine
                .parse("{name ? 'Name true' : 'Name false'}. {surname ? 'Surname true' : foo}.");
        assertEquals("Name true. baz.", template.data("name", true).data("foo", "baz").render());
    }

    @Test
    public void testMissingValue() {
        Engine engine = Engine.builder().addValueResolver(ValueResolvers.thisResolver())
                .addValueResolver(ValueResolvers.mapResolver())
                .addSectionHelper(new IfSectionHelper.Factory())
                .build();
        Map<String, Object> data = new HashMap<>();
        data.put("surname", "Bug");
        assertEquals("OK", engine.parse("{#if this.get('name') is null}OK{/}").render(data));
    }

    @Test
    public void testDelimitersEscaping() {
        assertEquals("{{foo}} bar",
                Engine.builder().addValueResolver(ValueResolvers.thisResolver()).build().parse("{{foo}} {this}").render("bar"));
    }

    @Test
    public void testComment() {
        assertEquals("OK",
                Engine.builder().build().parse("{! This is my comment !}OK").render(null));
        assertEquals("<h1>Foo</h1>",
                Engine.builder().addDefaultSectionHelpers().build().parse("{#if true}\n" +
                        "<h1>Foo</h1>\n" +
                        "{! \n" +
                        "{#else}\n" +
                        "\n" +
                        " <h1>Bar</h1>\n" +
                        "\n" +
                        "!}"
                        + "{/}").render(null).trim());
        assertEquals("NOK",
                Engine.builder().addDefaultSectionHelpers().build()
                        .parse("{! {#if true}OK{/if} !}NOK").render(null));
    }

    @Test
    public void testEmptySectionTag() {
        assertEquals("",
                Engine.builder().addValueResolver(ValueResolvers.thisResolver())
                        .addSectionHelper(new IfSectionHelper.Factory()).build().parse("{#if true /}")
                        .render(Collections.emptyList()));
    }

    @Test
    public void testNotFound() {
        assertEquals("foo.bar Collection size: 0",
                Engine.builder().addDefaultValueResolvers()
                        .addResultMapper(new ResultMapper() {

                            public int getPriority() {
                                return 10;
                            }

                            public boolean appliesTo(Origin origin, Object val) {
                                return val.equals(Result.NOT_FOUND);
                            }

                            @Override
                            public String map(Object result, Expression expression) {
                                return expression.toOriginalString();
                            }
                        }).addResultMapper(new ResultMapper() {

                            public int getPriority() {
                                return 1;
                            }

                            public boolean appliesTo(Origin origin, Object val) {
                                return val.equals(Result.NOT_FOUND);
                            }

                            @Override
                            public String map(Object result, Expression expression) {
                                return "fooo";
                            }
                        }).addResultMapper(new ResultMapper() {

                            public int getPriority() {
                                return 1;
                            }

                            public boolean appliesTo(Origin origin, Object val) {
                                return val instanceof Collection;
                            }

                            @Override
                            public String map(Object result, Expression expression) {
                                Collection<?> collection = (Collection<?>) result;
                                return "Collection size: " + collection.size();
                            }
                        }).build()
                        .parse("{foo.bar} {this}")
                        .render(Collections.emptyList()));
    }

}
