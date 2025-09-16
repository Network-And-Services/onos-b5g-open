#! /bin/bash

#post openroadm ROADMs
echo "roadms..."
onos-netcfg localhost ./season_device.json
sleep 2

#post openconfig transponders
echo "transponders..."
onos-netcfg localhost ./season_transponders.json
sleep 5

#post links 
echo "links..."
onos-netcfg localhost ./season_links.json
