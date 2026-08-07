package io.quarkus.arc.impl;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@Label("Arc Shutdown Action")
@Category({ "Quarkus", "Arc", "Shutdown" })
@Name("quarkus.arc.ShutdownAction")
@Description("An action executed during ArC container shutdown")
@StackTrace(false)
class ArcShutdownActionEvent extends Event {

    @Label("Action Type")
    public String actionType;

    @Label("Info")
    public String info;

}
