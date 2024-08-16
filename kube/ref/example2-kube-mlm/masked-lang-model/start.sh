#!/bin/bash
source /opt/conda/etc/profile.d/conda.sh
conda env list
exec "$@"
# sudo docker run --rm --name my-env-mlm-container env_mlm:v1
