#! /bin/bash

curl -u karaf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
    "mode-id": 2204,
    "mode-type": "TRANSCEIVER_MODE_TYPE_EXPLICIT",
    "operational-mode-capabilities": {
      "optical-channel-spectrum-width": "37.5",
      "min-rx-osnr": "10.0",
      "bit-rate": "10000000000.0",
      "max-differential-group-delay": "unknown",
      "modulation-format": "MODULATION_FORMAT_DP_QPSK",
      "baud-rate": "unknown",
      "max-chromatic-dispersion": "unknown",
      "min-tx-osnr": "unknown",
      "max-input-power": "3.0",
      "min-input-power": "-30.0",
      "max-polarization-dependent-loss": "unknown"
    },
    "fec": {
      "pre-fec-ber-threshold": "unknown",
      "coding-gain": "unknown",
      "fec-coding": "everest",
      "coding-overhead": "15.0"
    },
    "penalties": [
      {
        "parameter-and-unit": "unknown",
        "up-to-boundary": "unknown",
        "penalty-value": "unknown"
      },
      {
        "parameter-and-unit": "unknown",
        "up-to-boundary": "unknown",
        "penalty-value": "unknown"
      },
      {
        "parameter-and-unit": "unknown",
        "up-to-boundary": "unknown",
        "penalty-value": "unknown"
      }
    ],
    "filter": {
      "pulse-shaping-type": "OFF"
    },
    "optical-channel-config-value-constraints": {
      "max-central-frequency": "196100000",
      "grid-type": "FLEX",
      "max-output-power": "1",
      "adjustment-granularity": "G_6.25GHZ",
      "min-central-frequency": "191150000",
      "min-channel-spacing": "6.25",
      "min-output-power": "-10.0"
    }
  }
}' 'http://montebianco.polito.it:8181/onos/optical/opmodes/opMode'
