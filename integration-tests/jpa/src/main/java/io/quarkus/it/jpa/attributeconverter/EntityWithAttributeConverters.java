package io.quarkus.it.jpa.attributeconverter;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class EntityWithAttributeConverters {
    @Id
    @GeneratedValue
    private Long id;

    @Convert(converter = MyDataRequiringCDIConverter.class)
    private MyDataRequiringCDI myDataRequiringCDI;

    @Convert(converter = MyDataNotRequiringCDIConverter.class)
    private MyDataNotRequiringCDI myDataNotRequiringCDI;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MyDataRequiringCDI getMyDataRequiringCDI() {
        return myDataRequiringCDI;
    }

    public void setMyDataRequiringCDI(MyDataRequiringCDI myDataRequiringCDI) {
        this.myDataRequiringCDI = myDataRequiringCDI;
    }

    public MyDataNotRequiringCDI getMyDataNotRequiringCDI() {
        return myDataNotRequiringCDI;
    }

    public void setMyDataNotRequiringCDI(MyDataNotRequiringCDI myDataNotRequiringCDI) {
        this.myDataNotRequiringCDI = myDataNotRequiringCDI;
    }
}