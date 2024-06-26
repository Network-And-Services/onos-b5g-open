#! /bin/bash

#post openroadm ROADMs
echo "Post openroadm ROADMs"
onos-netcfg localhost ./b5g_roadm_devices-101.100.json
sleep 2

#post openconfig transponders
echo "Post openconfig transponders"
onos-netcfg localhost ./b5g_transponder_devices-101.100.json
sleep 5

#post links 
echo "Post incoming intra-domain links"
onos-netcfg localhost ./b5g_links-101.100.json
sleep 2

#annotate interdomain links
echo "Annotate ports with inter-domain links"
./b5g_links_interdomain_to_113.bash

#annotate link lenghts
echo "Annotate links lengths"
./b5g_links_length.bash

echo
