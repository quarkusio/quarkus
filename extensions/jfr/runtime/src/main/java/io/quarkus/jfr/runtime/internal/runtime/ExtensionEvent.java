package io.quarkus.jfr.runtime.internal.runtime;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.Period;
import jdk.jfr.StackTrace;

@Label("Quarkus Extension")
@Category({ "Quarkus", "Runtime" })
@Name("quarkus.extension")
@Description("Extension installed in Quarkus")
@StackTrace(false)
@Period
public class ExtensionEvent extends Event {

    @Label("Extension Name")
    @Description("The name of extension that is installed in Quarkus")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
