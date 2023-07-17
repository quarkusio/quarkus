package io.quarkus.it.jpa.mapping.xml.modern.app.xmlmappingonly.dirtychecking;

import java.util.ArrayList;
import java.util.List;

public class MyEntity {

    private Long id;

    private String basic;

    private MyEmbeddable embedded;

    private List<String> elementCollection = new ArrayList<>();

    private MyOtherEntity oneToOne;

    private MyOtherEntity manyToOne;

    private List<MyOtherEntity> oneToMany = new ArrayList<>();

    private List<MyOtherEntity> manyToMany = new ArrayList<>();

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

    public MyEmbeddable getEmbedded() {
        return embedded;
    }

    public void setEmbedded(MyEmbeddable embedded) {
        this.embedded = embedded;
    }

    public List<String> getElementCollection() {
        return elementCollection;
    }

    public void setElementCollection(List<String> elementCollection) {
        this.elementCollection = elementCollection;
    }

    public MyOtherEntity getOneToOne() {
        return oneToOne;
    }

    public void setOneToOne(MyOtherEntity oneToOne) {
        this.oneToOne = oneToOne;
    }

    public MyOtherEntity getManyToOne() {
        return manyToOne;
    }

    public void setManyToOne(MyOtherEntity manyToOne) {
        this.manyToOne = manyToOne;
    }

    public List<MyOtherEntity> getOneToMany() {
        return oneToMany;
    }

    public void setOneToMany(List<MyOtherEntity> oneToMany) {
        this.oneToMany = oneToMany;
    }

    public List<MyOtherEntity> getManyToMany() {
        return manyToMany;
    }

    public void setManyToMany(List<MyOtherEntity> manyToMany) {
        this.manyToMany = manyToMany;
    }
}
