#!/bin/sh

function cmd_create() {
  echo Creating function
  aws lambda create-function \
    --function-name ${FUNCTION_NAME} \
    --zip-file ${ZIP_FILE} \
    --handler ${HANDLER} \
    --runtime ${RUNTIME} \
    --role ${LAMBDA_ROLE_ARN} \
    ${LAMBDA_META}
}

function cmd_delete() {
  echo Deleting function
  aws lambda delete-function --function-name ${FUNCTION_NAME}
}

function cmd_invoke() {
  echo Invoking function
  aws lambda invoke response.txt \
    --function-name ${FUNCTION_NAME} \
    --payload file://payload.json \
    --log-type Tail \
    --query 'LogResult' \
    --output text |  base64 -d
}

function cmd_update() {
  echo Updating function
  aws lambda update-function-code \
    --function-name ${FUNCTION_NAME} \
    --zip-file ${ZIP_FILE}
}

FUNCTION_NAME=${resourceName}Function
HANDLER=io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest
RUNTIME=java8
ZIP_FILE=fileb://target/${artifactId}-${version}-runner.jar

function usage() {
  [ "_$1" == "_" ] && echo "\nUsage (JVM): \n$0 [create|delete|invoke]\ne.g.: $0 invoke"
  [ "_$1" == "_" ] && echo "\nUsage (Native): \n$0 native [create|delete|invoke]\ne.g.: $0 native invoke"

  [ "_" == "_`which aws 2>/dev/null`" ] && echo "\naws CLI not installed. Please see https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-install.html"
  [ ! -e $HOME/.aws/credentials ] && [ "_$AWS_ACCESS_KEY_ID" == "_" ] && echo "\naws configure not setup.  Please execute: aws configure"
  [ "_$LAMBDA_ROLE_ARN" == "_" ] && echo "\nEnvironment variable must be set: LAMBDA_ROLE_ARN\ne.g.: export LAMBDA_ROLE_ARN=arn:aws:iam::123456789012:role/my-example-role"
}

if [ "_$1" == "_" ] || [ "$1" == "help" ]
 then
  usage
fi

if [ "$1" == "native" ]
then
  RUNTIME=provided
  ZIP_FILE=fileb://target/function.zip
  FUNCTION_NAME=${resourceName}NativeFunction
  LAMBDA_META="--environment Variables={DISABLE_SIGNAL_HANDLERS=true}"
  shift
fi

while [ "$1" ]
do
  eval cmd_${1}
  shift
done

