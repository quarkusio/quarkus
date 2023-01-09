package io.quarkus.it.panache;

import jakarta.persistence.Entity;
import jakarta.persistence.Transient;

@Entity
public class AccessorEntity extends GenericEntity<Integer> {

    public String string;
    public char c;
    public boolean bool;
    public byte b;
    public short s;
    public int i;
    public long l;
    public float f;
    public double d;
    @Transient
    public Object trans;
    @Transient
    public Object trans2;

    // FIXME: those appear to be mapped by hibernate
    transient int getBCalls = 0;
    transient int setICalls = 0;
    transient int getTransCalls = 0;
    transient int setTransCalls = 0;

    public void method() {
        // touch some fields
        @SuppressWarnings("unused")
        byte b2 = b;
        i = 2;
        t = 1;
        t2 = 2;
    }

    // explicit getter or setter

    public byte getB() {
        getBCalls++;
        return b;
    }

    public void setI(int i) {
        setICalls++;
        this.i = i;
    }

    public Object getTrans() {
        getTransCalls++;
        return trans;
    }

    public void setTrans(Object trans) {
        setTransCalls++;
        this.trans = trans;
    }
}
