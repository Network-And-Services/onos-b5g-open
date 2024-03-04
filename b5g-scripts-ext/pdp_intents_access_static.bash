#! /bin/bash

echo "Posting C-BAND intents Groove"
./pdp_intent_groove_194.8.bash
echo "--- 194.800 THz"
sleep 4

./pdp_intent_groove_194.6.bash
echo "--- 194.600 THz"
sleep 4

./pdp_intent_groove_194.4.bash
echo "--- 194.400 THz"
sleep 4

./pdp_intent_groove_194.2.bash
echo "--- 194.200 THz"
sleep 4

./pdp_intent_groove_194.0.bash
echo "--- 194.000 THz"
sleep 4

./pdp_intent_groove_193.8.bash
echo "--- 193.800 THz"
sleep 4

echo "Posting O-BAND intents"
./pdp_intent_oband_230.6_width.bash
echo "--- 230.600 THz"
sleep 4

./pdp_intent_oband_229.8_width.bash
echo "--- 229.800 THz"
sleep 4




