#!/bin/bash

URL="http://localhost:8181/onos/optical/intents/mp2pIntent"

curl -u karaf:karaf -X POST "$URL" --header "Content-Type: application/json" --header "Accept: application/json" -d @- <<EOF
{
  "uuid": "123a4567-e89b-12d3-a456-4266141740a0",
  "appId": "org.onosproject.optical-rest",
  "egressPoint": {
    "device": "netconf:172.18.0.10:2022",
    "port": "1010"
  },
  "ingressPoint": [
    {
    "device": "netconf:172.18.0.12:2022",
    "port": "1011"
    },
    {
    "device": "netconf:172.18.0.13:2022",
    "port": "1011"
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
