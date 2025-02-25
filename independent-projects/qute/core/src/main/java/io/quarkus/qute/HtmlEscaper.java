package io.quarkus.qute;

import java.util.List;
import java.util.Optional;

import io.quarkus.qute.TemplateNode.Origin;

public class HtmlEscaper extends CharReplacementResultMapper {

    private final List<String> escapedContentTypes;
    // NOTE: this holding 8 values to allow the JIT to remove the bound checks since
    // the replacement id is always in range [0, 7] due to the REPLACEMENT_ID_MASK value
    private static final String[] REPLACEMENTS = { null, "&quot;", "&#39;", "&amp;", "&lt;", "&gt;", null, null };
    // We use 4 bits to pack the replacement id for each Latin character in the [0, 255] range.
    // We could have used 3 bits but that would require more complex bit manipulation since 3 doesn't divide 8
    private static final byte[] LATIN_REPLACEMENT_ID_TABLE = createLatinReplacementData();
    private static final int REPLACEMENT_ID_MASK = 0b111;

    private static byte[] createLatinReplacementData() {
        byte[] data = new byte[256];
        // by default we don't escape anything i.e. the escaped index is 0!
        setLatinReplacementId(data, '"', 1);
        setLatinReplacementId(data, '\'', 2);
        setLatinReplacementId(data, '&', 3);
        setLatinReplacementId(data, '<', 4);
        setLatinReplacementId(data, '>', 5);
        assert getLatinReplacementId(data, '"') == 1;
        assert getLatinReplacementId(data, '\'') == 2;
        assert getLatinReplacementId(data, '&') == 3;
        assert getLatinReplacementId(data, '<') == 4;
        assert getLatinReplacementId(data, '>') == 5;
        return data;
    }

    private static void setLatinReplacementId(byte[] data, int c, int id) {
        if (c > 255) {
            throw new IllegalArgumentException("Only Latin characters are supported: " + c);
        }
        if (id < 0 || id > 15) {
            throw new IllegalArgumentException("Replacement ID must be in range [0, 15] but was: " + id);
        }
        data[c] = (byte) id;
    }

    private static int getLatinReplacementId(byte[] data, int c) {
        return data[c] & REPLACEMENT_ID_MASK;
    }

    private static String replacementOf(char c) {
        if (c > 255) {
            return null;
        }
        int replacementId = getLatinReplacementId(LATIN_REPLACEMENT_ID_TABLE, c & 0xFF);
        // in the super class we still have to perform a null check vs String, which means
        // we can have a branch misprediction there.
        // Here we anticipate such cost and if this method is going to be inlined we could still
        // correctly predict if the subsequent null check is going to be taken or not.
        if (replacementId == 0) {
            return null;
        }
        return REPLACEMENTS[replacementId];
    }

    public HtmlEscaper(List<String> escapedContentTypes) {
        this.escapedContentTypes = escapedContentTypes;
    }

    @Override
    public boolean appliesTo(Origin origin, Object result) {
        if (result instanceof RawString) {
            return false;
        }
        Optional<Variant> variant = origin.getVariant();
        if (variant.isPresent()) {
            return requiresDefaultEscaping(variant.get());
        }
        return false;
    }

    private boolean requiresDefaultEscaping(Variant variant) {
        String contentType = variant.getContentType();
        if (contentType == null) {
            return false;
        }
        for (String escaped : escapedContentTypes) {
            if (contentType.startsWith(escaped)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected String replacementFor(char c) {
        return replacementOf(c);
    }

}
