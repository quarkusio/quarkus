package io.quarkus.it.data.hibernate;

import jakarta.data.metamodel.NumericAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;

@StaticMetamodel(Cat.class)
public class CatData {

    public static final TextAttribute<Cat> name = TextAttribute.of(Cat.class, "name");
    public static final NumericAttribute<Cat, Integer> age = NumericAttribute.of(Cat.class, "age", Integer.class);
    public static final TextAttribute<Cat> color = TextAttribute.of(Cat.class, "color");
}
