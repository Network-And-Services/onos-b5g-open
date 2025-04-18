#! /bin/bash

#post network devices to OPTICAL controller
onos-netcfg localhost ./ofc22_ase_devices.json

sleep 3

onos-netcfg localhost ./ofc22_lum_devices.json

sleep 3

onos-netcfg localhost ./ofc22_cas_devices.json

sleep 5

#post network links to OPTICAL controller
onos-netcfg localhost ./ofc22_links.json

