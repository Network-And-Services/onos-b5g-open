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

#annotate interdomain links
echo "Annotate ports with inter-domain links"
./b5g_links_interdomain_to_13.bash

#annotate link lenghts
echo "Annotate links lengths"
./b5g_links_length.bash
