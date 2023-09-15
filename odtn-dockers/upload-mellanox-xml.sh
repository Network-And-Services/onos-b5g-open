#!/bin/bash
sudo docker cp ../onos-xml/Mellanox.xml octp1:/confd/examples.confd/OpenConfigTelemetry2.0/
sudo docker cp ../onos-xml/Makefile-mellanox octp1:/confd/examples.confd/OpenConfigTelemetry2.0/Makefile

sudo docker cp ../onos-xml/Mellanox.xml octp2:/confd/examples.confd/OpenConfigTelemetry2.0/
sudo docker cp ../onos-xml/Makefile-mellanox octp2:/confd/examples.confd/OpenConfigTelemetry2.0/Makefile

