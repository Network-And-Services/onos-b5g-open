#! /bin/bash

#post OPTICAL intents from ASE to OSA
curl --noproxy '*' -u karaf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
   "appId": "org.onosproject.optical-rest",
   "ingressPoint": {
     "device": "netconf:192.168.1.53:830",
     "port": "1011"
   },
   "egressPoint": {
     "device": "netconf:192.168.1.101:830",
     "port": "5201"
   },
   "bidirectional": false,
   "signal": {
     "channelSpacing": "CHL_6P25GHZ",
     "gridType": "FLEX",
     "spacingMultiplier": 336,
     "slotGranularity": 8
   }
}' 'http://localhost:8181/onos/optical/intents'

