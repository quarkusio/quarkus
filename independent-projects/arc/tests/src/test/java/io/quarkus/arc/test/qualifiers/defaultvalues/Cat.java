package io.quarkus.arc.test.qualifiers.defaultvalues;

import javax.enterprise.context.Dependent;

@Dependent
@AnimalQualifier("cat")
public class Cat implements Animal {

    @Override
    public int noOfLeg() {
        return 4;
    }

}
