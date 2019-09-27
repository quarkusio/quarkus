package io.quarkus.tika.runtime;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

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

    public void initTikaParser(BeanContainer container, TikaConfiguration config, List<String> supportedParserNames) {
        TikaParser parser = initializeParser(config, supportedParserNames);
        TikaParserProducer producer = container.instance(TikaParserProducer.class);
        producer.initialize(parser);
    }

    private TikaParser initializeParser(TikaConfiguration config, List<String> supportedParserNames) {
        TikaConfig tikaConfig = null;

        try (InputStream stream = getTikaConfigStream(config, supportedParserNames)) {
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

    private static InputStream getTikaConfigStream(TikaConfiguration config, List<String> supportedParserNames) {
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
            is = generateTikaConfig(supportedParserNames);
        }
        return is;
    }

    private static InputStream generateTikaConfig(List<String> supportedParserNames) {
        StringBuilder sb = new StringBuilder();
        sb.append("<properties>");
        sb.append("<parsers>");
        for (String parserName : supportedParserNames) {
            sb.append("<parser class=\"").append(parserName).append("\"/>");
        }
        sb.append("</parsers>");
        sb.append("</properties>");
        return new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
    }
}
