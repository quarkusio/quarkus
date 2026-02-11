package io.quarkus.it.corestuff.serialization;

import java.io.Serializable;

import io.quarkus.bootstrap.graal.ImageInfo;

public class SerializationParentBuildTimeInit implements Serializable {

    public final static String value = "Parent: " + (ImageInfo.inImageBuildtimeCode() ? "BUILD_TIME" : "RUN_TIME");

}
