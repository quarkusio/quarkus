package org.jboss.shamrock.core;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public interface ArchiveProcessor {

    default Map<AttachmentKey<?>, MissingKeyBehaviour> getConsumedKeys() {
        return Collections.emptyMap();
    }

    default Set<AttachmentKey<?>> getProducedKeys() {
        return Collections.emptySet();
    }

    default Set<ListAttachmentKey> getConsumedCollections() {
        return Collections.emptySet();
    }

    default Set<ListAttachmentKey> getContributedCollections() {
        return Collections.emptySet();
    }



    enum MissingKeyBehaviour {
        DO_NOT_RUN,
        RUN,
        FAIL
    }
}
