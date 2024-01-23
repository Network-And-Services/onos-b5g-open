#!/bin/bash

#Remove previous topology
./removeTopo.sh

echo "Creating transponders..."
echo "--- OCTP-1 screen t1 IP 10.100.101.121"
screen -dmS t1 -T xterm sh -c 'docker run --net=netbr0 --ip=10.100.101.121 --name octp1 -it octmm.img:latest bash'
sleep 2

echo "--- OCTP-2 screen t2 IP 10.100.101.122"
screen -dmS t2 -T xterm sh -c 'docker run --net=netbr0 --ip=10.100.101.122 --name octp2 -it octmm.img:latest bash'
sleep 2

echo "--- OCTP-3 screen t3 IP 10.100.101.123"
screen -dmS t3 -T xterm sh -c 'docker run --net=netbr0 --ip=10.100.101.123 --name octp3 -it octmm.img:latest bash'
sleep 2

echo "--- OCTP-4 screen t4 IP 10.100.101.124"
screen -dmS t4 -T xterm sh -c 'docker run --net=netbr0 --ip=10.100.101.124 --name octp4 -it octmm.img:latest bash'
sleep 2

echo "Creating ROADMs..."
echo "--- ROADM-1 screen r1 IP 10.100.101.111"
screen -dmS r1 -T xterm sh -c 'docker run --net=netbr0 --ip=10.100.101.111 --name roadm1 -it ornotifelement.img:latest bash'
sleep 2

echo "--- ROADM-2 screen r2 IP 10.100.101.112"
screen -dmS r2 -T xterm sh -c 'docker run --net=netbr0 --ip=10.100.101.112 --name roadm2 -it ornotifelement.img:latest bash'
sleep 2

echo "--- ROADM-3 screen r3 IP 10.100.101.113"
screen -dmS r3 -T xterm sh -c 'docker run --net=netbr0 --ip=10.100.101.113 --name roadm3 -it ornotifelement.img:latest bash'
sleep 2

echo "--- ROADM-4 screen r4 IP 10.100.101.114"
screen -dmS r4 -T xterm sh -c 'docker run --net=netbr0 --ip=10.100.101.114 --name roadm4 -it ornotifelement.img:latest bash'
sleep 2

echo "Uploading xml schema ../onos-xml/configTerminalDeviceCNIT40.xml"
sudo docker cp ./configTerminalDeviceCNIT40.xml octp1:/confd/examples.confd/OpenConfigTelemetry2.0/
sudo docker cp ./configTerminalDeviceCNIT40.xml octp2:/confd/examples.confd/OpenConfigTelemetry2.0/
sudo docker cp ./configTerminalDeviceCNIT40.xml octp3:/confd/examples.confd/OpenConfigTelemetry2.0/
sudo docker cp ./configTerminalDeviceCNIT40.xml octp4:/confd/examples.confd/OpenConfigTelemetry2.0/
sleep 2

echo "Uploading Makefile"
sudo docker cp ./Makefile-TerminalDeviceCNIT40 octp1:/confd/examples.confd/OpenConfigTelemetry2.0/Makefile
sudo docker cp ./Makefile-TerminalDeviceCNIT40 octp2:/confd/examples.confd/OpenConfigTelemetry2.0/Makefile
sudo docker cp ./Makefile-TerminalDeviceCNIT40 octp3:/confd/examples.confd/OpenConfigTelemetry2.0/Makefile
sudo docker cp ./Makefile-TerminalDeviceCNIT40 octp4:/confd/examples.confd/OpenConfigTelemetry2.0/Makefile
sleep 2

echo "Starting the Netconf agents"
sudo screen -S t1 -X stuff './startNetconfAgent.sh\n'
sudo screen -S t2 -X stuff './startNetconfAgent.sh\n'
sudo screen -S t3 -X stuff './startNetconfAgent.sh\n'
sudo screen -S t4 -X stuff './startNetconfAgent.sh\n'

echo "Uploading xml schema ../onos-xml/nodeTIM.xml"
sudo docker cp ./nodeTIM.xml roadm1:/confd/examples.confd/OpenROADMNotifElement/
sudo docker cp ./nodeTIM.xml roadm2:/confd/examples.confd/OpenROADMNotifElement/
sudo docker cp ./nodeTIM.xml roadm3:/confd/examples.confd/OpenROADMNotifElement/
sudo docker cp ./nodeTIM.xml roadm4:/confd/examples.confd/OpenROADMNotifElement/
sleep 2

echo "Uploading Makefile"
sudo docker cp ./Makefile-nodeTIM roadm1:/confd/examples.confd/OpenROADMNotifElement/Makefile
sudo docker cp ./Makefile-nodeTIM roadm2:/confd/examples.confd/OpenROADMNotifElement/Makefile
sudo docker cp ./Makefile-nodeTIM roadm3:/confd/examples.confd/OpenROADMNotifElement/Makefile
sudo docker cp ./Makefile-nodeTIM roadm4:/confd/examples.confd/OpenROADMNotifElement/Makefile
sleep 2

echo "Starting the Netconf agents"
sudo screen -S r1 -X stuff './startNetconfAgent.sh\n'
sudo screen -S r2 -X stuff './startNetconfAgent.sh\n'
sudo screen -S r3 -X stuff './startNetconfAgent.sh\n'
sudo screen -S r4 -X stuff './startNetconfAgent.sh\n'
