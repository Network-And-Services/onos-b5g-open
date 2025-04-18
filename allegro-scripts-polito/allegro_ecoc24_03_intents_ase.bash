#! /bin/bash

echo "...intent 1"
./allegro_intent_ase_1.bash
sleep 10

echo "...intent 2"
./allegro_intent_ase_2.bash
sleep 10

echo "...intent 3"
./allegro_intent_ase_3.bash
sleep 10

echo "...intent 1 back"
./allegro_intent_ase_1_r.bash
sleep 10

echo "...intent 2 back"
./allegro_intent_ase_2_r.bash
sleep 10

echo "...intent 3 back"
./allegro_intent_ase_3_r.bash
sleep 10

