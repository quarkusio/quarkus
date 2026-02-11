package io.quarkus.it.corestuff.serialization;

import io.quarkus.bootstrap.graal.ImageInfo;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(serialization = true)
public class SerializationChildBuildTimeInit extends SerializationParentBuildTimeInit {

    public final static String value = "Child: " + (ImageInfo.inImageBuildtimeCode() ? "BUILD_TIME" : "RUN_TIME");

}
