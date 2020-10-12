== Logging JSON

Guide: https://quarkus.io/guides/logging

=== Example customization

Here are a few examples of customization of logging as JSON.
Place these in your src/main/resources/application.properties.

* Enable "pretty printing" of the JSON record. Note that some JSON parsers will fail to read pretty printed output.
```properties
quarkus.log.console.json.pretty-print=true
```

* Use a custom date format.
```properties
quarkus.log.console.json.date-format=YYYY-MM-dd HH:mm:ss
```

* More detailed exceptions in JSON logs
```properties
quarkus.log.console.json.exception-output-type=detailed-and-formatted
```
