**WARNING:** all tests are disabled by default. 
If you want to run them, you need to configure a valid GCP project inside the `application.properties`.

## Spanner
To test Spanner you first need to have a running Spanner cluster named `test-instance`.

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

And finally you need to create a table named `Singers`.

You can do it with `gcloud`:
```
gcloud spanner databases ddl update test-database --instance test-instance \
  --ddl='CREATE TABLE Singers ( SingerId INT64 NOT NULL, FirstName STRING(1024), LastName STRING(1024), SingerInfo BYTES(MAX) ) PRIMARY KEY (SingerId)'
```

## PubSub
To test PubSub you first need to create a topic named `test-topic`

You can create one with `gcloud`:

```
gcloud pubsub topics create test-topic
```

As PubSub mandates the usage of the `GOOGLE_APPLICATION_CREDENTIALS` environment variable to define it's credential, 
you need to set this one instead of relying on the `quarkus.google.cloud.service-account-location` property. 

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