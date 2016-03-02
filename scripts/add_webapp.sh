#!/usr/bin/env bash
set -x
docker run -d --net haaas_default haaas/webapp:1.0.0 $@
