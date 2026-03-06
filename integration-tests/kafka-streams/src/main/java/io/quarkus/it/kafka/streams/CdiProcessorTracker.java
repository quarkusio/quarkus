package io.quarkus.it.kafka.streams;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CdiProcessorTracker {

    private final List<String> processedValues = new CopyOnWriteArrayList<>();

    public void track(String value) {
        processedValues.add(value);
    }

    public List<String> getProcessedValues() {
        return processedValues;
    }

    public int getProcessedCount() {
        return processedValues.size();
    }
}
