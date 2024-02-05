#! /bin/bash

#post OPTICAL random link lenghts
curl -u karaf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' 'http://localhost:8181/onos/optical/links/annotate/allLinksLength?min=23&max=123'


