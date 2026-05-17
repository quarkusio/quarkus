package io.quarkus.quickcli.annotations;

import java.util.Collections;
import java.util.Iterator;

/**
 * Default (no-op) completion candidates class used as the default value
 * for {@link Option#completionCandidates()}.
 */
public class NullCompletionCandidates implements Iterable<String> {
    @Override
    public Iterator<String> iterator() {
        return Collections.emptyIterator();
    }
}
