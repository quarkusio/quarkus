# Example with AWS DynamoDB

## Running the tests

By default, the tests of this module are disabled.

To run the tests in a standard JVM with DynamoDB started as a Docker container, you can run the following command:

```
mvn clean install -Dtest-dynamodb -Ddocker
```

Additionally, you can generate a native image and run the tests for this native image by adding `-Dnative`:

```
mvn clean install -Dtest-dynamodb -Ddocker -Dnative
```

If you don't want to run DynamoDB as a Docker container, you can start your own [DynamoDB local server](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.DownloadingAndRunning.html) on any port.
 
Then you can define a specific connection URL with `-Ddynamodb-local.port=8888`

You can then run the tests as follows (either with `-Dnative` or not):

```
mvn clean install -Dtest-dynamodb -Ddynamodb-local.port=8888
```

Alternatively, you can run the tests against your AWS account.
AWS access key ID and secret key set to your AWS account set as environment variables 
```
export AWS_ACCESS_KEY_ID='YOUR_KEY'
export AWS_SECRET_ACCESS_KEY='YOUR_SECRET_KEY'
```
You can then run the tests as follows (either with `-Dnative` or not):
```
mvn clean install -Dtest-dynamodb -Ddynamodb.aws=true
```