package io.quarkus.arc.test.qualifiers.defaultvalues;

import jakarta.enterprise.context.Dependent;

@Dependent
@AnimalQualifier
public class Owl implements Animal {

    @Override
    public int noOfLeg() {
        return 2;
    }

}
