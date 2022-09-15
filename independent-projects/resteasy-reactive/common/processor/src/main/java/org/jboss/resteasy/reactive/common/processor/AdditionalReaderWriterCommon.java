package org.jboss.resteasy.reactive.common.processor;

import jakarta.ws.rs.RuntimeType;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({ "rawtypes" })
abstract class AdditionalReaderWriterCommon implements AdditionalReaderWriter {

    private final List<Entry> entries = new ArrayList<>();

    public void add(String handlerClass, String mediaType, String entityClass,
            RuntimeType constraint) {

        Entry newEntry = new Entry(handlerClass, mediaType, entityClass, constraint);

        // we first attempt to "merge" readers if we encounter the same reader needed for both client and server
        Entry matchingEntryIgnoringConstraint = null;
        for (Entry entry : entries) {
            if (entry.matchesIgnoringConstraint(newEntry)) {
                matchingEntryIgnoringConstraint = entry;
                break;
            }
        }
        if (matchingEntryIgnoringConstraint != null) {
            if (matchingEntryIgnoringConstraint.getConstraint() != newEntry.getConstraint()) {
                // in this case we have a MessageBodyReader that applies to both client and server so
                // we remove the existing entity and replace it with one that has no constraint
                entries.remove(matchingEntryIgnoringConstraint);
                entries.add(new Entry(handlerClass, mediaType, entityClass, null));
            } else {
                // nothing to do since the entries match completely
            }
        } else {
            entries.add(newEntry);
        }
    }

    public List<Entry> get() {
        return entries;
    }

}
