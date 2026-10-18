package org.acme.fieldannos;

public class FieldAnnoHolder {

    @FieldAnnoType(FieldAnnoValue.class)
    private String value;

    public String describe() {
        return "FieldAnnoHolder";
    }
}
