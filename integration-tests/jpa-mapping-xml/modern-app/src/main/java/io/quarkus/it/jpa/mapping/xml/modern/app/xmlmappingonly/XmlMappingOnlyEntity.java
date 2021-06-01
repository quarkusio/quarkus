package io.quarkus.it.jpa.mapping.xml.modern.app.xmlmappingonly;

import java.util.ArrayList;
import java.util.List;

public class XmlMappingOnlyEntity {

    private Long id;

    private String basic;

    private XmlMappingOnlyEmbeddable embedded;

    private List<String> elementCollection = new ArrayList<>();

    private XmlMappingOnlyOtherEntity oneToOne;

    private XmlMappingOnlyOtherEntity manyToOne;

    private List<XmlMappingOnlyOtherEntity> oneToMany = new ArrayList<>();

    private List<XmlMappingOnlyOtherEntity> manyToMany = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBasic() {
        return basic;
    }

    public void setBasic(String basic) {
        this.basic = basic;
    }

    public XmlMappingOnlyEmbeddable getEmbedded() {
        return embedded;
    }

    public void setEmbedded(XmlMappingOnlyEmbeddable embedded) {
        this.embedded = embedded;
    }

    public List<String> getElementCollection() {
        return elementCollection;
    }

    public void setElementCollection(List<String> elementCollection) {
        this.elementCollection = elementCollection;
    }

    public XmlMappingOnlyOtherEntity getOneToOne() {
        return oneToOne;
    }

    public void setOneToOne(XmlMappingOnlyOtherEntity oneToOne) {
        this.oneToOne = oneToOne;
    }

    public XmlMappingOnlyOtherEntity getManyToOne() {
        return manyToOne;
    }

    public void setManyToOne(XmlMappingOnlyOtherEntity manyToOne) {
        this.manyToOne = manyToOne;
    }

    public List<XmlMappingOnlyOtherEntity> getOneToMany() {
        return oneToMany;
    }

    public void setOneToMany(List<XmlMappingOnlyOtherEntity> oneToMany) {
        this.oneToMany = oneToMany;
    }

    public List<XmlMappingOnlyOtherEntity> getManyToMany() {
        return manyToMany;
    }

    public void setManyToMany(List<XmlMappingOnlyOtherEntity> manyToMany) {
        this.manyToMany = manyToMany;
    }
}
