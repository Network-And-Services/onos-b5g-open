#! /bin/bash

#post OPTICAL intents from ASE to OSA
curl --noproxy '*' -u karaf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
    "uuid": "550e8400-e29b-41d4-a716-446655440000",
    "appId": "org.onosproject.optical-rest",
    "ingressPoint": {
      "device": "netconf:10.100.101.11:2022",
      "port": "1021"
    },
    "egressPoint": {
      "device": "netconf:10.100.101.14:2022",
      "port": "1020"
    },
    "bidirectional": false,
    "signal": {
      "channelSpacing": "CHL_6P25GHZ",
      "gridType": "FLEX",
      "spacingMultiplier": 208,
      "slotGranularity": 8
    }
  }
}' 'http://localhost:8181/onos/optical/intents'

