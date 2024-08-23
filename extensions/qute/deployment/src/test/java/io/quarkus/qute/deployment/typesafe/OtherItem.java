package io.quarkus.qute.deployment.typesafe;

public class OtherItem {

    static final int ID = 1;
    static final int PRIMITIVE_ID = ID * -1;

    public Integer getId() {
        return ID;
    }

    public int getPrimitiveId() {
        return PRIMITIVE_ID;
    }

}
