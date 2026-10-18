package io.quarkus.qute.deployment;

import java.util.ArrayList;
import java.util.Arrays;

@SuppressWarnings("serial")
public class MyFooList extends ArrayList<Foo> {

    public MyFooList(Foo... foos) {
        super(Arrays.asList(foos));
    }

}
