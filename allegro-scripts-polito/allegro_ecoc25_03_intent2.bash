#! /bin/bash

#post OPTICAL intents from ASE to OSA
curl -u karaf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
   "appId": "org.onosproject.optical-rest",
   "uuid": "550e8400-e29b-41d4-a716-446675420000",
   "ingressPoint": {
     "device": "netconf:172.18.0.31:830",
     "port": "1021"
   },
   "egressPoint": {
     "device": "netconf:172.18.0.33:830",
     "port": "1020"
   },
   "bidirectional": true,
   "signal": {
     "channelSpacing": "CHL_6P25GHZ",
     "gridType": "FLEX",
     "spacingMultiplier": -16,
     "slotGranularity": 8
   },
   "suggestedPath": {
     "links": [
       {
       "src": "netconf:172.18.0.31:830/11",
       "dst": "netconf:172.18.0.33:830/10"
       }
    ]
  }
}' 'http://localhost:8181/onos/optical/intents'




