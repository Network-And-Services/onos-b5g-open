#! /bin/bash

#post openroadm ROADMs
echo "Post Lumentum ROADMs"
onos-netcfg localhost ./pdp_lumentum_devices_access.json
sleep 5

echo "Post TwinWSS ROADMs"
onos-netcfg localhost ./pdp_twinwss_devices.json
sleep 5

echo "Post Emulated ROADMs"
onos-netcfg localhost ./pdp_emulated_devices.json
sleep 5

echo "Post O-Band ROADMs"
onos-netcfg localhost ./pdp_oband_devices.json
sleep 5

echo "Post Filters"
onos-netcfg localhost ./pdp_emulated_filters.json
sleep 5

sleep 10

#post intradomain links 
echo "Post Intra-domain links"
#onos-netcfg localhost ./pdp_links_access_filters_cband.json
onos-netcfg localhost ./pdp_links_access_filters.complete.json
sleep 10

echo "Annotate links: lenght"
./pdp_annotate_links.sh
echo

echo "Annotate ports: inter-domain links"
./pdp_annotate_ports_access.sh
echo
