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
 
Then you can define a specific connection URL with `-Ddynamodb.url=http://localhost:8000`

You can then run the tests as follows (either with `-Dnative` or not):

```
mvn clean install -Dtest-dynamodb -Ddynamodb.url=http://localhost:8000
```

Alternatively, you can run the tests against your AWS account.
Before you can use the AWS SDKs with DynamoDB, you must get an AWS access key ID and secret access key. 
For more information, see [Setting Up DynamoDB (Web Service)](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/SettingUp.DynamoWebService.html).

You can then run the tests
```
mvn clean install -Dtest-dynamodb -Ddynamodb.url -Dquarkus.dynamodb.region=eu-central-1 -Dquarkus.dynamodb.credentials-config.type=DEFAULT
```

- `-Ddynamodb.url` have to be empty in order to remove URL override from the test
- `-Dquarkus.dynamodb.region=eu-central-1` - define your AWS region
- `-Dquarkus.dynamodb.credentials-config.type=DEFAULT` enables default credentials provider chain that looks, among other places, in the environment variables