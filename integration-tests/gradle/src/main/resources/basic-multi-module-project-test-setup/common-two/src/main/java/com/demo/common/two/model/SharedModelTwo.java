package com.demo.common.two.model;

import com.demo.common.base.model.BaseEntity;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Singleton;

@Singleton
public class SharedModelTwo implements BaseEntity {

    @Override
    public void someMethod() {

    }
}
