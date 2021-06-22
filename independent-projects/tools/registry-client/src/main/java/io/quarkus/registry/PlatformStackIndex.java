package io.quarkus.registry;

import io.quarkus.registry.union.UnionVersion;

public class PlatformStackIndex implements UnionVersion {

    public static PlatformStackIndex initial() {
        return new PlatformStackIndex(0, 0, 0);
    }

    private final int platformPriority;
    private final int streamPriority;
    private final int releasePriority;

    PlatformStackIndex(int platformPriority, int streamPriority, int releasePriority) {
        this.platformPriority = platformPriority;
        this.streamPriority = streamPriority;
        this.releasePriority = releasePriority;
    }

    @Override
    public int compareTo(UnionVersion o) {
        if (!(o instanceof PlatformStackIndex)) {
            return 0;
        }
        final PlatformStackIndex other = (PlatformStackIndex) o;
        if (platformPriority > other.platformPriority) {
            return 1;
        }
        if (platformPriority < other.platformPriority) {
            return -1;
        }
        if (streamPriority > other.streamPriority) {
            return 1;
        }
        if (streamPriority < other.streamPriority) {
            return -1;
        }
        if (releasePriority > other.releasePriority) {
            return 1;
        }
        if (releasePriority < other.releasePriority) {
            return -1;
        }
        return 0;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + platformPriority;
        result = prime * result + releasePriority;
        result = prime * result + streamPriority;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PlatformStackIndex other = (PlatformStackIndex) obj;
        if (platformPriority != other.platformPriority)
            return false;
        if (releasePriority != other.releasePriority)
            return false;
        if (streamPriority != other.streamPriority)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return platformPriority + "." + streamPriority + "." + releasePriority;
    }
}
