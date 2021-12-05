package org.jboss.resteasy.reactive.server.processor.util;

import io.quarkus.gizmo.ClassOutput;
import java.util.ArrayList;
import java.util.List;

public class GeneratedClassOutput implements ClassOutput {

    final List<GeneratedClass> output = new ArrayList<>();

    @Override
    public void write(String name, byte[] data) {
        output.add(new GeneratedClass(name, data));
    }

    public List<GeneratedClass> getOutput() {
        return output;
    }
}
