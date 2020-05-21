# Google Cloud Functions - HTTP Binding

This integration test has no automated test, it needs to be launched manually.

## Build the artifact

First, you need to log in to Google Cloud:

```shell script
gcloud auth login
```

Then you need to use Maven to build the artifact and put it in a directory where it will be the only file.

```shell script
mkdir deployment
mvn clean package
cp target/quarkus-integration-test-google-cloud-functions-http-999-SNAPSHOT-runner.jar deployment/
```

Finally, you need to use `gcloud` to deploy the function to Google Cloud

```shell script
gcloud beta functions deploy quarkus-example-http --entry-point=io.quarkus.gcp.functions.http.QuarkusHttpFunction \
  --runtime=java11 --trigger-http --source=deployment
```

## Testing the endpoints

After deploying to Google Cloud, the `gcloud` command will output the endpoint base location.

You can issue the following `curl` commands to test it:

```shell script
curl -v {httpsTrigger.url}/hello
curl -v {httpsTrigger.url}/servlet/hello
curl -v {httpsTrigger.url}/vertx/hello
```
