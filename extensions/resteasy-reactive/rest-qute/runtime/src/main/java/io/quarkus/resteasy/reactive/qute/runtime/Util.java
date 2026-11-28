package io.quarkus.resteasy.reactive.qute.runtime;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;

import io.quarkus.qute.ChunkWriter;
import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateException;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.Variant;
import io.smallrye.mutiny.Uni;

final class Util {

    private Util() {
    }

    static Uni<byte[]> toUni(TemplateInstance instance, Engine engine) {
        Uni<byte[]> uni = Uni.createFrom().completionStage(() -> {
            // Collect the chunks of the rendered template and build the resulting byte array in a single bulk copy;
            // static text is handed over pre-encoded and never re-encoded
            ChunkCollector collector = new ChunkCollector();
            return instance.consume(collector).thenApply(unused -> collector.toByteArray());
        });
        if (!engine.useAsyncTimeout()) {
            // Make sure the timeout is always used
            long timeout = instance.getTimeout();
            uni = uni.ifNoItem().after(Duration.ofMillis(timeout))
                    .failWith(() -> new TemplateException(instance + " rendering timeout [" + timeout + "ms] occurred"));
        }
        return uni;
    }

    static final class ChunkCollector implements ChunkWriter {

        private final List<byte[]> chunks = new ArrayList<>();
        private int length;

        @Override
        public void write(String chunk) {
            byte[] encoded = chunk.getBytes(StandardCharsets.UTF_8);
            chunks.add(encoded);
            length += encoded.length;
        }

        @Override
        public void write(byte[] chunk) {
            chunks.add(chunk);
            length += chunk.length;
        }

        byte[] toByteArray() {
            byte[] out = new byte[length];
            int offset = 0;
            for (byte[] chunk : chunks) {
                System.arraycopy(chunk, 0, out, offset, chunk.length);
                offset += chunk.length;
            }
            return out;
        }

    }

    @SuppressWarnings("unchecked")
    static MediaType setSelectedVariant(TemplateInstance result,
            Request request, List<Locale> acceptableLanguages) {
        List<Variant> quteVariants = List.of();
        Object variantsAttr = result.getAttribute(TemplateInstance.VARIANTS);
        if (variantsAttr != null) {
            quteVariants = (List<Variant>) variantsAttr;
        } else {
            // If no variants are available then try to use the template variant
            Template template = result.getTemplate();
            if (template.getVariant().isPresent()) {
                quteVariants = List.of(template.getVariant().get());
            }
        }
        if (!quteVariants.isEmpty()) {
            List<jakarta.ws.rs.core.Variant> jaxRsVariants = new ArrayList<>(quteVariants.size());
            for (Variant variant : quteVariants) {
                jaxRsVariants.add(new jakarta.ws.rs.core.Variant(MediaType.valueOf(variant.getMediaType()), variant.getLocale(),
                        variant.getEncoding()));
            }
            jakarta.ws.rs.core.Variant selected = request
                    .selectVariant(jaxRsVariants);

            if (selected != null) {
                Locale selectedLocale = selected.getLanguage();
                if (selectedLocale == null) {
                    if (!acceptableLanguages.isEmpty()) {
                        selectedLocale = acceptableLanguages.get(0);
                    }
                }
                result.setAttribute(TemplateInstance.SELECTED_VARIANT,
                        new Variant(selectedLocale, selected.getMediaType().toString(),
                                selected.getEncoding()));
                return selected.getMediaType();
            }
        }
        return null;
    }
}
