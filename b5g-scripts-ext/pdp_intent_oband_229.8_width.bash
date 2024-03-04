#! /bin/bash

#post OPTICAL intents from ASE to OSA
curl --noproxy '*' -u karaf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
   "appId": "org.onosproject.optical-rest",
   "ingressPoint": {
     "device": "netconf:10.100.101.110:2022",
     "port": "1011"
   },
   "egressPoint": {
     "device": "netconf:192.168.1.35:830",
     "port": "1010"
   },
   "bidirectional": false,
   "signal": {
     "channelSpacing": "CHL_6P25GHZ",
     "gridType": "FLEX",
     "spacingMultiplier": 5872,
     "slotGranularity": 48
   }
}' 'http://localhost:8181/onos/optical/intents'

