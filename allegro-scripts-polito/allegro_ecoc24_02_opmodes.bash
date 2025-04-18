#! /bin/bash

echo "...opmode 2108"
./allegro_op_mode_2108_DP_16QAM.bash
sleep 2

echo "...opmode 2204"
./allegro_op_mode_2204_DP_QPSK.bash
sleep 2

echo "...opmode 2106"
./allegro_op_mode_2106_DP_8QAM.bash
sleep 10

