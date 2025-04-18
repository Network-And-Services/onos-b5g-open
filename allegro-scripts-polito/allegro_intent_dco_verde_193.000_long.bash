#! /bin/bash

#post OPTICAL intents from ASE to OSA
curl -u karaf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
   "appId": "org.onosproject.optical-rest",
   "ingressPoint": {
     "device": "netconf:192.168.88.11:830",
     "port": "102"
   },
   "egressPoint": {
     "device": "netconf:192.168.88.12:830",
     "port": "103"
   },
   "bidirectional": false,
   "signal": {
     "channelSpacing": "CHL_50GHZ",
     "gridType": "DWDM",
     "spacingMultiplier": -2,
     "slotGranularity": 4
   },
   "suggestedPath": {
     "links": [
       {
         "src": "netconf:192.168.88.11:830/102",
         "dst": "netconf:172.18.0.31:830/1011"
       },
       {
         "src": "netconf:172.18.0.31:830/21",
         "dst": "netconf:172.18.0.32:830/10"
       },
       {
         "src": "netconf:172.18.0.32:830/21",
	 "dst": "netconf:172.18.0.33:830/20"
       },
       {
         "src": "netconf:172.18.0.33:830/1010",
         "dst": "netconf:192.168.88.12:830/103"
       }
     ]
   }
 }' 'http://localhost:8181/onos/optical/intents'




