#!/usr/bin/env bash

set -e

# pruning is only run when inside CI to avoid accidentally removing stuff
# GITHUB_ACTIONS is always set to true inside Github Actions
# https://help.github.com/en/actions/configuring-and-managing-workflows/using-environment-variables
if [ "${GITHUB_ACTIONS}" == true ] ; then
  docker container prune -f
  docker image prune -f
  docker network prune -f
  docker volume prune -f
fi
