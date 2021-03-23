package io.quarkus.hibernate.orm.runtime.boot.xml;

import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappings;
import org.hibernate.boot.jaxb.spi.Binding;

import io.quarkus.runtime.annotations.RecordableConstructor;

/**
 * A substitute to Hibernate ORM's "Binding",
 * which is actually a representation of a parsed XML mapping file (orm.xml or hbm.xml).
 * <p>
 * On contrary to Binding, this class can be serialized/deserialized by the BytecodeRecorder.
 */
public class RecordableXmlMapping {
    // The following two properties are mutually exclusive: exactly one of them is non-null.
    private final JaxbEntityMappings ormXmlRoot;
    private final JaxbHbmHibernateMapping hbmXmlRoot;

    private final SourceType originType;
    private final String originName;

    public static RecordableXmlMapping create(Binding<?> binding) {
        Object root = binding.getRoot();
        Origin origin = binding.getOrigin();
        if (root instanceof JaxbEntityMappings) {
            return new RecordableXmlMapping((JaxbEntityMappings) root, null, origin.getType(), origin.getName());
        } else if (root instanceof JaxbHbmHibernateMapping) {
            return new RecordableXmlMapping(null, (JaxbHbmHibernateMapping) root, origin.getType(), origin.getName());
        } else {
            throw new IllegalArgumentException("Unsupported mapping file root (unrecognized type): " + root);
        }
    }

    @RecordableConstructor
    public RecordableXmlMapping(JaxbEntityMappings ormXmlRoot, JaxbHbmHibernateMapping hbmXmlRoot, SourceType originType,
            String originName) {
        this.ormXmlRoot = ormXmlRoot;
        this.hbmXmlRoot = hbmXmlRoot;
        this.originType = originType;
        this.originName = originName;
    }

    @Override
    public String toString() {
        return "RecordableXmlMapping{" +
                "originName='" + originName + '\'' +
                '}';
    }

    public JaxbEntityMappings getOrmXmlRoot() {
        return ormXmlRoot;
    }

    public JaxbHbmHibernateMapping getHbmXmlRoot() {
        return hbmXmlRoot;
    }

    public SourceType getOriginType() {
        return originType;
    }

    public String getOriginName() {
        return originName;
    }

    public Binding<?> toHibernateOrmBinding() {
        Origin origin = new Origin(originType, originName);
        if (ormXmlRoot != null) {
            return new Binding<>(ormXmlRoot, origin);
        } else {
            return new Binding<>(hbmXmlRoot, origin);
        }
    }
}
