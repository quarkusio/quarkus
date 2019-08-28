# Quarkus Jackson extension tester

This project verifies Jackson (de-)serialization support in native mode. 

This project consists of the following modules:

- model

    This module contains the library with a simple models. This model makes use 
    of Jackson to support (de-)serialization to JSON. Various forms of models 
    exists. At the time of writing the following models are present/tested:
    - Immutable models using a Builder to construct new instances.
    - Simple POJO model being registered for reflection.
    
    Unit tests are available to prove JVM based JSON (de-)serialization works 
    properly.

- service 

    This module contains a very simple RESTful resources with only a POST method
    to POST new models to this service. A simple unit test is included to verify
    correct behaviour for both unit and integration test.
    
    The following curl command can be used to send POST request to this service:
    
    ```
    curl -X POST -H "Content-Type: application/json" \
    -d '{"version": 2, "id": "123", "value": "val"}' \
    -v localhost:8080/<model-type>
    ```
  

## Build

To build the project, run the following command from the project root directory:

```
mvn clean package
```

This build should run correctly showing no errors and no test failures.

For the remainder make the service module your current working directory:

```
cd service
```

## Package JVM

Running a JVM based version of the service can either be done with `quarkus:dev` 
or by using the JVM based runner. 

- **Using `quarkus:dev`**
    ```
    mvn quarkus:dev
    ```

- **Using JVM runner**
    ```
    java -jar target/service-999-SNAPSHOT-runner.jar
    ```

In either case posting new model data like described earlier should result in 
a successful `201` response code with the posted message in the body. For example:

```
~$ curl -X POST -H "Content-Type: application/json" -d '{"version": 2, "id": "123", "value": "val"}' -v localhost:8080/model
Note: Unnecessary use of -X or --request, POST is already inferred.
*   Trying 127.0.0.1...
* TCP_NODELAY set
* Connected to localhost (127.0.0.1) port 8080 (#0)
> POST /model HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.58.0
> Accept: */*
> Content-Type: application/json
> Content-Length: 43
> 
* upload completely sent off: 43 out of 43 bytes
< HTTP/1.1 201 Created
< Connection: keep-alive
< Content-Type: application/json
< Content-Length: 38
< Date: Thu, 22 Aug 2019 13:31:48 GMT
< 
* Connection #0 to host localhost left intact
{"version":2,"id":"123","value":"val"}
```

## Package Native 

Checking proper behaviour can be achieved in the following 2 ways:
 
- **Integration test**

    This scenario requires no additional manual steps besides 
    executing the following command:   
    
    ```
    mvn integration-test verify -Pnative
    ```
 
    The application will be started automatically and test 
    scenario's will run. The output will indicate whether the
    test ran successfully or not. 
    
    In this scenario it is not possible to post new model data 
    manually. This can be achieved by using the next scenario. 

- **Native runner**

    Running the native version of the service manually like:

    ```
    mvn package -Pnative
    ...
    ./target/service-999-SNAPSHOT-runner
    ```
  
    Now the application is running new model data can be posted 
    like described earlier. This should result in a successful 
    `201` response code with the posted message in the response 
    body. Just like the JVM example given previously.
