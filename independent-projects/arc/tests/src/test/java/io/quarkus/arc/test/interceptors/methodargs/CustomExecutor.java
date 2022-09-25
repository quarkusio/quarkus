package io.quarkus.arc.test.interceptors.methodargs;

import javax.inject.Singleton;

import io.quarkus.arc.test.interceptors.methodargs.base.BaseExecutor;

@Singleton
@Simple
public class CustomExecutor extends BaseExecutor {

}
