package io.quarkus.annotation.processor.generate_doc;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Represent one output file, its items are going to be appended to the file
 */
interface ConfigDoc {

    List<WriteItem> getWriteItems();

    /**
     * An item is a summary table, note below the table, ...
     */
    interface WriteItem {
        void accept(Writer writer) throws IOException;
    }
}
