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

    public void initTikaParserFromCustomConfiguration(BeanContainer container, TikaConfiguration config) {
        TikaParser parser = initializeParser(config, loadCustomTikaConfig(config));
        TikaParserProducer producer = container.instance(TikaParserProducer.class);
        producer.initialize(parser);
    }

    public void initTikaParserFromDefaultConfiguration(BeanContainer container, TikaConfiguration config) {
        TikaParser parser = initializeParser(config, loadDefaultTikaConfig(config));
        TikaParserProducer producer = container.instance(TikaParserProducer.class);
        producer.initialize(parser);
    }

    private static TikaParser initializeParser(TikaConfiguration config, TikaConfig nativeConfig) {
        // Create a native Tika Parser. AutoDetectParser is used by default but it is wrapped
        // by RecursiveParserWrapper if the appending of the embedded content is disabled
        Parser nativeParser = new AutoDetectParser(nativeConfig);
        if (!config.appendEmbeddedContent) {
            // the recursive parser will catch the embedded exceptions by default
            nativeParser = new RecursiveParserWrapper(nativeParser, true);
        }
        return new TikaParser(nativeParser, config.appendEmbeddedContent);
    }

    private static TikaConfig loadCustomTikaConfig(TikaConfiguration config) {
        // Load tika-config.xml resource
        final String tikaConfigPath = config.tikaConfigPath.get();
        InputStream is = TikaRecorder.class.getResourceAsStream(
                tikaConfigPath.startsWith("/") ? tikaConfigPath : "/" + tikaConfigPath);
        if (is != null) {
            try (InputStream stream = is) {
                return new TikaConfig(stream);
            } catch (Exception ex) {
                final String errorMessage = "Invalid tika-config.xml";
                throw new TikaParseException(errorMessage, ex);
            }
        } else {
            final String errorMessage = "tika-config.xml can not be found at " + tikaConfigPath;
            throw new TikaParseException(errorMessage);
        }
    }

    private static TikaConfig loadDefaultTikaConfig(TikaConfiguration config) {
        return TikaConfig.getDefaultConfig();
    }
}
