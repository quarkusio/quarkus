package org.jboss.resteasy.reactive.common.processor.scanning;

import java.util.List;

public class SerializerScanningResult {

    private final List<ScannedSerializer> readers;
    private final List<ScannedSerializer> writers;

    public SerializerScanningResult(List<ScannedSerializer> readers, List<ScannedSerializer> writers) {
        this.readers = List.copyOf(readers);
        this.writers = List.copyOf(writers);
    }

    public List<ScannedSerializer> getReaders() {
        return readers;
    }

    public List<ScannedSerializer> getWriters() {
        return writers;
    }
}
