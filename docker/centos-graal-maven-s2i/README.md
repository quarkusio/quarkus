# [Quarkus.io](http://quarkus.io) GraalVM Native S2I

## OpenShift

### OpenShift Build

    oc new-build https://github.com/quarkusio/quarkus.git --context-dir=docker/centos-graal-maven-s2i --name quarkus-native-s2i
    oc logs -f bc/quarkus-native-s2i

### OpenShift Use

    oc new-app quarkus-native-s2i~https://github.com/quarkusio/quarkus-quickstarts --context-dir=getting-started --name=getting-started-native
    oc logs -f bc/getting-started-native
    oc expose svc/getting-started-native

Note that GraalVM-based native build are more memory & CPU intensive than regular pure Java builds,
and you therefore may need to increase the quota for OpenShift's S2I builder containers.

## Locally (only for testing)

### Local Build

    docker build . -t quarkus-native-s2i

### Local use

    sudo dnf install source-to-image

    s2i build --copy ../../../quarkus-quickstarts/getting-started-native quarkus-native-s2i getting-started-native

    docker run --rm -it -p 8080:8080 getting-started-native

    curl http://localhost:8080/hello/greeting/quarkus
