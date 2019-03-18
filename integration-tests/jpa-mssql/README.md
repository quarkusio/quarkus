# JPA example with Microsoft SQL Server

## Running the tests

By default, the tests of this module are disabled.

To run the tests in a standard JVM with SQL Server started as a Docker container, you can run the following command:

```
mvn clean install -Ddocker -Dtest-mssql
```

To also test as a native image, add `-Dnative`:

```
mvn clean install -Ddocker -Dtest-mssql -Dnative
```

Alternatively you can connect to your own SQL Server.
Reconfigure the connection URL with `-Dmssqldb.url=jdbc:sqlserver://...`;
you'll probably want to change the authentication password too: `-Dmssqldb.sa-password=NotS0Secret`.

## Limitations

### Active Directory

Authentication to the server via Active Directory is a feature of the official JDBC driver, but this was disabled in Quarkus so to minimize the dependencies.

If you really need this feature, please let us know.

### Localization, Encoding an Character sets

SQL Server by default uses collation `SQL_Latin1_General_CP1_CI_AS` ; attempting to use this in a native-image will result in an error:

    java.io.UnsupportedEncodingException: "Codepage Cp1252 is not supported by the Java environment."

The solution is simple: you'll need to make sure your native images contains additional, non-default character sets.

Just set the flag `addAllCharsets` in the `native-image` configuration of your favourite build tool.
