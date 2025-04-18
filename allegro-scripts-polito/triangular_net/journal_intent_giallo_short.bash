#! /bin/bash

#post OPTICAL intents from ASE to OSA
curl -u karaf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
   "appId": "org.onosproject.optical-rest",
   "ingressPoint": {
     "device": "netconf:192.168.88.12:830",
     "port": "105"
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
         "src": "netconf:192.168.88.12:830/105",
         "dst": "netconf:192.168.88.31:830/4105"
       },
       {
         "src": "netconf:192.168.88.31:830/3001",
         "dst": "netconf:192.168.88.31:830/3001"
       },
       {
         "src": "netconf:192.168.88.31:830/5209",
         "dst": "netconf:192.168.88.33:830/4109"
       },
       {
         "src": "netconf:192.168.88.33:830/3001",
         "dst": "netconf:192.168.88.33:830/3001"
       },
       {
         "src": "netconf:192.168.88.33:830/5205",
         "dst": "netconf:192.168.88.12:830/105"
       }
     ]
   }
 }' 'http://localhost:8181/onos/optical/intents'




