# Google Cloud Functions - Funqy Binding

This integration test has no automated test, it needs to be launched manually.

## Build the artifact

First, you need to log in to Google Cloud:

```shell script
gcloud auth login
```

Then you need to use Maven to build the artifact, the build will automatically copy it inside `target/deployment`.

```shell script
mvn clean package
```

Finally, you need to use `gcloud` to deploy the function to Google Cloud. The `gcloud` command is different for each
Background function so the set of instructions differs for each.

This example contains multiple Funqy functions, if you want to test a different function that the one defined inside
your `application.properties`, you can use the `--set-env-vars` option of `gcloud` to define the name of the function via the 
`QUARKUS_FUNQY_EXPORT` environment variable.

## Background function

### PubSub event

To deploy a background function that listen to PubSub event, you can use the following `gcloud` command:

```shell script
gcloud functions deploy quarkus-funqy-pubsub --entry-point=io.quarkus.funqy.gcp.functions.FunqyBackgroundFunction \
  --trigger-resource hello_topic --trigger-event google.pubsub.topic.publish \
  --runtime=java11 --source=target/deployment --set-env-vars=QUARKUS_FUNQY_EXPORT=helloPubSubWorld
```

You can then invoke your function via `gcloud`:

```shell script
gcloud functions call quarkus-funqy-pubsub --data '{"data":"HelloWorld"}'
```

### Storage event

To deploy a background function that listen to Storage event, you can use the following `gcloud` command:

```shell script
gcloud functions deploy quarkus-funqy-storage --entry-point=io.quarkus.funqy.gcp.functions.FunqyBackgroundFunction \
  --trigger-resource my_java11_gcs_bucket --trigger-event google.storage.object.finalize \
  --runtime=java11 --source=target/deployment --set-env-vars=QUARKUS_FUNQY_EXPORT=helloGCSWorld
```

You can then invoke your function via `gcloud`:

```shell script
gcloud functions call quarkus-funqy-storage --data '{"name":"hello.txt"}'
```