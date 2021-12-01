/*
 * Copyright 2022-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This work was partially supported by EC H2020 project B5G-OPEN (101016663).
 */

package org.onosproject.drivers.odtn;

import com.google.common.collect.ImmutableList;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.onlab.packet.ChassisId;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.Device;
import org.onosproject.net.Port;
import org.onosproject.net.DeviceId;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.PortNumber;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.optical.device.OchPortHelper;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
Tested on Cassini running OcNOS version EC_AS7716-24SC-OcNOS-5.0.187-OTN_IPBASE-S0-P0.

- Driver consider proprietary YANG model by IP Infusion
- The device also support an OpenConfig-based model that can be activated using NETCONF translation
- Cassini CLI command to activate NETCONF translation
 --- OcNOS# cml netconf translation (disable|openconfig)
 */
/**
 * Implementation of DeviceDescriptionDiscovery interface for Cassini device running Ocnos v5.
 */
public class CassiniOcnos5DeviceDiscovery
    extends AbstractHandlerBehaviour
    implements OdtnDeviceDescriptionDiscovery, DeviceDescriptionDiscovery {

        private static final String RPC_TAG_NETCONF_BASE =
                "<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">";

        private static final String RPC_CLOSE_TAG = "</rpc>";

        private static final String OC_PLATFORM_TYPES_TRANSCEIVER =
                "oc-platform-types:TRANSCEIVER";

        private static final String OC_PLATFORM_TYPES_PORT =
                "oc-platform-types:PORT";

        private static final String OC_TRANSPORT_TYPES_OPTICAL_CHANNEL =
                "oc-opt-types:OPTICAL_CHANNEL";

        private static final Logger log = getLogger(CassiniOcnos5DeviceDiscovery.class);

        /**
         * Returns the NetconfSession with the device for which the method was called.
         *
         * @param deviceId device indetifier
         *
         * @return The netconf session or null
         */
        private NetconfSession getNetconfSession(DeviceId deviceId) {
            NetconfController controller = handler().get(NetconfController.class);
            NetconfDevice ncdev = controller.getDevicesMap().get(deviceId);
            if (ncdev == null) {
                log.trace("No netconf device, returning null session");
                return null;
            }
            return ncdev.getSession();
        }


        /**
         * Get the deviceId for which the methods apply.
         *
         * @return The deviceId as contained in the handler data
         */
        private DeviceId did() {
            return handler().data().deviceId();
        }


        /**
         * Get the device instance for which the methods apply.
         *
         * @return The device instance
         */
        private Device getDevice() {
            DeviceService deviceService = checkNotNull(handler().get(DeviceService.class));
            Device device = deviceService.getDevice(did());
            return device;
        }


        /**
         * Construct a String with a Netconf filtered get RPC Message.
         *
         * @param filter A valid XML tree with the filter to apply in the get
         * @return a String containing the RPC XML Document
         */
        private String filteredGetBuilder(String filter) {
            StringBuilder rpc = new StringBuilder(RPC_TAG_NETCONF_BASE);
            rpc.append("<get>");
            rpc.append("<filter type='subtree'>");
            rpc.append(filter);
            rpc.append("</filter>");
            rpc.append("</get>");
            rpc.append(RPC_CLOSE_TAG);
            return rpc.toString();
        }


        /**
         * Construct a String with a Netconf filtered get RPC Message.
         *
         * @param filter A valid XPath Expression with the filter to apply in the get
         * @return a String containing the RPC XML Document
         *
         * Note: server must support xpath capability.

         * <select=" /components/component[name='PORT-A-In-1']/properties/...
         * ...property[name='onos-index']/config/value" type="xpath"/>
         */
        private String xpathFilteredGetBuilder(String filter) {
            StringBuilder rpc = new StringBuilder(RPC_TAG_NETCONF_BASE);
            rpc.append("<get>");
            rpc.append("<filter type='xpath' select=\"");
            rpc.append(filter);
            rpc.append("\"/>");
            rpc.append("</get>");
            rpc.append(RPC_CLOSE_TAG);
            return rpc.toString();
        }


        /**
         * Builds a request to get Device details, operational data.
         *
         * @return A string with the Netconf RPC for a get with subtree rpcing based on
         *    /components/component/state/type being oc-platform-types:OPERATING_SYSTEM
         */
        private String getDeviceDetailsBuilder() {
            StringBuilder filter = new StringBuilder();
            filter.append("<components xmlns='http://openconfig.net/yang/platform'>");
            filter.append(" <component>");
            filter.append("  <state>");
            filter.append("   <type xmlns:oc-platform-types='http://openconfig.net/");
            filter.append("yang/platform-types'>oc-platform-types:OPERATING_SYSTEM</type>");
            filter.append("  </state>");
            filter.append(" </component>");
            filter.append("</components>");
            return filteredGetBuilder(filter.toString());
        }

        private String getCassiniDeviceDetailsBuilder() {
            StringBuilder filter = new StringBuilder();
            filter.append("<components xmlns='http://www.ipinfusion.com/yang/ocnos/ipi-platform'>");
            filter.append(" <component>");
            filter.append("   <name>CHASSIS</name>");
            filter.append(" </component>");
            filter.append("</components>");
            return filteredGetBuilder(filter.toString());
        }

        private String getCassiniDeviceComponentsBuilder() {
                StringBuilder filter = new StringBuilder();
                filter.append("<components xmlns='http://www.ipinfusion.com/yang/ocnos/ipi-platform'>");
                filter.append("</components>");
                return filteredGetBuilder(filter.toString());
        }

        /**
         * Returns a DeviceDescription with Device info.
         *
         * @return DeviceDescription or null
         *
         * //CHECKSTYLE:OFF
         * <pre>{@code
         * <data>
         * <components xmlns="http://openconfig.net/yang/platform">
         *  <component>
         *   <state>
         *     <name>FIRMWARE</name>
         *     <type>oc-platform-types:OPERATING_SYSTEM</type>
         *     <description>CTTC METRO-HAUL Emulated OpenConfig TerminalDevice</description>
         *     <version>0.0.1</version>
         *   </state>
         *  </component>
         * </components>
         * </data>
         *}</pre>
         * //CHECKSTYLE:ON
         */
        @Override
        public DeviceDescription discoverDeviceDetails() {
            log.info("CassiniOcnos5DeviceDiscovery::discoverDeviceDetails device {}", did());
            boolean defaultAvailable = true;
            SparseAnnotations annotations = DefaultAnnotations.builder().build();

            org.onosproject.net.Device.Type type = Device.Type.TERMINAL_DEVICE;

            // Some defaults
            String vendor       = "Not loaded";
            String hwVersion    = "Not loaded";
            String swVersion    = "Not loaded";
            String serialNumber = "Not loaded";
            String chassisId    = "12";

            // Get the session,
            NetconfSession session = getNetconfSession(did());
            if (session != null) {
                try {
                    String reply = session.get(getCassiniDeviceDetailsBuilder());
                    XMLConfiguration xconf = (XMLConfiguration) XmlConfigParser.loadXmlString(reply);

                    log.debug("REPLY from device: {}", xconf);

                    vendor       = xconf.getString("data.components.component.chassis.state.vendor-name", vendor);
                    serialNumber = xconf.getString("data.components.component.state.serial-no", serialNumber);
                    // Requires OpenConfig >= 2018
                    swVersion    = xconf.getString("data.components.component.state.software-version", swVersion);
                    hwVersion    = xconf.getString("data.components.component.state.mfg-date", hwVersion);
                } catch (Exception e) {
                    throw new IllegalStateException(new NetconfException("Failed to retrieve version info.", e));
                }
            } else {
                log.info("CassiniOcnos5DeviceDiscovery::discoverDeviceDetails - No netconf session for {}", did());
            }

            log.info("VENDOR    {}", vendor);
            log.info("HWVERSION {}", hwVersion);
            log.info("SWVERSION {}", swVersion);
            log.info("SERIAL    {}", serialNumber);
            log.info("CHASSISID {}", chassisId);

            ChassisId cid = new ChassisId(Long.valueOf(chassisId, 10));

            return new DefaultDeviceDescription(did().uri(),
                    type, vendor, hwVersion, swVersion, serialNumber,
                    cid, defaultAvailable, annotations);
        }

        /**
         * Returns a list of PortDescriptions for the device.
         *
         * @return a list of descriptions.
         *
         * The RPC reply follows the following pattern:
         * //CHECKSTYLE:OFF
         * <pre>{@code
         * <?xml version="1.0" encoding="UTF-8"?>
         * <rpc-reply xmlns="urn:ietf:params:xml:ns:netconf:base:1.0" message-id="7">
         * <data>
         *   <components xmlns="http://openconfig.net/yang/platform">
         *     <component>....
         *     </component>
         *     <component>....
         *     </component>
         *   </components>
         * </data>
         * </rpc-reply>
         * }</pre>
         * //CHECKSTYLE:ON
         */
        @Override
        public List<PortDescription> discoverPortDetails() {
            try {
                XPathExpressionEngine xpe = new XPathExpressionEngine();
                NetconfSession session = getNetconfSession(did());
                if (session == null) {
                    log.error("discoverPortDetails called with null session for {}", did());
                    return ImmutableList.of();
                }

                String reply = session.get(getCassiniDeviceComponentsBuilder());

                XMLConfiguration xconf = (XMLConfiguration) XmlConfigParser.loadXmlString(reply);
                xconf.setExpressionEngine(xpe);

                HierarchicalConfiguration components = xconf.configurationAt("data/components");
                return parsePorts(components);
            } catch (Exception e) {
                log.error("Exception discoverPortDetails() {}", did(), e);
                return ImmutableList.of();
            }
        }

        /**
         * Parses port information from OpenConfig XML configuration.
         *
         * @param components the XML document with components root.
         * @return List of ports
         *
         * //CHECKSTYLE:OFF
         * <pre>{@code
         *   <components xmlns="http://openconfig.net/yang/platform">
         *     <component>....
         *     </component>
         *     <component>....
         *     </component>
         *   </components>
         * }</pre>
         * //CHECKSTYLE:ON
         */
        protected List<PortDescription> parsePorts(HierarchicalConfiguration components) {

            return components.configurationsAt("component")
                    .stream()
                    .map(component -> {
                                try {
                                    // Pass the root document for cross-reference
                                    return parsePortComponent(component);
                                } catch (Exception e) {
                                    return null;
                                }
                            }
                    )
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        /**
         * Parses a component XML doc into a PortDescription.
         *
         * @param component subtree to parse. It must be a component ot type PORT.
         *  case we need to check transceivers or optical channels.
         *
         * @return PortDescription or null if component does not have onos-index
         */
        private PortDescription parsePortComponent(HierarchicalConfiguration component) {
            Map<String, String> annotations = new HashMap<>();
            String name = component.getString("name");

            log.info("Parsing Component {}", name);

            // Build the port using heuristic
            // NOTE: using portNumber(id, name) breaks things. Intent parsing, port resorce management, etc. There seems
            // to be an issue with resource mapping

            if (name.contains("QSFP")) {
                annotations.put(PORT_TYPE, OdtnDeviceDescriptionDiscovery.OdtnPortType.CLIENT.value());

                // Assing an ONOS port number
                PortNumber portNum;
                portNum = PortNumber.portNumber(fromPortNameToPortId(name));
                log.info("--- CLIENT PORT {} assigned number {}", name, portNum);

                DefaultPortDescription.Builder builder = DefaultPortDescription.builder();
                builder.type(Port.Type.PACKET);
                builder.withPortNumber(portNum);
                builder.annotations(DefaultAnnotations.builder().putAll(annotations).build());

                return builder.build();
            }

            //Could be improved checking if there is an OCH subcomponent or current <oper-status>
            if (name.contains("PORT-coherent")) {
                PortNumber portNum;
                portNum = PortNumber.portNumber(fromPortNameToPortId(name));

                annotations.put(PORT_TYPE, OdtnDeviceDescriptionDiscovery.OdtnPortType.LINE.value());

                String state = component.configurationAt("state").getString("oper-status");

                if (!state.contains("disabled")) {
                    String slotName = String.valueOf(portNum.toLong() - 100);

                    //Config annotations
                    annotations.put(PORT_TYPE, OdtnPortType.LINE.value());
                    annotations.put(OC_NAME, name);
                    annotations.put(OC_OPTICAL_CHANNEL_NAME, slotName);

                    log.info("--- LINE port {} assigned onos index {}", name, portNum);
                    log.info("--- LINE port {} associated OPTICAL_CHANNEL {}", name, slotName);

                    // TODO: To be configured
                    OchSignal signalId = OchSignal.newDwdmSlot(ChannelSpacing.CHL_50GHZ, 1);

                    return OchPortHelper.ochPortDescription(
                            portNum, true,
                            OduSignalType.ODU4, // TODO Client signal to be discovered
                            true,
                            signalId,
                            DefaultAnnotations.builder().putAll(annotations).build());
                } else {
                    log.info("--- LINE port {} is disabled", name);
                }
            }

            log.warn("Unknown port type");
            return null;
        }

    //Client ports are reported in the model as QSFP-1, QSFP-2 and imported as 1, 2, ...
    //Line ports are reported in the model as PORT-Coherent-1, PORT-Coherent-2 and imported as 101, 102
        public int fromPortNameToPortId(String name) {
            String[] portions;
            if (name.contains("QSFP")) {
                portions = name.split("-");
                return Integer.parseInt(portions[1]);
            }
            if (name.contains("PORT")) {
                portions = name.split("-");
                return (100 + Integer.parseInt(portions[2]));
            }
            log.error("Port name not supported");
            return 0;
        }
}
