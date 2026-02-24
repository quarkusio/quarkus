package io.quarkus.runtime.shutdown;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@Label("Shutdown Action")
@Category({ "Quarkus", "Shutdown" })
@Name("quarkus.ShutdownAction")
@Description("An action executed during Quarkus shutdown sequence")
@StackTrace(false)
public class ShutdownActionEvent extends Event {

    @Label("Action Type")
    public String actionType;

    @Label("Info")
    public String info;

}
