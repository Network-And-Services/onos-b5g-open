#! /bin/bash

#post OPTICAL intents from ASE to OSA
curl -u karaf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
   "appId": "org.onosproject.optical-rest",
   "ingressPoint": {
     "device": "netconf:10.30.2.44:2022",
     "port": "4"
   },
   "egressPoint": {
     "device": "netconf:10.30.2.54:2022",
     "port": "4"
   },
   "bidirectional": true,
   "signal": {
     "channelSpacing": "CHL_6P25GHZ",
     "gridType": "FLEX",
     "spacingMultiplier": 176,
     "slotGranularity": 4
   }
 }' 'http://localhost:8181/onos/optical/intents'

