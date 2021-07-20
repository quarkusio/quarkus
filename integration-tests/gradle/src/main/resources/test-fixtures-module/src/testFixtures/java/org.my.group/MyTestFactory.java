package com.example;

import org.my.group.MyFactory;
import org.my.group.MyService;

public class MyTestFactory implements MyFactory {

    @Override
    public MyService createMyService() {
        return () -> "test-fixtures";
    }

}
