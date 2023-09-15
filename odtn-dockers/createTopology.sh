#!/bin/bash

#Remove previous topology
echo "Removing running agents"
./removeTopo.sh

echo "Creating TP: 10.100.101.2"
screen -dmS t1 -T xterm sh -c 'docker run --net=netbr0 --ip=10.100.101.2 --name octp1 -it octmm.img:latest ./startNetconfAgent.sh '
sleep 2

echo "Creating TP: 10.100.101.3"
screen -dmS t2 -T xterm sh -c 'docker run --net=netbr0 --ip=10.100.101.3 --name octp2 -it octmm.img:latest ./startNetconfAgent.sh '
sleep 2

echo "Creating ROADM: 10.100.101.12"
screen -dmS r1 -T xterm sh -c 'docker run --net=netbr0 --ip=10.100.101.12 --name roadm1 -it ornotifelement.img:latest  /bin/bash -c "python modifyMakefile.py 40client.xml; ./startNetconfAgent.sh "'
sleep 2

echo "Creating ROADM: 10.100.101.13"
screen -dmS r2 -T xterm sh -c 'docker run --net=netbr0 --ip=10.100.101.13 --name roadm2 -it ornotifelement.img:latest  /bin/bash -c "python modifyMakefile.py 40client.xml; ./startNetconfAgent.sh "'
sleep 2

echo "Creating ROADM: 10.100.101.14"
screen -dmS r3 -T xterm sh -c 'docker run --net=netbr0 --ip=10.100.101.14 --name roadm3 -it ornotifelement.img:latest  /bin/bash -c "python modifyMakefile.py 40client.xml; ./startNetconfAgent.sh "'

