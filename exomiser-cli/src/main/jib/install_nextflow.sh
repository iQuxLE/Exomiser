#!/bin/bash

curl -fsSL https://get.nextflow.io | bash

# Move the Nextflow binary to a directory in PATH
mv nextflow /usr/local/bin/

if [ $? -eq 0 ]; then
  echo "Nextflow successfully installed"
else
  echo "Nextflow installation failed"
fi

