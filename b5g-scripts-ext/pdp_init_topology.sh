#! /bin/bash

#post openroadm ROADMs
echo "Post Lumentum ROADMs"
onos-netcfg localhost ./pdp_lumentum_devices.json
sleep 5

echo "Post TwinWSS ROADMs"
onos-netcfg localhost ./pdp_twinwss_devices.json
sleep 5

echo "Post Emulated ROADMs"
onos-netcfg localhost ./pdp_emulated_devices.json
sleep 30

#post intradomain links 
echo "Post intra-domain links"
onos-netcfg localhost ./pdp_links.json

