package org.jboss.shamrock.runtime;

@Template
public class DefaultInjectionTemplate {

    public InjectionFactory defaultFactory() {
        return new DefaultInjectionFactory();
    }

    private static class DefaultInjectionFactory implements InjectionFactory {
        @Override
        public <T> InjectionInstance<T> create(Class<T> type) {
            return new InjectionInstance<T>() {

                @Override
                public T newInstance() {
                    try {
                        return type.newInstance();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        }
    }
}
