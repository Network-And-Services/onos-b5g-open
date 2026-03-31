#! /bin/bash

#post OPTICAL intents from ASE to OSA
curl -u karaf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
   "appId": "org.onosproject.optical-rest",
   "uuid": "550e8400-e29b-41d4-a716-446655440027",
   "ingressPoint": {
     "device": "netconf:192.168.88.13:830",
     "port": "101"
   },
   "egressPoint": {
     "device": "netconf:192.168.88.34:830",
     "port": "101"
   },
   "bidirectional": true,
   "signal": {
     "channelSpacing": "CHL_6P25GHZ",
     "gridType": "FLEX",
     "spacingMultiplier": 260,
     "slotGranularity": 16
   }
}' 'http://localhost:8181/onos/optical/intents'




