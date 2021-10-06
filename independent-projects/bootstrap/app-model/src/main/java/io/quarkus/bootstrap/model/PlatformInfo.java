package io.quarkus.bootstrap.model;

import io.quarkus.maven.dependency.ArtifactCoords;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class PlatformInfo implements Serializable {

    private final String key;
    private final List<PlatformStreamInfo> streams = new ArrayList<>(1); // most of the time there will be only one

    public PlatformInfo(String key) {
        this.key = key;
    }

    public String getPlatformKey() {
        return key;
    }

    boolean isAligned(Collection<ArtifactCoords> importedBoms) {
        if (streams.isEmpty()) {
            return true;
        }
        if (streams.size() > 1) {
            return false;
        }
        return streams.get(0).isAligned(importedBoms);
    }

    List<List<String>> getPossibleAlignments(Collection<ArtifactCoords> importedPlatformBoms) {
        if (streams.size() > 1) {
            final StringBuilder buf = new StringBuilder();
            buf.append("Imported BOMs ");
            final Iterator<ArtifactCoords> it = importedPlatformBoms.iterator();
            if (it.hasNext()) {
                buf.append(it.next());
                while (it.hasNext()) {
                    buf.append(", ").append(it.next());
                }
            }
            buf.append(" belong to different platform streams ").append(streams.get(0));
            for (int i = 1; i < streams.size(); ++i) {
                buf.append(", ").append(streams.get(i));
            }
            throw new RuntimeException(buf.append(" while only one stream per platform is allowed.").toString());
        }
        return streams.get(0).getPossibleAlignemnts(importedPlatformBoms);
    }

    PlatformStreamInfo getOrCreateStream(String stream) {
        PlatformStreamInfo s = getStream(stream);
        if (s == null) {
            s = new PlatformStreamInfo(stream);
            streams.add(s);
        }
        return s;
    }

    Collection<PlatformStreamInfo> getStreams() {
        return streams;
    }

    PlatformStreamInfo getStream(String stream) {
        for (PlatformStreamInfo s : streams) {
            if (s.getId().equals(stream)) {
                return s;
            }
        }
        return null;
    }
}
