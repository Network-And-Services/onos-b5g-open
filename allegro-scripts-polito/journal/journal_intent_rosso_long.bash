#! /bin/bash

#post OPTICAL intents from ASE to OSA
curl -u karaf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
   "appId": "org.onosproject.optical-rest",
   "ingressPoint": {
     "device": "netconf:192.168.88.11:830",
     "port": "104"
   },
   "egressPoint": {
     "device": "netconf:192.168.88.11:830",
     "port": "104"
   },
   "bidirectional": false,
   "signal": {
     "channelSpacing": "CHL_50GHZ",
     "gridType": "DWDM",
     "spacingMultiplier": -22,
     "slotGranularity": 4
   },
   "suggestedPath": {
     "links": [
       {
         "src": "netconf:192.168.88.11:830/104",
         "dst": "netconf:192.168.88.31:830/4104"
       },
       {
         "src": "netconf:192.168.88.31:830/3001",
         "dst": "netconf:192.168.88.31:830/3001"
       },
       {
         "src": "netconf:192.168.88.31:830/5210",
	 "dst": "netconf:192.168.88.32:830/3001"
       },
       {
         "src": "netconf:192.168.88.32:830/5210",
         "dst": "netconf:192.168.88.32:830/4110"
       },
       {
         "src": "netconf:192.168.88.32:830/3001",
         "dst": "netconf:192.168.88.33:830/4110"
       },
       {
         "src": "netconf:192.168.88.33:830/3001",
         "dst": "netconf:192.168.88.33:830/3001"
       },
       {
         "src": "netconf:192.168.88.33:830/5204",
         "dst": "netconf:192.168.88.11:830/104"
       }
     ]
   }
 }' 'http://localhost:8181/onos/optical/intents'




