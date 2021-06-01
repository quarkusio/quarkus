package io.quarkus.it.jpa.mapping.xml.modern.app.xmlmappingonly;

public class XmlMappingOnlyEmbeddable {

    private String embeddedBasic;

    public String getEmbeddedBasic() {
        return embeddedBasic;
    }

    public void setEmbeddedBasic(String embeddedBasic) {
        this.embeddedBasic = embeddedBasic;
    }
}
