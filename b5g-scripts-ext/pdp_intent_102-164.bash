#! /bin/bash

#post OPTICAL intents from ASE to OSA
curl --noproxy '*' -u karaf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
   "appId": "org.onosproject.optical-rest",
   "ingressPoint": {
     "device": "netconf:192.168.1.102:830",
     "port": "4120"
   },
   "egressPoint": {
     "device": "netconf:192.168.1.164:830",
     "port": "5201"
   },
   "bidirectional": false
   }
 }' 'http://localhost:8181/onos/optical/intents'

