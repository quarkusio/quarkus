package io.quarkus.vertx.http.runtime.devmode;

import java.io.StringWriter;
import java.util.ArrayList;

class JsonArray {

    private final ArrayList<Object> list;

    JsonArray() {
        this.list = new ArrayList<Object>();
    }

    JsonArray put(JsonObject value) {
        list.add(value);
        return this;
    }

    void write(StringWriter writer, int indentFactor, int indent) {
        boolean needsComma = false;
        int length = list.size();
        writer.write('[');

        if (length == 1) {
            JsonObject.writeValue(writer, list.get(0), indentFactor, indent);
        } else if (length != 0) {
            int newIndent = indent + indentFactor;
            for (int i = 0; i < length; i += 1) {
                if (needsComma) {
                    writer.write(',');
                }
                if (indentFactor > 0) {
                    writer.write('\n');
                }
                JsonObject.indent(writer, newIndent);
                JsonObject.writeValue(writer, list.get(i), indentFactor, newIndent);
                needsComma = true;
            }
            if (indentFactor > 0) {
                writer.write('\n');
            }
            JsonObject.indent(writer, indent);
        }
        writer.write(']');
    }
}
