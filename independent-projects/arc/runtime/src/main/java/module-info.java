module io.quarkus.arc {
  requires jakarta.annotation;
  requires jakarta.cdi;
  requires jakarta.el;
  requires jakarta.transaction;
  requires io.smallrye.mutiny;
  requires org.jboss.logging;

  exports io.quarkus.arc;
}