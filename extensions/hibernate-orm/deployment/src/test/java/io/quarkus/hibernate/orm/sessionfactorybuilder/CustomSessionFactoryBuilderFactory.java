package io.quarkus.hibernate.orm.sessionfactorybuilder;

import org.hibernate.SessionFactory;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.spi.AbstractDelegatingSessionFactoryBuilderImplementor;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryBuilderFactory;
import org.hibernate.boot.spi.SessionFactoryBuilderImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.internal.SessionFactoryImpl;

public class CustomSessionFactoryBuilderFactory implements SessionFactoryBuilderFactory {

    @Override
    public SessionFactoryBuilder getSessionFactoryBuilder(MetadataImplementor metadata,
            SessionFactoryBuilderImplementor defaultBuilder) {
        return new CustomSessionFactoryBuilder(metadata, defaultBuilder);
    }

    static class CustomSessionFactoryBuilder
            extends AbstractDelegatingSessionFactoryBuilderImplementor<CustomSessionFactoryBuilder> {

        private final MetadataImplementor metadata;

        CustomSessionFactoryBuilder(MetadataImplementor metadata, SessionFactoryBuilderImplementor delegate) {
            super(delegate);
            this.metadata = metadata;
        }

        @Override
        protected CustomSessionFactoryBuilder getThis() {
            return this;
        }

        @Override
        public SessionFactory build() {
            return new CustomSessionFactory(metadata, buildSessionFactoryOptions(),
                    metadata.getTypeConfiguration().getMetadataBuildingContext().getBootstrapContext());
        }
    }

    public static class CustomSessionFactory extends SessionFactoryImpl {

        CustomSessionFactory(MetadataImplementor metadata, SessionFactoryOptions options,
                BootstrapContext bootstrapContext) {
            super(metadata, options, bootstrapContext);
        }
    }
}
