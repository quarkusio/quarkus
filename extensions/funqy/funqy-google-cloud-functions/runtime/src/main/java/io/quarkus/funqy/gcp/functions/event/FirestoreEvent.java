package io.quarkus.funqy.gcp.functions.event;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Background function event for Firestore
 *
 * @see <a href="https://cloud.google.com/functions/docs/calling/cloud-firestore#event_structure">Firestore event structure</a>
 */
public class FirestoreEvent {
    public Document oldValue;
    public Document value;
    public UpdateMask updateMask;

    public static class Document {
        public LocalDateTime createTime;
        public String fields;
        public String name;
        public LocalDateTime updateTime;
    }

    public static class UpdateMask {
        public List<String> fieldPaths;
    }
}
