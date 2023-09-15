#!/bin/bash

#Remove previous topology
echo "Removing running agents"
./removeTopo.sh

echo "Creating TP: 10.100.101.2"
#screen -dmS t1 -T xterm sh -c 'docker run --net=netbr0 --ip=10.100.101.2 --name octp1 -it octmm.img:latest ./startNetconfAgent.sh '
screen -dmS t1 -T xterm sh -c 'docker run --net=netbr0 --ip=10.100.101.2 --name octp1 -it octmm.img:latest /bin/bash '
sleep 2

echo "Creating TP: 10.100.101.3"
#screen -dmS t2 -T xterm sh -c 'docker run --net=netbr0 --ip=10.100.101.3 --name octp2 -it octmm.img:latest ./startNetconfAgent.sh '
screen -dmS t2 -T xterm sh -c 'docker run --net=netbr0 --ip=10.100.101.3 --name octp2 -it octmm.img:latest /bin/bash '
sleep 2


