#!/usr/bin/env bash

if [ $# -lt 1 ]; then
    echo "Script needs the index of the OchSignal"
    exit 1
fi

SPACING_MULTIPLIER="$1"

curl -u karaf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d "{
    \"priority\": 100,
    \"timeout\": 0,
    \"isPermanent\": true,
    \"deviceId\": \"netconf:172.17.0.2:2022\",
    \"selector\": {
      \"criteria\": [
        {
          \"type\": \"IN_PORT\",
          \"port\": 10
        },
        {
          \"type\": \"OCH_SIGID\",
          \"ochSignalId\": {
            \"gridType\": \"DWDM\",
            \"channelSpacing\": \"CHL_50GHZ\",
            \"spacingMultiplier\": ${SPACING_MULTIPLIER},
            \"slotGranularity\": 4
          }
        },
        {
          \"type\": \"OCH_SIGTYPE\",
          \"ochSignalType\": \"FIXED_GRID\"
        }
      ]
    },
    \"treatment\": {
      \"instructions\": [
        {
          \"type\": \"L0MODIFICATION\",
          \"subtype\": \"OCH\",
          \"gridType\": \"DWDM\",
          \"channelSpacing\": \"CHL_50GHZ\",
          \"spacingMultiplier\": ${SPACING_MULTIPLIER},
          \"slotGranularity\": 4
        },
        {
          \"type\": \"OUTPUT\",
          \"port\": \"21\"
        }
      ]
    }
  }" 'http://localhost:8181/onos/v1/flows/netconf%3A172.17.0.2%3A2022?appId=org.onosproject.roadm'
