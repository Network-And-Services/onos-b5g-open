#! /bin/bash

#post openroadm ROADMs
onos-netcfg localhost ./ofcdemo2023_roadm_devices.json
sleep 2

#post openconfig transponders
onos-netcfg localhost ./ofcdemo2023_transponder_devices.json
sleep 5

#post links 
onos-netcfg localhost ./ofcdemo2023_links.json
