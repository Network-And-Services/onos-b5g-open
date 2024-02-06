curl -u karaf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' 'http://localhost:8181/onos/optical/nodes/annotate/onePort?connectPoint=netconf%3A10.100.101.113%3A2022%2F1021&key=interdomain-connect-point&value=netconf%3A10.100.101.13%3A2022%2F1020'

curl -u karaf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' 'http://localhost:8181/onos/optical/nodes/annotate/onePort?connectPoint=netconf%3A10.100.101.113%3A2022%2F1020&key=interdomain-connect-point&value=netconf%3A10.100.101.13%3A2022%2F1021'

