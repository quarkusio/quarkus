package io.quarkus.opentelemetry.deployment;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.logging.MDC;

import io.quarkus.arc.Unremovable;

@Unremovable
@ApplicationScoped
public class TestMdcCapturer {
    private final List<MdcEntry> mdcEntries = new ArrayList<>();

    public void reset() {
        synchronized (this) {
            mdcEntries.clear();
        }
    }

    public void captureMdc() {
        mdcEntries.add(new MdcEntry(
                Boolean.parseBoolean(String.valueOf(MDC.get("sampled"))),
                String.valueOf(MDC.get("parentId")),
                String.valueOf(MDC.get("spanId")),
                String.valueOf(MDC.get("traceId"))));
    }

    public List<MdcEntry> getCapturedMdcEntries() {
        synchronized (this) {
            return List.copyOf(mdcEntries);
        }
    }
}
