#!/bin/bash

INDEX=0

CHANNEL_START=-244
CHANNEL_STOP=412
CHANNEL_WIDTH=12   #Expressed in 6.25GHz slots - 75GHz is 6 slots of 12.5GHz --> 12 slots of 6.25GHz
CHANNEL_SPACING=16 #Expressed in 6.25GHz slots - 75GHz is 6 slots of 12.5GHz --> 12 slots of 6.25GHz

FREQUENCY_START=$( echo "193.100 + $CHANNEL_START * 0.00625" | bc)
FREQUENCY_STOP=$( echo "193.100 + $CHANNEL_STOP * 0.00625" | bc)
FREQUENCY_WIDTH=$( echo "$CHANNEL_WIDTH * 6.25" | bc)
FREQUENCY_SPACING=$( echo "$CHANNEL_SPACING * 6.25" | bc)

TOTAL_CHANNELS=$(echo "1 + ($CHANNEL_STOP - $CHANNEL_START) / $CHANNEL_SPACING" | bc -l)

NO_CHANNEL_1=-116
NO_CHANNEL_2=-100
NO_CHANNEL_3=252
NO_CHANNEL_4=268

echo -----------------------------------------------------
echo --- ALL intents are with width "$FREQUENCY_WIDTH" GHz
echo --- ALL intents are with spacing "$FREQUENCY_SPACING" GHz
echo --- FIRST intent on central frequency "$FREQUENCY_START" THz with index "$CHANNEL_START"
echo --- LAST intent on central frequency "$FREQUENCY_STOP" THz with index "$CHANNEL_STOP"
echo --- TOTAL number of channels "$TOTAL_CHANNELS"
echo --- EXCLUDED channels "$NO_CHANNEL_1", "$NO_CHANNEL_2", "$NO_CHANNEL_3", "$NO_CHANNEL_4"
echo -----------------------------------------------------

for ((i="$CHANNEL_START"; i<="$CHANNEL_STOP"; i=i+"$CHANNEL_SPACING")); do
    INDEX=$(($INDEX+1))
    CHANNEL="$i"
    FREQUENCY=$( echo "193.100 + $CHANNEL * 0.00625" | bc)

    #post OPTICAL intents from Degree 3 of Node A ---> to Degree 1 of Node B
    if [[ "$CHANNEL" -ne "$NO_CHANNEL_1" && "$CHANNEL" -ne "$NO_CHANNEL_2"&& "$CHANNEL" -ne "$NO_CHANNEL_3" && "$CHANNEL" -ne "$NO_CHANNEL_4" ]]
    then
        echo Intent "$INDEX" with channel "$CHANNEL" on central frequency "$FREQUENCY" THz

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
            "slotGranularity": '"$((CHANNEL_WIDTH / 2))"'
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

	sleep 10
    else
        echo Intent "$INDEX" with channel "$CHANNEL" on central frequency "$FREQUENCY" THz --- SKIPPED
        sleep 1
    fi

    sleep 5
    done

