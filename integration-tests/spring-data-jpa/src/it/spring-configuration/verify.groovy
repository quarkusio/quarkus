//Check that file exits
String base = basedir
File mvnInvokerLog = new File(base, "build.log")
assert mvnInvokerLog.exists()

def contentFile = mvnInvokerLog.text
assert contentFile.contains("Quarkus does not support the spring.jpa.show-sql property you may try to use the Quarkus equivalent one : quarkus.hibernate-orm.log.sql.spring.jpa.show-sql property.")
assert contentFile.contains("Quarkus does not support the spring.jpa.data.hibernate.dialect property.")
