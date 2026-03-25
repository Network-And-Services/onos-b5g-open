#!/bin/bash

if [ "$#" -ne 2 ]; then
  echo "Use: $0 INTENT_KEY_START INTENT_KEY_STEP"
  exit 1
fi

# Assegna i tre numeri a variabili
KEY_START=$1
KEY_STEP=$2

INDEX=0
KEY="$KEY_START"

CHANNEL_START=-244
CHANNEL_STOP=412
CHANNEL_WIDTH=12    #75GHz is 6 slots of 12.5GHz --> 12 slots of 6.25GHz
CHANNEL_SPACING=16

FREQUENCY_START=$( echo "193.100 + $CHANNEL_START * 0.00625" | bc)
FREQUENCY_STOP=$( echo "193.100 + $CHANNEL_STOP * 0.00625" | bc)
FREQUENCY_WIDTH=$( echo "$CHANNEL_WIDTH * 6.25" | bc)
FREQUENCY_SPACING=$( echo "$CHANNEL_SPACING * 6.25" | bc)

TOTAL_CHANNELS=$(echo "1 + ($CHANNEL_STOP - $CHANNEL_START) / $CHANNEL_SPACING" | bc -l)

NO_CHANNEL_1=999
NO_CHANNEL_2=999
NO_CHANNEL_3=999
NO_CHANNEL_4=999

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
    if [[ "$CHANNEL" -ne "$NO_CHANNEL_1" && "$CHANNEL" -ne "$NO_CHANNEL_2" && "$CHANNEL" -ne "$NO_CHANNEL_3" && "$CHANNEL" -ne "$NO_CHANNEL_4" ]]
    then
	curl -u karaf:karaf -X DELETE --header 'Accept: application/json' "http://localhost:8181/onos/optical/intents/org.onosproject.optical-rest/$KEY"
        echo - Intent "$INDEX" with key "$KEY" channel "$CHANNEL" on central frequency "$FREQUENCY" THz
    else
        echo - Intent "$INDEX" with channel "$CHANNEL" on central frequency "$FREQUENCY" THz --- SKIPPED
    fi

    KEY=$((KEY + KEY_STEP))
    sleep 5
done

