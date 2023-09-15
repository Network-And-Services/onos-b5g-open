#!/bin/bash
sudo docker cp ../onos-xml/configTerminalDeviceCNIT-ADVA.xml octp1:/confd/examples.confd/OpenConfigTelemetry2.0/
sudo docker cp ../onos-xml/Makefile octp1:/confd/examples.confd/OpenConfigTelemetry2.0/

sudo docker cp ../onos-xml/configTerminalDeviceCNIT-ADVA.xml octp2:/confd/examples.confd/OpenConfigTelemetry2.0/
sudo docker cp ../onos-xml/Makefile octp2:/confd/examples.confd/OpenConfigTelemetry2.0/

