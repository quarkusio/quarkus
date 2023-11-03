package io.quarkus.it.jpa.mapping.xml.modern.app.xmlmappingonly.dirtychecking;

public class MyEmbeddable {

    private String embeddedBasic;

    public String getEmbeddedBasic() {
        return embeddedBasic;
    }

    public void setEmbeddedBasic(String embeddedBasic) {
        this.embeddedBasic = embeddedBasic;
    }
}
