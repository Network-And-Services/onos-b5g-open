#! /bin/bash

#post OPTICAL intents from ASE to OSA
curl -u karaf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
   "appId": "org.onosproject.optical-rest",
   "ingressPoint": {
     "device": "netconf:10.100.101.21:2022",
     "port": "2001"
   },
   "egressPoint": {
     "device": "netconf:10.100.101.22:2022",
     "port": "2001"
   },
   "bidirectional": false,
   "signal": {
     "channelSpacing": "CHL_50GHZ",
     "gridType": "DWDM",
     "spacingMultiplier": 8,
     "slotGranularity": 4
   },
   "suggestedPath": {
     "links": [
       {
         "src": "netconf:10.100.101.21:2022/2001",
         "dst": "netconf:192.168.88.31:830/4101"
       },
       {
         "src": "netconf:192.168.88.31:830/3001",
         "dst": "netconf:192.168.88.31:830/3001"
       },
       {
         "src": "netconf:192.168.88.31:830/5201",
         "dst": "netconf:192.168.88.33:830/4101"
       },
       {
         "src": "netconf:192.168.88.33:830/3001",
         "dst": "netconf:192.168.88.33:830/3001"
       },
       {
         "src": "netconf:192.168.88.33:830/5201",
         "dst": "netconf:10.100.101.22:2022/2001"
       }
     ]
   }
 }' 'http://localhost:8181/onos/optical/intents'




