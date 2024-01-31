#! /bin/bash

#post OPTICAL intents from ASE to OSA
curl --noproxy '*' -u karaf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
   "appId": "org.onosproject.optical-rest",
   "ingressPoint": {
     "device": "netconf:163.162.95.53:830",
     "port": "1041"
   },
   "egressPoint": {
     "device": "netconf:163.162.95.101:830",
     "port": "5203"
   },
   "bidirectional": false,
   "signal": {
     "channelSpacing": "CHL_6P25GHZ",
     "gridType": "FLEX",
     "spacingMultiplier": 432,
     "slotGranularity": 8
   }
}' 'http://localhost:8181/onos/optical/intents'

