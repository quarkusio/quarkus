package org.jboss.shamrock.runtime;

public interface InjectionFactory {

    <T> InjectionInstance<T> create(Class<T> type);
}
