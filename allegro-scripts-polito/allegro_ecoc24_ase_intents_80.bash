#!/bin/bash

OFFSET=2028

for ((i=2001; i<=2075; i=i+1)); do
    CHANNEL="$((i-OFFSET))"

    #post OPTICAL intents from ASE to OSA
    if [[ "$CHANNEL" -ne -22 && "$CHANNEL" -ne -2 && "$CHANNEL" -ne 18 && "$CHANNEL" -ne 38 ]]
    then
        echo Intent on ports "$i" on channel "$CHANNEL"

	curl -u karaf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
	  "appId": "org.onosproject.optical-rest",
	  "uuid": "550e8400-e29b-41d4-a716-446655440000",
          "ingressPoint": {
     	    "device": "netconf:172.18.0.31:830",
            "port": "30"
          },
          "egressPoint": {
            "device": "netconf:172.18.0.33:830",
            "port": "21"
          },
	  "bidirectional": false,
	  "signal": {
     	     "channelSpacing": "CHL_50GHZ",
     	     "gridType": "DWDM",
     	     "spacingMultiplier": '"$((i-OFFSET))"',
     	     "slotGranularity": 4
   	  },
	  "suggestedPath": {
     	    "links": [
              {
              "src": "netconf:172.18.0.31:830/11",
              "dst": "netconf:172.18.0.33:830/10"
              }
	    ]
          }
	}' 'http://localhost:8181/onos/optical/intents'

    fi
    sleep 5
    done

