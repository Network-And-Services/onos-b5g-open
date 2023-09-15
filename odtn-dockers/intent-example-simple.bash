#! /bin/bash

#post OPTICAL intents from ASE to OSA
curl -u karaf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
   "appId": "org.onosproject.optical-rest",
   "ingressPoint": {
     "device": "netconf:192.168.88.12:830",
     "port": "103"
   },
   "egressPoint": {
     "device": "netconf:192.168.88.12:830",
     "port": "103"
   },
   "bidirectional": false
 }' 'http://localhost:8181/onos/optical/intents'



