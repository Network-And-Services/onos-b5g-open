#! /bin/bash

#post OPTICAL intents from ASE to OSA
curl -u karaf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
   "appId": "org.onosproject.optical-rest",
   "ingressPoint": {
     "device": "netconf:192.168.88.12:830",
     "port": "103"
   },
   "egressPoint": {
     "device": "netconf:192.168.88.12:830",
     "port": "103"
   },
   "bidirectional": false,
   "signal": {
     "channelSpacing": "CHL_50GHZ",
     "gridType": "DWDM",
     "spacingMultiplier": 38,
     "slotGranularity": 4
   },
   "suggestedPath": {
     "links": [
       {
         "src": "netconf:192.168.88.12:830/103",
         "dst": "netconf:192.168.88.31:830/4103"
       },
       {
         "src": "netconf:192.168.88.31:830/3001",
         "dst": "netconf:192.168.88.32:830/3001"
       },
       {
         "src": "netconf:192.168.88.32:830/5201",
         "dst": "netconf:192.168.88.33:830/3001"
       },
       {
         "src": "netconf:192.168.88.33:830/5203",
         "dst": "netconf:192.168.88.12:830/103"
       }
     ]
   }
 }' 'http://localhost:8181/onos/optical/intents'




