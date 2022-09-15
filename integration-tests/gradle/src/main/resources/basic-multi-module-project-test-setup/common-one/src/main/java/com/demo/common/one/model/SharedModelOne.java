package com.demo.common.one.model;

import com.demo.common.base.model.BaseEntity;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Singleton;

@Alternative
@Priority(1)
@Singleton
public class SharedModelOne implements BaseEntity {

    @Override
    public void someMethod() {

    }
}
