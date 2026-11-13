package io.quarkus.qute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

public class ChunkWriterTest {

    static class CollectingChunkWriter implements ChunkWriter {

        final List<Object> chunks = new ArrayList<>();

        @Override
        public void write(String chunk) {
            chunks.add(chunk);
        }

        @Override
        public void write(byte[] chunk) {
            chunks.add(chunk);
        }

        String asString() {
            StringBuilder builder = new StringBuilder();
            for (Object chunk : chunks) {
                if (chunk instanceof byte[] bytes) {
                    builder.append(new String(bytes, StandardCharsets.UTF_8));
                } else {
                    builder.append((String) chunk);
                }
            }
            return builder.toString();
        }

    }

    static CollectingChunkWriter consume(TemplateInstance instance) {
        CollectingChunkWriter writer = new CollectingChunkWriter();
        instance.consume(writer).toCompletableFuture().join();
        return writer;
    }

    @Test
    public void testStaticTextPreEncoded() {
        Engine engine = Engine.builder().addDefaults().build();
        TemplateInstance instance = engine.parse("Hello {name}!").instance().data("name", "world");

        CollectingChunkWriter writer = consume(instance);
        assertThat(writer.chunks).hasSize(3);
        assertThat(writer.chunks.get(0)).isEqualTo("Hello ".getBytes(StandardCharsets.UTF_8));
        assertThat(writer.chunks.get(1)).isEqualTo("world");
        assertThat(writer.chunks.get(2)).isEqualTo("!".getBytes(StandardCharsets.UTF_8));
        assertThat(writer.asString()).isEqualTo("Hello world!");
    }

    @Test
    public void testNonLatinStaticText() {
        Engine engine = Engine.builder().addDefaults().build();
        TemplateInstance instance = engine.parse("Привет {name}").instance().data("name", "мир");

        CollectingChunkWriter writer = consume(instance);
        assertThat(writer.chunks.get(0)).isEqualTo("Привет ".getBytes(StandardCharsets.UTF_8));
        assertThat(writer.chunks.get(1)).isEqualTo("мир");
        assertThat(writer.asString()).isEqualTo("Привет мир");
    }

    @Test
    public void testPreEncodedOnce() {
        Engine engine = Engine.builder().addDefaults().build();
        Template template = engine.parse("Hello {name}!");

        CollectingChunkWriter first = consume(template.instance().data("name", "world"));
        CollectingChunkWriter second = consume(template.instance().data("name", "qute"));

        // The same byte array must be reused by all renderings of a template
        assertThat(second.chunks.get(0)).isSameAs(first.chunks.get(0));
        assertThat(second.chunks.get(2)).isSameAs(first.chunks.get(2));
        assertThat(second.asString()).isEqualTo("Hello qute!");
    }

    @Test
    public void testChunkOutputMatchesStringRender() {
        Engine engine = Engine.builder().addDefaults().build();
        String templateString = "{#for item in items}<li>{item.name}</li>{/for}{#if active}ACTIVE{/if}";
        TemplateInstance instance = engine.parse(templateString).instance()
                .data("items", List.of(Map.of("name", "Lu"), Map.of("name", "Čolek")))
                .data("active", true);

        assertThat(consume(instance).asString()).isEqualTo(instance.render());
    }

    @Test
    public void testNonUtf8WriterReceivesStrings() {
        Engine engine = Engine.builder().addDefaults().build();
        TemplateInstance instance = engine.parse("Hello {name}!").instance().data("name", "world");

        CollectingChunkWriter writer = new CollectingChunkWriter() {

            @Override
            public Charset charset() {
                return StandardCharsets.ISO_8859_1;
            }

            @Override
            public void write(byte[] chunk) {
                throw new AssertionError("Pre-encoded chunks must not be handed over to a non-UTF-8 writer");
            }

        };
        instance.consume(writer).toCompletableFuture().join();
        // Static text must fall back to String chunks
        assertThat(writer.chunks).containsExactly("Hello ", "world", "!");
    }

    @Test
    public void testCustomResultNodeFallsBackToStrings() {
        Engine engine = Engine.builder().addDefaults()
                .addSectionHelper(new CustomSectionHelper.Factory()).build();
        TemplateInstance instance = engine.parse("a{#custom}{/custom}b").instance();

        CollectingChunkWriter writer = consume(instance);
        // A custom result node that only implements process(Consumer<String>) keeps working
        assertThat(writer.chunks).hasSize(3);
        assertThat(writer.chunks.get(0)).isEqualTo("a".getBytes(StandardCharsets.UTF_8));
        assertThat(writer.chunks.get(1)).isEqualTo("CUSTOM");
        assertThat(writer.chunks.get(2)).isEqualTo("b".getBytes(StandardCharsets.UTF_8));
        assertThat(writer.asString()).isEqualTo("aCUSTOMb");
    }

    @Test
    public void testErrorPropagation() {
        Engine engine = Engine.builder().addDefaults().strictRendering(true).build();
        TemplateInstance instance = engine.parse("Hello {missing}!").instance();

        CollectingChunkWriter writer = new CollectingChunkWriter();
        CompletionStage<Void> stage = instance.consume(writer);
        assertThatExceptionOfType(CompletionException.class)
                .isThrownBy(() -> stage.toCompletableFuture().join())
                .withCauseInstanceOf(TemplateException.class);
    }

    public static class CustomSectionHelper implements SectionHelper {

        @Override
        public CompletionStage<ResultNode> resolve(SectionResolutionContext context) {
            return CompletedStage.of(new ResultNode() {

                @Override
                public void process(Consumer<String> resultConsumer) {
                    resultConsumer.accept("CUSTOM");
                }

            });
        }

        public static class Factory implements SectionHelperFactory<CustomSectionHelper> {

            @Override
            public List<String> getDefaultAliases() {
                return List.of("custom");
            }

            @Override
            public CustomSectionHelper initialize(SectionInitContext context) {
                return new CustomSectionHelper();
            }

        }

    }

}
