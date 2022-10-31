package io.quarkus.qute.deployment.typesafe;

public class ItemWithName {
    private final Name name;

    public ItemWithName(Name name) {
        this.name = name;
    }

    public Integer getId() {
        return 2;
    }

    public int getPrimitiveId() {
        return getId() * -1;
    }

    public Name getName() {
        return name;
    }

    public static class Name {

        public String toUpperCase() {
            return OrOperatorTemplateExtensionTest.ITEM_NAME.toUpperCase();
        }

        public String pleaseMakeMyCaseUpper() {
            return "UPPER CASE";
        }
    }
}
