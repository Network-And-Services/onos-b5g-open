curl -u karaf:karaf -X POST \
  -H "Content-Type: application/json" \
  http://localhost:8181/onos/v1/configuration/org.onosproject.net.flow.impl.FlowRuleManager \
  -d '{
        "allowExtraneousRules": true
      }'
