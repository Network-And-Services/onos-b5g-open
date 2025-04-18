#! /bin/bash

#post network devices to OPTICAL controller
#onos-netcfg localhost ./ofc22_ase_devices.json

#sleep 3

onos-netcfg localhost ./allegro_polito_roadm_devices.json

sleep 3

onos-netcfg localhost ./allegro_polito_cassini_devices.json

sleep 100

#post network links to OPTICAL controller
onos-netcfg localhost ./allegro_links.json

