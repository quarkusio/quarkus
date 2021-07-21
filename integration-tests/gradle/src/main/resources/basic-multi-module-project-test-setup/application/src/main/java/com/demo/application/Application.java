package com.demo.application;

import com.demo.common.base.model.BaseEntity;
import javax.enterprise.context.ApplicationScoped;

import javax.inject.Inject;

@ApplicationScoped
public class Application {

    @Inject
    BaseEntity model;

    void schedule() {


    }
}
