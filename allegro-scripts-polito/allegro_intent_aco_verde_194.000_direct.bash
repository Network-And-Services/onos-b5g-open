#! /bin/bash

#post OPTICAL intents from ASE to OSA
curl -u karaf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
   "appId": "org.onosproject.optical-rest",
   "ingressPoint": {
     "device": "netconf:192.168.88.11:830",
     "port": "104"
   },
   "egressPoint": {
     "device": "netconf:192.168.88.12:830",
     "port": "105"
   },
   "bidirectional": false,
   "signal": {
     "channelSpacing": "CHL_50GHZ",
     "gridType": "DWDM",
     "spacingMultiplier": 18,
     "slotGranularity": 4
   },
   "suggestedPath": {
     "links": [
       {
         "src": "netconf:192.168.88.11:830/102",
         "dst": "netconf:192.168.88.12:830/103"
       }
     ]
   }
 }' 'http://localhost:8181/onos/optical/intents'




