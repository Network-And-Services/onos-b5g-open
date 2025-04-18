#! /bin/bash

#post OPTICAL intents from ASE to OSA
curl -u karaf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
   "appId": "org.onosproject.optical-rest",
   "ingressPoint": {
     "device": "netconf:10.100.101.21:2022",
     "port": "2003"
   },
   "egressPoint": {
     "device": "netconf:10.100.101.22:2022",
     "port": "2003"
   },
   "bidirectional": false,
   "signal": {
     "channelSpacing": "CHL_50GHZ",
     "gridType": "DWDM",
     "spacingMultiplier": 16,
     "slotGranularity": 4
   },
   "suggestedPath": {
     "links": [
       {
         "src": "netconf:10.100.101.21:2022/2003",
         "dst": "netconf:192.168.88.31:830/4103"
       },
       {
         "src": "netconf:192.168.88.31:830/3001",
         "dst": "netconf:192.168.88.31:830/3001"
       },
       {
         "src": "netconf:192.168.88.31:830/5202",
         "dst": "netconf:192.168.88.32:830/3001"
       },
       {
         "src": "netconf:192.168.88.32:830/5201",
         "dst": "netconf:10.100.101.22:2022/2003"
       }
     ]
   }
 }' 'http://localhost:8181/onos/optical/intents'




