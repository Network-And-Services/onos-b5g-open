#! /bin/bash

echo "Posting GROOVE intents C-Band"
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

echo "Posting Edgecore intents C-Band"
./pdp_intent_edgecore_195.200.bash
echo "--- 195.200 THz"
sleep 4

./pdp_intent_edgecore_195.400.bash
echo "--- 195.400 THz"
sleep 4

./pdp_intent_edgecore_195.600_access.bash
echo "--- 195.600 THz - inter-domain"
sleep 4

./pdp_intent_edgecore_195.800.bash
echo "--- 195.800 THz"
sleep 4
