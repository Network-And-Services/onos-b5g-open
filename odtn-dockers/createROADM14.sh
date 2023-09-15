#!/bin/bash

#Remove previous topology
echo "Removing running agents"
./removeTopo.sh

echo "Creating ROADM: 10.100.101.14"
screen -dmS r3 -T xterm sh -c 'docker run --net=netbr0 --ip=10.100.101.14 --name roadm3 -it ornotifelement.img:latest  /bin/bash -c "python modifyMakefile.py 40client.xml; bash "'

