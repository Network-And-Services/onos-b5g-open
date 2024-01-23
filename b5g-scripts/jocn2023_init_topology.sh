#! /bin/bash

#post lumentum ROADM-20
onos-netcfg localhost ./push_lumentum_device.json
sleep 1

#post openroadm ROADMs
onos-netcfg localhost ./b5g_roadm_mini_devices.json
sleep 3

#post openconfig transponders
#onos-netcfg localhost ./b5g_transponder_mini_devices.json
#sleep 5

#post links 
onos-netcfg localhost ./jocn2023_mini_links.json
