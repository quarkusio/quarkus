package io.quarkus.avro.runtime;

import java.lang.reflect.Field;
import java.util.Map;

import org.apache.avro.specific.SpecificData;
import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class AvroRecorder {

    private static final Logger log = Logger.getLogger(AvroRecorder.class);

    public void clearStaticCaches() {
        try {
            Field instanceField = SpecificData.class.getDeclaredField("INSTANCE");
            instanceField.setAccessible(true);
            SpecificData data = (SpecificData) instanceField.get(null);
            Field classCache = SpecificData.class.getDeclaredField("classCache");
            classCache.setAccessible(true);
            Map<String, Class> classCacheMap = (Map<String, Class>) classCache.get(data);
            classCacheMap.clear();
        } catch (Throwable t) {
            log.error("Failed to clear Avro cache", t);
        }
    }
}
