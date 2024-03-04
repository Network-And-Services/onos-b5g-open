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
     "channelSpacing": "CHL_6P25GHZ",
     "gridType": "FLEX",
     "spacingMultiplier": -230,
     "slotGranularity": 4
   },
   "suggestedPath": {
     "links": [
       {
         "src": "netconf:10.100.101.21:2022/11003",
         "dst": "netconf:10.100.101.11:2022/1011"
       },
       {
         "src": "netconf:10.100.101.11:2022/11",
         "dst": "netconf:10.100.101.14:2022/12"
       },
       {
         "src": "netconf:10.100.101.14:2022/1010",
         "dst": "netconf:10.100.101.24:2022/11003"
       }
     ]
   }
 }' 'http://localhost:8181/onos/optical/intents'

