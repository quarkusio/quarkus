#!/bin/bash

function cmd_invoke() {
  echo Invoking function

  inputFormat=""
  if [ $(aws --version | awk '{print substr($1,9)}' | cut -c1-1) -ge 2 ]; then inputFormat="--cli-binary-format raw-in-base64-out"; fi

  set -x

  aws lambda invoke response.txt \
    ${inputFormat} \
    --function-name ${FUNCTION_NAME} \
    --payload file://payload.json \
    --log-type Tail \
    --query 'LogResult' \
    --output text |  base64 --decode
  { set +x; } 2>/dev/null
  cat response.txt && rm -f response.txt
}

FUNCTION_NAME=$2

function usage() {
  [ "_$1" == "_" ] && echo -e "\nUsage: \n$0 [invoke]\ne.g.: $0 invoke"

  [ "_" == "_`which aws 2>/dev/null`" ] && echo -e "\naws CLI not installed. Please see https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-install.html"
  [ ! -e $HOME/.aws/credentials ] && [ "_$AWS_ACCESS_KEY_ID" == "_" ] && echo -e "\naws configure not setup.  Please execute: aws configure"
  [ "_$LAMBDA_ROLE_ARN" == "_" ] && echo -e "\nEnvironment variable must be set: LAMBDA_ROLE_ARN\ne.g.: export LAMBDA_ROLE_ARN=arn:aws:iam::123456789012:role/my-example-role"
}

if [ "_$1" == "_" ] || [ "$1" == "help" ]
 then
  usage
fi

eval cmd_invoke
