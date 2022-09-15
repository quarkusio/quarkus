package io.quarkus.arc.test.qualifiers.defaultvalues;

import jakarta.enterprise.context.Dependent;

@Dependent
@AnimalQualifier("cat")
public class Cat implements Animal {

    @Override
    public int noOfLeg() {
        return 4;
    }

}
