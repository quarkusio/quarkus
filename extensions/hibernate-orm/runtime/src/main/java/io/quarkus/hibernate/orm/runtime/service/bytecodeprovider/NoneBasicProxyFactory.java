package io.quarkus.hibernate.orm.runtime.service.bytecodeprovider;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.spi.BasicProxyFactory;

final class NoneBasicProxyFactory implements BasicProxyFactory {

    private final Class superClassOrInterface;

    public NoneBasicProxyFactory(Class superClassOrInterface) {
        this.superClassOrInterface = superClassOrInterface;
    }

    @Override
    public Object getProxy() {
        throw new HibernateException("NoneBasicProxyFactory is unable to generate a BasicProxy for type "
                + superClassOrInterface + ". Enable a different BytecodeProvider.");
    }

}
