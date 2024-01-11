curl -u karaf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
   "appId": "org.onosproject.optical-rest",
   "ingressPoint": {
     "device": "netconf:10.10.10.30:830",
     "port": "3001"
   },
   "egressPoint": {
     "device": "netconf:10.100.101.11:2022",
     "port": "1010"
   },
   "bidirectional": false,
   "signal": {
     "channelSpacing": "CHL_100GHZ",
     "gridType": "DWDM",
     "spacingMultiplier": -12,
     "slotGranularity": 8
  }
  }
 }' 'http://193.205.83.89:8181/onos/optical/intents'

