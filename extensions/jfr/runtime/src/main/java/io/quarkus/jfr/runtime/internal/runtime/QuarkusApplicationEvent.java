package io.quarkus.jfr.runtime.internal.runtime;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.Period;
import jdk.jfr.StackTrace;

@Label("Quarkus Application")
@Category({ "Quarkus" })
@Name("quarkus.application")
@Description("Applications developed using Quarkus")
@StackTrace(false)
@Period
public class QuarkusApplicationEvent extends Event {

    @Label("Application Name")
    @Description("The name of application that is running")
    private String name;

    @Label("Application Version")
    @Description("The version of application that is running")
    private String version;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
