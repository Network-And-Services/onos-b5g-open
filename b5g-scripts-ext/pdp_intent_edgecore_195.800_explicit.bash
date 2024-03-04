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
     "port": "5203"
   },
   "bidirectional": false,
   "signal": {
     "channelSpacing": "CHL_6P25GHZ",
     "gridType": "FLEX",
     "spacingMultiplier": 432,
     "slotGranularity": 8
   },
   "suggestedPath": {
     "links": [
       {
         "src": "netconf:10.100.101.11:830/11",
         "dst": "netconf:10.100.101.111:2022/12"
       },
       {
         "src": "netconf:10.100.101.111:2022/21",
         "dst": "netconf:10.100.101.112:2022/22"
       },
       {
         "src": "netconf:10.100.101.112:2022/11",
         "dst": "netconf:192.168.1.53:830/12"
       },
       {
         "src": "netconf:192.168.1.53:830/31",
         "dst": "netconf:10.100.101.113:2022/12"
       },
       {
         "src": "netconf:10.100.101.113:2022/21",
         "dst": "netconf:10.100.101.114:2022/22"
       },
       {
         "src": "netconf:10.100.101.114:2022/11",
         "dst": "netconf:192.168.1.201:830/12"
       },
       {
         "src": "netconf:192.168.1.201:830/31",
         "dst": "netconf:10.100.101.115:2022/12"
       },
       {
         "src": "netconf:10.100.101.115:2022/21",
         "dst": "netconf:10.100.101.116:2022/22"
       },
       {
         "src": "netconf:10.100.101.116:2022/11",
         "dst": "netconf:192.168.1.101:830/3001"
       }
     ]
   }
}' 'http://localhost:8181/onos/optical/intents'

