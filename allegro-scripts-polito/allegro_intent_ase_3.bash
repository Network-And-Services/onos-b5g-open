#! /bin/bash

#post OPTICAL intents from ASE to OSA
curl -u karaf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
   "appId": "org.onosproject.optical-rest",
   "uuid": "550e8400-e29b-41d4-a716-446655440000",
   "ingressPoint": {
     "device": "netconf:172.18.0.31:830",
     "port": "30"
   },
   "egressPoint": {
     "device": "netconf:172.18.0.32:830",
     "port": "11"
   },
   "bidirectional": false,
   "signal": {
     "channelSpacing": "CHL_6P25GHZ",
     "gridType": "FLEX",
     "spacingMultiplier": 436,
     "slotGranularity": 48
   },
   "suggestedPath": {
     "links": [
       {
       "src": "netconf:172.18.0.31:830/11",
       "dst": "netconf:172.18.0.33:830/10"
       },
       {
       "src": "netconf:172.18.0.33:830/21",
       "dst": "netconf:172.18.0.32:830/20"
       }
    ]
  }
}' 'http://localhost:8181/onos/optical/intents'




