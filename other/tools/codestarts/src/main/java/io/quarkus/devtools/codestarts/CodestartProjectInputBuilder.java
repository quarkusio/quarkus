package io.quarkus.devtools.codestarts;

import io.quarkus.devtools.messagewriter.MessageWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CodestartProjectInputBuilder {
    Collection<String> dependencies = new ArrayList<>();
    Collection<String> boms = new ArrayList<>();
    CodestartsSelection selection = new CodestartsSelection();
    Map<String, Object> data = new HashMap<>();
    MessageWriter messageWriter = MessageWriter.info();

    protected CodestartProjectInputBuilder() {

    }

    public CodestartProjectInputBuilder addDependencies(Collection<String> dependencies) {
        this.dependencies.addAll(dependencies);
        return this;
    }

    public CodestartProjectInputBuilder addDependency(String dependency) {
        return this.addDependencies(Collections.singletonList(dependency));
    }

    public CodestartProjectInputBuilder addBoms(Collection<String> boms) {
        this.boms.addAll(boms);
        return this;
    }

    public CodestartProjectInputBuilder addCodestarts(Collection<String> codestarts) {
        this.selection.addNames(codestarts);
        return this;
    }

    public CodestartProjectInputBuilder addCodestart(String name) {
        this.selection.addName(name);
        return this;
    }

    public CodestartProjectInputBuilder addData(Map<String, Object> data) {
        this.data.putAll(data);
        return this;
    }

    public CodestartProjectInputBuilder putData(String key, Object value) {
        if (value != null) {
            this.data.put(key, value);
        }
        return this;
    }

    public CodestartProjectInputBuilder putData(DataKey dataKey, Object value) {
        return this.putData(dataKey.key(), value);
    }

    public CodestartProjectInputBuilder messageWriter(MessageWriter messageWriter) {
        this.messageWriter = messageWriter;
        return this;
    }

    public CodestartProjectInput build() {
        return new CodestartProjectInput(this);
    }

    public boolean containsData(DataKey dataKey) {
        return this.containsData(dataKey.key());
    }

    public boolean containsData(String key) {
        return this.data.containsKey(key);
    }
}
