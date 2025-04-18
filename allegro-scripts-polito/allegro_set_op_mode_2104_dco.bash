#! /bin/bash

curl -u karaf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
   "appId": "org.onosproject.optical-rest",
   "ingressPoint": {
     "device": "netconf:192.168.88.11:830",
     "port": "102"
   },
   "egressPoint": {
     "device": "netconf:192.168.88.12:830",
     "port": "103"
   },
   "opModeId": "2108"
 }' 'http://montebianco.polito.it:8181/onos/optical/opmodes/opModeConfigure'
