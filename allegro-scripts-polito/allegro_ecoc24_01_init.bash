#! /bin/bash

onos-netcfg localhost ./allegro_polito_roadm_devices.json
echo "- 3 ROADMs"

sleep 2

onos-netcfg localhost ./allegro_polito_cassini_devices.json
echo "- 2 Cassini transponders"

sleep 2

sleep 10

#post network links to OPTICAL controller
onos-netcfg localhost ./allegro_links.json
echo "- 10 unidirectional links"
