#! /bin/bash

#post OPTICAL intents from ASE to OSA
curl --noproxy '*' -u karaf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
   "appId": "org.onosproject.optical-rest",
   "ingressPoint": {
     "device": "netconf:10.100.101.11:830",
     "port": "1010"
   },
   "egressPoint": {
     "device": "netconf:163.162.95.101:830",
     "port": "5220"
   },
   "bidirectional": false
   }
 }' 'http://localhost:8181/onos/optical/intents'

