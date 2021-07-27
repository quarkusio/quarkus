package com.demo.common.two.model;

import com.demo.common.base.model.BaseEntity;

import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.inject.Singleton;

@Singleton
public class SharedModelTwo implements BaseEntity {

    @Override
    public void someMethod() {

    }
}
