#! /bin/bash

curl -u karaf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' 'http://montebianco.polito.it:8181/onos/optical/intents/powerConfig/org.onosproject.optical-rest/0x0/0'

