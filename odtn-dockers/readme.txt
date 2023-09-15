#To run the topology for the first time
docker load --input oc.tar
docker load --input or.tar

sudo docker network create --driver=bridge --ip-range=10.100.101.0/24 --subnet=10.100.101.0/24 -o "com.docker.network.bridge.name=br0" netbr0

sudo ./createTopology.sh

#To restart the topology
sudo ./createTopology.sh

#To delete the topology
sudo ./removeTopo.sh

#--- Other useful commands
#save tar image
docker save image > image.tar

#load tar image
docker load --input image.tar

#delete
docker rmi image.tar

#lista immagini
docker images

#lista delle istanze attive
docker ps -a

#save tar.gz
docker save image:latest | gzip > image.tar.gz

#load tar.gz image
docker load < image.tar.gz

#create network hub for the devices
sudo docker network create --driver=bridge --ip-range=10.100.101.0/24 --subnet=10.100.101.0/24 -o "com.docker.network.bridge.name=br0" netbr0

#per lanciare la topologia
sudo ./createTopo.sh (2 tp e 3 roadm)

#entrare dentro un docker con una nuova bash
sudo docker exec -ti octp1 bash

#entrare dentro alla bash main del docker
sudo screen -r t1

Per uscire CNTRL+A+D

#vedere tutti gli screen
sudo screen -ls

#copiare file da a docker
sudo docker cp octp1:/confd/examples.confd/OpenConfigTelemetry2.0/configTerminalDeviceCNIT.xml ../onos-xml/
