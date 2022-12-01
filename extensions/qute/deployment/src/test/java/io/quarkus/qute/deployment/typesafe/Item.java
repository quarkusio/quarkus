package io.quarkus.qute.deployment.typesafe;

public class Item {

    String name;

    OtherItem[] otherItems;

    public Item(String name, OtherItem... otherItems) {
        this.name = name;
        this.otherItems = otherItems;
    }

    public String getName() {
        return name;
    }

    public OtherItem[] getOtherItems() {
        return otherItems;
    }

    public int getPrimitiveId() {
        return 9;
    }

}
