#! /bin/bash

#post OPTICAL intents from ASE to OSA
curl --noproxy '*' -u karaf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
    "uuid": "550e8400-e29b-41d4-a716-446655440000",
    "appId": "org.onosproject.optical-rest",
    "ingressPoint": {
      "device": "netconf:10.100.101.11:2022",
      "port": "22"
    },
    "egressPoint": {
      "device": "netconf:10.100.101.14:2022",
      "port": "21"
    },
    "bidirectional": false,
    "signal": {
      "channelSpacing": "CHL_6P25GHZ",
      "gridType": "FLEX",
      "spacingMultiplier": 336,
      "slotGranularity": 8
    },
    "suggestedPath": {
     "links": [
	{
         "src": "netconf:10.100.101.11:2022/31",
         "dst": "netconf:10.100.101.12:2022/32"
       },
       {
         "src": "netconf:10.100.101.12:2022/21",
         "dst": "netconf:10.100.101.13:2022/22"
       },
              {
         "src": "netconf:10.100.101.13:2022/31",
         "dst": "netconf:10.100.101.14:2022/32"
       }
     ]
   }
  }
}' 'http://localhost:8181/onos/optical/intents'

