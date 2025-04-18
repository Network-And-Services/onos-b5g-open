#! /bin/bash

#post OPTICAL intents from ASE to OSA
curl -u karaf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
   "appId": "org.onosproject.optical-rest",
   "ingressPoint": {
     "device": "netconf:10.100.101.21:2022",
     "port": "2002"
   },
   "egressPoint": {
     "device": "netconf:10.100.101.22:2022",
     "port": "2002"
   },
   "bidirectional": false
 }' 'http://localhost:8181/onos/optical/intents'




