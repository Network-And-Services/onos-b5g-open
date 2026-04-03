#!/bin/bash

URL="http://localhost:8181/onos/optical/intents/p2mpIntent"

curl -u karaf:karaf -X POST "$URL" --header "Content-Type: application/json" --header "Accept: application/json" -d @- <<EOF
{
  "uuid": "123e4567-e89b-12d3-a456-4266141740a0",
  "appId": "org.onosproject.optical-rest",
  "ingressPoint": {
    "device": "netconf:172.18.0.10:2022",
    "port": "1011"
  },
  "egressPoint": [
    {
    "device": "netconf:172.18.0.12:2022",
    "port": "1010"
    },
    {
    "device": "netconf:172.18.0.13:2022",
    "port": "1010"
    }
  ],
  "signal": {
     "channelSpacing": "CHL_6P25GHZ",
     "gridType": "FLEX",
     "spacingMultiplier": 20,
     "slotGranularity": 16
   }
}
EOF
