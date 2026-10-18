package io.quarkus.jfr.runtime.internal.runtime;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.Period;
import jdk.jfr.StackTrace;

@Label("Quarkus Runtime")
@Category({ "Quarkus", "Runtime" })
@Name("quarkus.runtime")
@Description("Quarkus running")
@StackTrace(false)
@Period
public class QuarkusRuntimeEvent extends Event {

    @Label("Version")
    @Description("The version of Quarkus on which application is running")
    private String version;

    @Label("Image Mode")
    @Description("The image mode of Quarkus in which application is running")
    private String imageMode;

    @Label("Profiles")
    @Description("The profiles of Quarkus which application is running")
    private String profiles;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getImageMode() {
        return imageMode;
    }

    public void setImageMode(String imageMode) {
        this.imageMode = imageMode;
    }

    public String getProfiles() {
        return profiles;
    }

    public void setProfiles(String profiles) {
        this.profiles = profiles;
    }
}
