#! /bin/bash

#post OPTICAL intents from ASE to OSA
curl -u karaf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
   "appId": "org.onosproject.optical-rest",
   "ingressPoint": {
     "device": "netconf:10.100.101.21:2022",
     "port": "2002"
   },
   "egressPoint": {
     "device": "netconf:10.100.101.22:2022",
     "port": "2002"
   },
   "bidirectional": false,
   "signal": {
     "channelSpacing": "CHL_50GHZ",
     "gridType": "DWDM",
     "spacingMultiplier": 24,
     "slotGranularity": 4
   },
   "suggestedPath": {
     "links": [
       {
         "src": "netconf:10.100.101.21:2022/2002",
         "dst": "netconf:192.168.88.31:830/4102"
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
         "src": "netconf:192.168.88.32:830/5202",
         "dst": "netconf:192.168.88.32:830/4102"
       },
       {
         "src": "netconf:192.168.88.32:830/3001",
         "dst": "netconf:192.168.88.33:830/4102"
       },
       {
         "src": "netconf:192.168.88.33:830/5202",
         "dst": "netconf:10.100.101.22:2022/2002"
       }
     ]
   }
 }' 'http://localhost:8181/onos/optical/intents'




