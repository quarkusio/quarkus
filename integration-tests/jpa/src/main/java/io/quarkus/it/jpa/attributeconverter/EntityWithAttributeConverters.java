package io.quarkus.it.jpa.attributeconverter;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class EntityWithAttributeConverters {
    @Id
    @GeneratedValue
    private Long id;

    @Convert(converter = MyDataRequiringCDIExplicitScopeConverter.class)
    private MyDataRequiringCDI myDataRequiringCDIExplicitScope;

    @Convert(converter = MyDataRequiringCDIImplicitScopeConverter.class)
    private MyDataRequiringCDI myDataRequiringCDIImplicitScope;

    @Convert(converter = MyDataNotRequiringCDINoInjectionConverter.class)
    private MyDataNotRequiringCDI myDataNotRequiringCDINoInjection;

    @Convert(converter = MyDataNotRequiringCDIVetoedConverter.class)
    private MyDataNotRequiringCDI myDataNotRequiringCDIVetoed;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MyDataRequiringCDI getMyDataRequiringCDIExplicitScope() {
        return myDataRequiringCDIExplicitScope;
    }

    public void setMyDataRequiringCDIExplicitScope(MyDataRequiringCDI myDataRequiringCDI) {
        this.myDataRequiringCDIExplicitScope = myDataRequiringCDI;
    }

    public MyDataRequiringCDI getMyDataRequiringCDIImplicitScope() {
        return myDataRequiringCDIImplicitScope;
    }

    public void setMyDataRequiringCDIImplicitScope(MyDataRequiringCDI myDataRequiringCDI) {
        this.myDataRequiringCDIImplicitScope = myDataRequiringCDI;
    }

    public MyDataNotRequiringCDI getMyDataNotRequiringCDINoInjection() {
        return myDataNotRequiringCDINoInjection;
    }

    public void setMyDataNotRequiringCDINoInjection(MyDataNotRequiringCDI myDataNotRequiringCDINoInjection) {
        this.myDataNotRequiringCDINoInjection = myDataNotRequiringCDINoInjection;
    }

    public MyDataNotRequiringCDI getMyDataNotRequiringCDIVetoed() {
        return myDataNotRequiringCDIVetoed;
    }

    public void setMyDataNotRequiringCDIVetoed(MyDataNotRequiringCDI myDataNotRequiringCDIVetoed) {
        this.myDataNotRequiringCDIVetoed = myDataNotRequiringCDIVetoed;
    }
}
