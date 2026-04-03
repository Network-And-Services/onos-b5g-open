#! /bin/bash

#set ONOS variables
./openroadm_p2mp_allowExternalFlowRules.bash
echo "- Configuring ONOS"
sleep 2

#post ROADM devices
onos-netcfg localhost ./openroadm_p2mp_4.json
echo "- Pushing 4 emulated ROADMs"
sleep 10

#post network links among ROADMs
onos-netcfg localhost ./openroadm_p2mp_links_4.json
echo "- Pushing 6 unidirectional links among ROADMs"

#sleep 5

#post transponders
#onos-netcfg localhost ./phoenix_1.json
#onos-netcfg localhost ./phoenix_2.json
#echo "- 2 transponders"

#sleep 60

#post network links between transponders and ROADMs
#onos-netcfg localhost ./topo_phoenix_roadm_links.json

#sleep 5



