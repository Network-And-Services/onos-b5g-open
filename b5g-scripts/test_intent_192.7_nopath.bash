#! /bin/bash

#post OPTICAL intents from ASE to OSA
curl -u karaf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
   "appId": "org.onosproject.optical-rest",
   "ingressPoint": {
     "device": "netconf:10.100.101.21:2022",
     "port": "11003"
   },
   "egressPoint": {
     "device": "netconf:10.100.101.24:2022",
     "port": "11003"
   },
   "bidirectional": true,
   "signal": {
     "channelSpacing": "CHL_50GHZ",
     "gridType": "DWDM",
     "spacingMultiplier": -8,
     "slotGranularity": 4
   }
 }' 'http://193.205.83.89:8181/onos/optical/intents'

