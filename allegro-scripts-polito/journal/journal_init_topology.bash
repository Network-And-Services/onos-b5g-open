#! /bin/bash

#post virtual devices 
onos-netcfg localhost ./journal_virtual_devices.json
sleep 3

#post Lumentum ROADMs
onos-netcfg localhost ./journal_lum_devices.json
sleep 3

#post Cassini transponders
onos-netcfg localhost ./journal_cas_devices.json
sleep 10

#post links 
onos-netcfg localhost ./journal_links.json

