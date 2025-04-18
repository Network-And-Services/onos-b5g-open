#!/bin/bash

for ((i=-278; i<=360; i=i+12)); do
    CHANNEL="$i"

    #post OPTICAL intents from ASE to OSA
    if [[ "$CHANNEL" -ne -230 ]]
    then
        echo Intent on ports "$i" on channel "$CHANNEL"

	curl -u karaf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
	  "appId": "org.onosproject.optical-rest",
	  "uuid": "550e8400-e29b-41d4-a716-446655440000",
          "ingressPoint": {
     	    "device": "netconf:172.18.0.33:830",
            "port": "30"
          },
          "egressPoint": {
            "device": "netconf:172.18.0.32:830",
            "port": "21"
          },
	  "bidirectional": false,
          "signal": {
     	    "channelSpacing": "CHL_6P25GHZ",
     	    "gridType": "FLEX",
     	    "spacingMultiplier": '"$i"',
     	    "slotGranularity": 6
          },
	  "suggestedPath": {
     	    "links": [
              {
              "src": "netconf:172.18.0.33:830/11",
              "dst": "netconf:172.18.0.31:830/10"
              },
	      {
              "src": "netconf:172.18.0.31:830/21",
              "dst": "netconf:172.18.0.32:830/10"
              }
	    ]
          }
	}' 'http://localhost:8181/onos/optical/intents'

    fi
    sleep 20
    done

