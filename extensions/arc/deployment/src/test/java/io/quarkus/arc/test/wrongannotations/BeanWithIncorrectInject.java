package io.quarkus.arc.test.wrongannotations;

import jakarta.enterprise.inject.spi.BeanManager;

import com.google.inject.Inject;

public class BeanWithIncorrectInject {

    @Inject
    BeanManager bm1;

    @javax.inject.Inject
    BeanManager bm2;

    @com.oracle.svm.core.annotate.Inject
    BeanManager bm3;

}
