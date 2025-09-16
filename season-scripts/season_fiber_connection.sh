#! /bin/bash

curl -u karaf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
   "flows": [
     {
       "priority": 40000,
       "timeout": 0,
       "isPermanent": true,
       "deviceId": "netconf:10.100.101.15:830",
       "selector": {
         "criteria": [
           {
             "type": "IN_PORT",
             "port": "10"
           }
         ]
       },
       "treatment": {
         "instructions": [ 
           {
             "type": "OUTPUT",
             "port": "21"
           }
         ]
       }
     }
   ]
 }' 'http://localhost:8181/onos/v1/flows?appId=org.onosproject.optical-rest'
