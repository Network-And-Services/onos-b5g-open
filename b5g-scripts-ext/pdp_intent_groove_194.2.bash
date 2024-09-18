#! /bin/bash

#post OPTICAL intents from ASE to OSA
curl --noproxy '*' -u karaf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
   "appId": "org.onosproject.optical-rest",
   "ingressPoint": {
     "device": "netconf:10.100.101.11:830",
     "port": "1041"
   },
   "egressPoint": {
     "device": "netconf:192.168.1.101:830",
     "port": "5214"
   },
   "bidirectional": false,
   "signal": {
     "channelSpacing": "CHL_50GHZ",
     "gridType": "DWDM",
     "spacingMultiplier": 22,
     "slotGranularity": 4
   }
}' 'http://localhost:8181/onos/optical/intents'
