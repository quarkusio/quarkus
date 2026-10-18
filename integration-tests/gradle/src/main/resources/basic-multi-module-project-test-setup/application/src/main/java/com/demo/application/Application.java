package com.demo.application;

import com.demo.common.base.model.BaseEntity;
import jakarta.enterprise.context.ApplicationScoped;

import jakarta.inject.Inject;

@ApplicationScoped
public class Application {

    @Inject
    BaseEntity model;

    void schedule() {


    }
}
