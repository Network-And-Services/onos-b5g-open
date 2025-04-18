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
	  "ingressPoint": {
	     "device": "netconf:10.100.101.21:2022",
	     "port": '"$i"'
	  },
	  "egressPoint": {
	     "device": "netconf:10.100.101.22:2022",
	     "port": '"$i"'
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
         	"src": "netconf:10.100.101.21:2022/'"$i"'",
         	"dst": "netconf:192.168.88.31:830/4101"
       		},
       		{
         	"src": "netconf:192.168.88.31:830/3001",
         	"dst": "netconf:192.168.88.31:830/3001"
       		},
		{
                "src": "netconf:192.168.88.31:830/5210",
		"dst": "netconf:192.168.88.32:830/3001"
                },
		{
                "src": "netconf:192.168.88.32:830/5210",
                "dst": "netconf:192.168.88.32:830/4110"
                },
		{
                "src": "netconf:192.168.88.32:830/3001",
                "dst": "netconf:192.168.88.33:830/4110"
                },
       		{
         	"src": "netconf:192.168.88.33:830/3001",
         	"dst": "netconf:192.168.88.33:830/3001"
       		},
       		{
         	"src": "netconf:192.168.88.33:830/5201",
         	"dst": "netconf:10.100.101.22:2022/'"$i"'"
		}
     	    ]
   	  }
	}' 'http://localhost:8181/onos/optical/intents'

    fi
    sleep 20
    done

