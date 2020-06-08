package com.demo.common.one.model;

import com.demo.common.base.model.BaseEntity;

import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.inject.Singleton;

@Alternative
@Priority(1)
@Singleton
public class SharedModelOne implements BaseEntity {

    @Override
    public void someMethod() {

    }
}
