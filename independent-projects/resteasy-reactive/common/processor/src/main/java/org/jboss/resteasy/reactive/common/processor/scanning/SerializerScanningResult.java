package org.jboss.resteasy.reactive.common.processor.scanning;

import java.util.ArrayList;
import java.util.List;

public class SerializerScanningResult {

    private final List<ScannedSerializer> readers;
    private final List<ScannedSerializer> writers;

    public SerializerScanningResult(List<ScannedSerializer> readers, List<ScannedSerializer> writers) {
        this.readers = new ArrayList<>(readers);
        this.writers = new ArrayList<>(writers);
    }

    public List<ScannedSerializer> getReaders() {
        return readers;
    }

    public List<ScannedSerializer> getWriters() {
        return writers;
    }
}
