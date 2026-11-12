package io.quarkus.qute;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Receives the rendered output in chunks. A chunk is either a {@link String} or a pre-encoded byte array.
 * <p>
 * Byte chunks are always encoded in the charset returned by {@link #charset()}. An implementation that cannot accept
 * byte chunks in this charset is never handed any; it only receives {@link String} chunks.
 * <p>
 * This interface is not a functional interface on purpose. It must be possible to overload
 * {@link TemplateInstance#consume(ChunkWriter)} without creating an ambiguity for lambda expressions passed to
 * {@link TemplateInstance#consume(java.util.function.Consumer)}.
 */
public interface ChunkWriter {

    /**
     * @param chunk a dynamic chunk of the rendered output
     */
    void write(String chunk);

    /**
     * @param chunk a pre-encoded chunk of the rendered output, encoded in {@link #charset()}
     */
    void write(byte[] chunk);

    /**
     * @return the charset used for pre-encoded byte chunks
     */
    default Charset charset() {
        return StandardCharsets.UTF_8;
    }

}
