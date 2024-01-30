package io.quarkus.annotation.processor.generate_doc;

import java.io.IOException;
import java.io.Writer;
import java.text.Normalizer;
import java.util.List;

interface DocFormatter {
    default String getAnchor(String string) {
        // remove accents
        string = Normalizer.normalize(string, Normalizer.Form.NFKC)
                .replaceAll("[àáâãäåāąă]", "a")
                .replaceAll("[çćčĉċ]", "c")
                .replaceAll("[ďđð]", "d")
                .replaceAll("[èéêëēęěĕė]", "e")
                .replaceAll("[ƒſ]", "f")
                .replaceAll("[ĝğġģ]", "g")
                .replaceAll("[ĥħ]", "h")
                .replaceAll("[ìíîïīĩĭįı]", "i")
                .replaceAll("[ĳĵ]", "j")
                .replaceAll("[ķĸ]", "k")
                .replaceAll("[łľĺļŀ]", "l")
                .replaceAll("[ñńňņŉŋ]", "n")
                .replaceAll("[òóôõöøōőŏœ]", "o")
                .replaceAll("[Þþ]", "p")
                .replaceAll("[ŕřŗ]", "r")
                .replaceAll("[śšşŝș]", "s")
                .replaceAll("[ťţŧț]", "t")
                .replaceAll("[ùúûüūůűŭũų]", "u")
                .replaceAll("[ŵ]", "w")
                .replaceAll("[ýÿŷ]", "y")
                .replaceAll("[žżź]", "z")
                .replaceAll("[æ]", "ae")
                .replaceAll("[ÀÁÂÃÄÅĀĄĂ]", "A")
                .replaceAll("[ÇĆČĈĊ]", "C")
                .replaceAll("[ĎĐÐ]", "D")
                .replaceAll("[ÈÉÊËĒĘĚĔĖ]", "E")
                .replaceAll("[ĜĞĠĢ]", "G")
                .replaceAll("[ĤĦ]", "H")
                .replaceAll("[ÌÍÎÏĪĨĬĮİ]", "I")
                .replaceAll("[Ĵ]", "J")
                .replaceAll("[Ķ]", "K")
                .replaceAll("[ŁĽĹĻĿ]", "L")
                .replaceAll("[ÑŃŇŅŊ]", "N")
                .replaceAll("[ÒÓÔÕÖØŌŐŎ]", "O")
                .replaceAll("[ŔŘŖ]", "R")
                .replaceAll("[ŚŠŞŜȘ]", "S")
                .replaceAll("[ÙÚÛÜŪŮŰŬŨŲ]", "U")
                .replaceAll("[Ŵ]", "W")
                .replaceAll("[ÝŶŸ]", "Y")
                .replaceAll("[ŹŽŻ]", "Z")
                .replaceAll("[ß]", "ss");

        // Apostrophes.
        string = string.replaceAll("([a-z])'s([^a-z])", "$1s$2");
        // Allow only letters, -, _
        string = string.replaceAll("[^\\w-_]", "-").replaceAll("-{2,}", "-");
        // Get rid of any - at the start and end.
        string = string.replaceAll("-+$", "").replaceAll("^-+", "");

        return string.toLowerCase();
    }

    void format(Writer writer, String initialAnchorPrefix, boolean activateSearch, List<ConfigDocItem> configDocItems,
            boolean includeConfigPhaseLegend) throws IOException;

    void format(Writer writer, ConfigDocKey configDocKey) throws IOException;

    void format(Writer writer, ConfigDocSection configDocSection) throws IOException;
}
