package io.quarkus.devtools.codestarts;

import static java.util.Objects.requireNonNull;

import io.quarkus.devtools.codestarts.utils.NestedMaps;
import io.quarkus.devtools.messagewriter.MessageWriter;
import java.util.Collection;
import java.util.Map;

public class CodestartProjectInput {
    private final Collection<String> dependencies;
    private final Collection<String> boms;
    private final Map<String, Object> data;
    private final CodestartsSelection selection;
    private final MessageWriter messageWriter;

    protected CodestartProjectInput(final CodestartProjectInputBuilder builder) {
        this.dependencies = requireNonNull(builder.dependencies, "dependencies is required");
        this.boms = requireNonNull(builder.boms, "boms is required");
        this.selection = requireNonNull(builder.selection, "selection is required");
        this.data = NestedMaps.unflatten(requireNonNull(builder.data, "data is required"));
        this.messageWriter = requireNonNull(builder.messageWriter, "messageWriter is required");
    }

    public static CodestartProjectInputBuilder builder() {
        return new CodestartProjectInputBuilder();
    }

    public MessageWriter log() {
        return messageWriter;
    }

    public CodestartsSelection getSelection() {
        return selection;
    }

    public Collection<String> getDependencies() {
        return dependencies;
    }

    public Collection<String> getBoms() {
        return boms;
    }

    public Map<String, Object> getData() {
        return data;
    }
}
