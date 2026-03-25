#! /bin/bash

#post ROADM devices
onos-netcfg localhost ./allegro_polito_roadm_devices.json
echo "- 3 ROADMs"
sleep 60

#post network links among ROADMs
onos-netcfg localhost ./allegro_ecoc25_links.json
echo "- 6 unidirectional links among ROADMs"

sleep 5

#post transponders
onos-netcfg localhost ./phoenix_1.json
onos-netcfg localhost ./phoenix_2.json
echo "- 2 transponders"

sleep 60

#post network links between transponders and ROADMs
onos-netcfg localhost ./topo_phoenix_roadm_links.json

sleep 5



