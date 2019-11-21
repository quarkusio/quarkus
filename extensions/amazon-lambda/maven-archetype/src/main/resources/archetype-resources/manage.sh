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
    --payload file://payload.json
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

