package io.quarkus.tika.runtime;

import java.io.InputStream;

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

    public void initTikaParser(BeanContainer container, TikaConfiguration config) {
        TikaParser parser = initializeParser(config);
        TikaParserProducer producer = container.instance(TikaParserProducer.class);
        producer.initialize(parser);
    }

    private TikaParser initializeParser(TikaConfiguration config) {
        // Load tika-config.xml resource
        TikaConfig tikaConfig = null;
        InputStream is = getClass().getResourceAsStream(
                config.tikaConfigPath.startsWith("/") ? config.tikaConfigPath : "/" + config.tikaConfigPath);
        if (is != null) {
            try (InputStream stream = is) {
                tikaConfig = new TikaConfig(stream);
            } catch (Exception ex) {
                final String errorMessage = "Invalid tika-config.xml";
                throw new TikaParseException(errorMessage, ex);
            }
        } else {
            tikaConfig = TikaConfig.getDefaultConfig();
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
}
