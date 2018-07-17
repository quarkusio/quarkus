module com.example {
    requires java.persistence;
    requires org.hibernate.orm.core;
    opens com.example to org.hibernate.orm.core;
}
