package io.quarkus.it.mongodb.panache;

import org.bson.codecs.pojo.annotations.BsonIgnore;

public class AccessorEntity extends GenericEntity {

    public byte b;
    public boolean bool;
    public char c;
    public double d;
    public float f;
    public int i;
    public long l;
    public short s;
    public String string;
    @BsonIgnore
    public Object trans;
    @BsonIgnore
    public Object trans2;

    transient int getBCalls = 0;
    transient int getTransCalls = 0;
    transient int setICalls = 0;
    transient int setTransCalls = 0;

    public byte getB() {
        getBCalls++;
        return b;
    }

    // explicit getter or setter

    public Object getTrans() {
        getTransCalls++;
        return trans;
    }

    public void setTrans(Object trans) {
        setTransCalls++;
        this.trans = trans;
    }

    public void method() {
        // touch some fields
        @SuppressWarnings("unused")
        byte b2 = b;
        i = 2;
        t = 1;
        t2 = 2;
    }

    public void setI(int i) {
        setICalls++;
        this.i = i;
    }
}
