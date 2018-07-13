package org.jboss.shamrock.runtime;

public class RuntimeInjector {

    private static volatile InjectionFactory factory = new DefaultInjectionFactory();

    public static <T> InjectionInstance<T> newFactory(Class<T> type) {
        return factory.create(type);
    }

    public static void setFactory(InjectionFactory f) {
        factory = f;
    }

    private static class DefaultInjectionFactory implements InjectionFactory {
        @Override
        public <T> InjectionInstance<T> create(Class<T> type) {
            return new InjectionInstance<T>() {

                //TODO: sort out the ordering, although this is probably fine for a POC
                volatile InjectionInstance<T> delegate;

                @Override
                public T newInstance() {
                    if (factory == DefaultInjectionFactory.this) {
                        try {
                            return type.newInstance();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    } else if (delegate == null) {
                        synchronized (this) {
                            if (delegate == null) {
                                delegate = factory.create(type);
                            }
                        }
                    }
                    return delegate.newInstance();

                }
            };
        }
    }
}
