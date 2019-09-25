package io.quarkus.tika.runtime;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.RecursiveParserWrapper;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.tika.TikaParseException;
import io.quarkus.tika.TikaParser;

@Recorder
public class TikaRecorder {

    public void initTikaParser(BeanContainer container, TikaConfiguration config,
            Map<String, List<TikaParserParameter>> parserConfig) {
        TikaParser parser = initializeParser(config, parserConfig);
        TikaParserProducer producer = container.instance(TikaParserProducer.class);
        producer.initialize(parser);
    }

    private TikaParser initializeParser(TikaConfiguration config, Map<String, List<TikaParserParameter>> parserConfig) {
        TikaConfig tikaConfig = null;

        try (InputStream stream = getTikaConfigStream(config, parserConfig)) {
            tikaConfig = new TikaConfig(stream);
        } catch (Exception ex) {
            final String errorMessage = "Invalid tika-config.xml";
            throw new TikaParseException(errorMessage, ex);
        }

        // Create a native Tika Parser. AutoDetectParser is used by default but it is wrapped
        // by RecursiveParserWrapper if the appending of the embedded content is disabled
        Parser nativeParser = new AutoDetectParser(tikaConfig);
        if (!config.appendEmbeddedContent) {
            // the recursive parser will catch the embedded exceptions by default
            nativeParser = new RecursiveParserWrapper(nativeParser, true);
        }
        return new TikaParser(nativeParser, config.appendEmbeddedContent);
    }

    private static InputStream getTikaConfigStream(TikaConfiguration config,
            Map<String, List<TikaParserParameter>> parserConfig) {
        // Load tika-config.xml resource
        InputStream is = null;
        if (config.tikaConfigPath.isPresent()) {
            is = TikaRecorder.class.getResourceAsStream(
                    config.tikaConfigPath.get().startsWith("/") ? config.tikaConfigPath.get()
                            : "/" + config.tikaConfigPath.get());
            if (is == null) {
                final String errorMessage = "tika-config.xml can not be found at " + config.tikaConfigPath.get();
                throw new TikaParseException(errorMessage);
            }
        } else {
            is = generateTikaConfig(parserConfig);
        }
        return is;
    }

    private static InputStream generateTikaConfig(Map<String, List<TikaParserParameter>> parserConfig) {
        StringBuilder sb = new StringBuilder();
        sb.append("<properties>");
        sb.append("<parsers>");
        for (Entry<String, List<TikaParserParameter>> parserEntry : parserConfig.entrySet()) {
            sb.append("<parser class=\"").append(parserEntry.getKey()).append("\">");
            if (!parserEntry.getValue().isEmpty()) {
                appendParserParameters(sb, parserEntry.getValue());
            }
            sb.append("</parser>");
        }
        sb.append("</parsers>");
        sb.append("</properties>");
        return new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void appendParserParameters(StringBuilder sb, List<TikaParserParameter> parserParams) {
        sb.append("<params>");
        for (TikaParserParameter parserParam : parserParams) {
            sb.append("<param name=\"").append(parserParam.getName());
            sb.append("\" type=\"").append(parserParam.getType()).append("\">");
            sb.append(parserParam.getValue());
            sb.append("</param>");
        }
        sb.append("</params>");
    }
}
