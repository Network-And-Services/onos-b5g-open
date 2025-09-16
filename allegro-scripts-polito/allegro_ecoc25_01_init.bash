#! /bin/bash

onos-netcfg localhost ./allegro_polito_roadm_devices.json
echo "- 3 ROADMs"

sleep 20

#post network links to OPTICAL controller
onos-netcfg localhost ./allegro_ecoc25_links.json
echo "- 6 unidirectional links"
