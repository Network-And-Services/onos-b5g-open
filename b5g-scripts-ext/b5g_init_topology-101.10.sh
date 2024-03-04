#! /bin/bash

#post openroadm ROADMs
echo "Post openroadm ROADMs"
onos-netcfg localhost ./b5g_roadm_devices.json
sleep 2

#post openconfig transponders
echo "Post openconfig transponders"
onos-netcfg localhost ./b5g_transponder_devices.json
sleep 5

#post intradomain links 
echo "Post intra-domain links"
onos-netcfg localhost ./b5g_links.json

#post incoming interdomain links
echo "Post incoming inter-domain links"
onos-netcfg localhost ./b5g_links_interdomain_to_13.json 
