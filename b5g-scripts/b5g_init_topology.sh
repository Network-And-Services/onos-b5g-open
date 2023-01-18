#! /bin/bash

#post openroadm ROADMs
onos-netcfg localhost ./b5g_roadm_devices.json
sleep 2

#post openconfig transponders
onos-netcfg localhost ./b5g_transponder_devices.json
sleep 5

#post links 
onos-netcfg localhost ./b5g_links.json
