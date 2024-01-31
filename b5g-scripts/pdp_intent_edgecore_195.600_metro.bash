#! /bin/bash

#post OPTICAL intents from ASE to OSA
curl --noproxy '*' -u karaf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
   "appId": "org.onosproject.optical-rest",
   "ingressPoint": {
     "device": "netconf:163.162.95.102:830",
     "port": "4120"
   },
   "egressPoint": {
     "device": "netconf:163.162.95.164:830",
     "port": "5201"
   },
   "bidirectional": false,
   "signal": {
     "channelSpacing": "CHL_6P25GHZ",
     "gridType": "FLEX",
     "spacingMultiplier": 400,
     "slotGranularity": 8
   }
}' 'http://localhost:8181/onos/optical/intents'

