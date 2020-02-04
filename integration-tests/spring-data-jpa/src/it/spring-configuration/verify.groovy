//Check that file exits
String base = basedir
File mvnInvokerLog = new File(base, "build.log")
assert mvnInvokerLog.exists()

def contentFile = mvnInvokerLog.text
assert contentFile.contains("Quarkus does not support the following Spring Boot configuration properties :")
assert contentFile.contains("- spring.jpa.show-sql should be replaced by quarkus.hibernate-orm.log.sql")
assert contentFile.contains("- spring.jpa.properties.hibernate.dialect should be replaced by quarkus.hibernate-orm.dialect")
assert contentFile.contains("- spring.jpa.open-in-view")
