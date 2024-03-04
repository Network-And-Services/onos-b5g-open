#! /bin/bash

#post openroadm ROADMs
onos-netcfg localhost ./b5g_roadm_mini_devices.json
sleep 2

#post openconfig transponders
onos-netcfg localhost ./b5g_transponder_mini_devices.json
sleep 5

#post links 
onos-netcfg localhost ./b5g_mini_links.json
