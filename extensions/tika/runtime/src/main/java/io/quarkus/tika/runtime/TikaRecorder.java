package io.quarkus.tika.runtime;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

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

    public void initTikaParser(BeanContainer container, TikaConfiguration config, String tikaXmlConfiguration) {
        TikaParser parser = initializeParser(config, tikaXmlConfiguration);
        TikaParserProducer producer = container.instance(TikaParserProducer.class);
        producer.initialize(parser);
    }

    private TikaParser initializeParser(TikaConfiguration config, String tikaXmlConfiguration) {
        TikaConfig tikaConfig;

        try (InputStream stream = getTikaConfigStream(config, tikaXmlConfiguration)) {
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

    private static InputStream getTikaConfigStream(TikaConfiguration config, String tikaXmlConfiguration) {
        // Load tika-config.xml resource
        InputStream is;
        if (config.tikaConfigPath.isPresent()) {
            is = TikaRecorder.class.getResourceAsStream(
                    config.tikaConfigPath.get().startsWith("/") ? config.tikaConfigPath.get()
                            : "/" + config.tikaConfigPath.get());
            if (is == null) {
                final String errorMessage = "tika-config.xml can not be found at " + config.tikaConfigPath.get();
                throw new TikaParseException(errorMessage);
            }
        } else {
            is = new ByteArrayInputStream(tikaXmlConfiguration.getBytes(StandardCharsets.UTF_8));
        }
        return is;
    }
}
