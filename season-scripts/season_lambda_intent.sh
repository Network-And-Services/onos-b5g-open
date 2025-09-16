#! /bin/bash

curl -ukaraf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
   "appId": "org.onosproject.optical-rest",
   "uuid": "919da43b-5c7f-4460-a9e4-afec6b0f8189",
   "ingressPoint": {
     "device": "netconf:10.100.101.21:2022",
     "port": "11003"
   },
   "egressPoint": {
     "device": "netconf:10.100.101.23:2022",
     "port": "11003"
   },
   "bidirectional": false,
   "signal": {
     "channelSpacing": "CHL_6P25GHZ",
     "gridType": "FLEX",
     "spacingMultiplier": 800,
     "slotGranularity": 8
   }
 }' 'http://localhost:8181/onos/optical/intents'

