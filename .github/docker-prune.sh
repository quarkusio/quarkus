#!/usr/bin/env bash

set -e

# pruning is only run when inside CI to avoid accidentally removing stuff
# GITHUB_ACTIONS is always set to true inside Github Actions
# https://docs.github.com/en/actions/learn-github-actions/environment-variables
#if [ "${GITHUB_ACTIONS}" == true ] ; then
#  docker container prune -f
#  docker image prune -f
#  docker network prune -f
#  docker volume prune -f
#fi

echo "docker-prune.sh is disabled for now"
