#! /bin/bash

#post openroadm ROADMs
onos-netcfg localhost ./b5g_roadm_devices-101.100.json
sleep 2

#post openconfig transponders
onos-netcfg localhost ./b5g_transponder_devices-101.100.json
sleep 5

#post links 
onos-netcfg localhost ./b5g_links-101.100.json
