#! /bin/bash

#post OPTICAL intents from ASE to OSA
curl -u karaf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
   "appId": "org.onosproject.optical-rest",
   "ingressPoint": {
     "device": "netconf:10.100.101.11:2022",
     "port": "1011"
   },
   "egressPoint": {
     "device": "netconf:10.100.101.14:2022",
     "port": "1010"
   },
   "bidirectional": true
 }' 'http://localhost:8181/onos/optical/intents'

