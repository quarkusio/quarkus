## Spanner
To test spanner you first need to have a running Spanner cluster named `test-instance`.

You can create one with `gcloud`:
```
gcloud spanner instances create test-instance --config=regional-us-central1 \
    --description="Test Instance" --nodes=1
```

Then you need a database named `test-database`.

You can create one with `gcloud`:
```
gcloud spanner databases create test-database --instance test-instance
```

## PubSub
To test PubSub you first need to create a topic named `test-topic`

You can create one with `gcloud`:

```
gcloud pubsub topics create test-topic
```

As PubSub mandates the usage of the `GOOGLE_APPLICATION_CREDENTIALS` environment variable to define it's credential, 
you need to set this one instead of relying on of the `quarkus.google.cloud.service-account-location` property. 

```
export GOOGLE_APPLICATION_CREDENTIALS=<your-service-account-file>
```

## Storage
To test Storage you first need to create a bucket named `quarkus-hello` then upload a file `hello.txt` in it.
This file will be read by the test and return from the endpoint.

You can use `gsutil`:

```
gsutil mb gs://quarkus-hello
echo "Hello World!" > hello.txt
gsutil cp hello.txt gs://my-bucket
```