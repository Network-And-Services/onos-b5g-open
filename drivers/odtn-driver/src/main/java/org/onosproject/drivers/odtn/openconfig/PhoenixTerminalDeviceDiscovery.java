/*
 * Copyright 2018-present Open Networking Foundation
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
 * This work was partially supported by EC H2020 project METRO-HAUL (761727).
 */

package org.onosproject.drivers.odtn.openconfig;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.OchSignal;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.PortNumber;
import org.onosproject.net.CltSignalType;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.optical.device.OduCltPortHelper;
import org.onosproject.net.OcOperationalMode;
import org.onosproject.net.optical.ocopmode.OcOperationalModesManager;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;

import org.onlab.packet.ChassisId;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;

import org.onosproject.drivers.utilities.XmlConfigParser;

import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.PortDescription;

import org.onosproject.net.driver.AbstractHandlerBehaviour;

import org.onosproject.net.optical.device.OchPortHelper;

import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;

import com.google.common.collect.ImmutableList;

import org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery;


/**
 * Driver Implementation of the DeviceDescription discovery for OpenConfig terminal devices.
 * Developed and tested for NEC OpenConfig implementation
 *
 * As defined in OpenConfig each PORT component includes a subcomponent:
 * --- client ports are of type: oc-opt-types:TERMINAL_CLIENT
 * --- line   ports are of type: oc-opt-types:TERMINAL_LINE
 *
 *
 * Other assumptions:
 * --- The port name is in the format "cfp2-xx" for line ports
 * --- The port name is in the format "qsfp-xx" for client ports
 * --- The subcomponent of type TRANSCEIVER has a name in the format "cfp2-transceiver-xx"
 * --- The subcomponent of type OPTICAL_CHANNEL has a name in the format "cfp2-opt-xx-1"
 *
 * Where xxx is an integer number (ranging from 1 to 99)
 *
 *  --- FINO A QUI
 *
 * --- In the section <terminal-device><logical-channels> the channel with index xxx is associated to transceiver-xxx
 *
 * See simplified example of a port component:
 *
 * //CHECKSTYLE:OFF
 * <component>
 *     <name>port-11801</name>
 *     <state>
 *         <name>port-11801</name>
 *         <type>oc-platform-types:PORT</type>
 *     </state>
 *     <properties>
 *         <property>
 *             <name>odtn-port-type</name>
 *             <state>
 *                 <name>odtn-port-type</name>
 *                 <value>client</value>
 *             </state>
 *         </property>
 *         <property>
 *             <name>onos-index</name>
 *             <state>
 *                 <name>onos-index</name>
 *                 <value>11801</value>
 *             </state>
 *             </property>
 *     </properties>
 *     <subcomponents>
 *         <subcomponent>
 *             <name>transceiver-11801</name>
 *             <state>
 *                 <name>transceiver-11801</name>
 *             </state>
 *         </subcomponent>
 *     </subcomponents>
 * </component>
 * <terminal-device>
 *     <logical-channels>
 *         <channel>
 *             <index>11801</index>
 *             <state>
 *                 <index>11801</index>
 *                 <description>Logical channel 11801</description>
 *                 <admin-state>DISABLED</admin-state>
 *                 <rate-class>oc-opt-types:TRIB_RATE_10G</rate-class>
 *                 <trib-protocol>oc-opt-types:PROT_10GE_LAN</trib-protocol>
 *                 <logical-channel-type>oc-opt-types:PROT_ETHERNET</logical-channel-type>
 *                 <loopback-mode>NONE</loopback-mode>
 *                 <test-signal>false</test-signal>
 *                 <link-state>UP</link-state>
 *             </state>
 *             <ingress>
 *                 <state>
 *                     <transceiver>transceiver-11801</transceiver>
 *                 </state>
 *             </ingress>
 *         </channel>
 *     <logical-channels>
 * <terminal-device>
 * //CHECKSTYLE:ON
 */
public class PhoenixTerminalDeviceDiscovery
        extends AbstractHandlerBehaviour
        implements OdtnDeviceDescriptionDiscovery, DeviceDescriptionDiscovery {

    private static final String RPC_TAG_NETCONF_BASE =
            "<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">";

    private static final String OC_PLATFORM_TYPES_OPERATING_SYSTEM =
            "oc-platform-types:OPERATING_SYSTEM";

    private static final String RPC_CLOSE_TAG = "</rpc>";

    private static final String OC_PLATFORM_TYPES_TRANSCEIVER =
            "oc-platform-types:TRANSCEIVER";

    private static final String OC_PLATFORM_TYPES_PORT =
            "oc-platform-types:PORT";

    private static final String OC_TRANSPORT_TYPES_OPTICAL_CHANNEL =
            "oc-opt-types:OPTICAL_CHANNEL";

    private static final String OC_PLATFORM_TYPES_ACTIVE =
            "oc-platform-types:ACTIVE";

    private static final String OC_PLATFORM_TYPES_INACTIVE =
            "oc-platform-types:INACTIVE";

    private static final Logger log = getLogger(PhoenixTerminalDeviceDiscovery.class);

    private Set<Integer> supportedOpModeIds;

    private List<OcOperationalMode> describedOpModes;

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
    private String getDeviceComponentTypeBuilder(String componentType) {
        StringBuilder filter = new StringBuilder();
        filter.append("<components xmlns='http://openconfig.net/yang/platform'>");
        filter.append("<component>");
        filter.append("<state>");
        filter.append("<type xmlns:oc-platform-types='http://openconfig.net/yang/platform-types'>");
        filter.append(componentType);
        filter.append("</type>");
        filter.append("</state>");
        filter.append("</component>");
        filter.append("</components>");

        log.info("Request sent {}", filter);

        return filteredGetBuilder(filter.toString());
    }

    /**
     * Builds a request to get Device Components, config and operational data.
     *
     * @return A string with the Netconf RPC for a get with subtree rpcing based on
     *    /components/
     */
    private String getDeviceComponentsBuilder() {
        return filteredGetBuilder(
                "<components xmlns='http://openconfig.net/yang/platform'/>");
    }

    private String getOperationalModesBuilder() {
        StringBuilder filter = new StringBuilder();
        filter.append("<operational-modes xmlns='http://example.net/yang/openconfig-terminal-device-properties'/>");

        log.info("Request sent {}", filter);

        return filteredGetBuilder(filter.toString());
    }

    private String getSupportedOperationalModesBuilder() {
        StringBuilder filter = new StringBuilder();

        filter.append("<terminal-device xmlns='http://openconfig.net/yang/terminal-device'>");
        filter.append("<operational-modes/>");
        filter.append("</terminal-device>");

        log.info("Request sent {}", filter);

        return filteredGetBuilder(filter.toString());
    }

    private Set<Integer> getSupportedOpModeIds() {

        try {
            XPathExpressionEngine xpe = new XPathExpressionEngine();
            NetconfSession session = getNetconfSession(did());
            if (session == null) {
                log.error("getSupportedOpModeIds called with null session for {}", did());
                return null;
            }

            CompletableFuture<String> fut = session.rpc(getSupportedOperationalModesBuilder());
            String rpcReply = fut.get();

            log.debug("REPLY OP_MODES {}", rpcReply);

            XMLConfiguration xconf = (XMLConfiguration) XmlConfigParser.loadXmlString(rpcReply);
            xconf.setExpressionEngine(xpe);

            HierarchicalConfiguration modes = xconf.configurationAt("data/terminal-device/operational-modes");
            return parseSupportedOpModes(modes);
        } catch (Exception e) {
            log.error("Exception discoverOperationalModes() {}", did(), e);
            //return ImmutableList.of();
            return null;
        }
    }

    /**
     * Builds a request to get Device Ports, config and operational data.
     *
     * @return A string with the Netconf RPC for a get with subtree rpcing based on
     *    /components/component/state/type being oc-platform-types:PORT
     */
    private String getDevicePortsBuilder() {
        StringBuilder rpc = new StringBuilder();
        rpc.append("<components xmlns='http://openconfig.net/yang/platform'>");
        rpc.append(" <component><state>");
        rpc.append("   <type xmlns:oc-platform-types='http://openconfig.net/");
        rpc.append("yang/platform-types'>oc-platform-types:PORT</type>");
        rpc.append(" </state></component>");
        rpc.append("</components>");

        return filteredGetBuilder(rpc.toString());
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
        OcOperationalModesManager modesManager = checkNotNull(handler().get(OcOperationalModesManager.class));

        boolean defaultAvailable = true;
        SparseAnnotations annotations = DefaultAnnotations.builder().build();

        log.info("PhoenixTerminalDeviceDiscovery::discoverDeviceDetails device {}", did());

        // Other option "OTN" or "OTHER", we use TERMINAL_DEVICE
        org.onosproject.net.Device.Type type = Device.Type.TERMINAL_DEVICE;

        // Some defaults
        String vendor       = "NEC";
        String serialNumber = "none";
        String hwVersion    = "none";
        String swVersion    = "none";
        String chassisId    = "128";

        // Get the session
        NetconfSession session = getNetconfSession(did());

        //First get operation 3 attemps
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                String reply = session.get(getDeviceComponentTypeBuilder(OC_PLATFORM_TYPES_OPERATING_SYSTEM));
                log.debug("REPLY to DeviceDescription {}", reply);

                // <rpc-reply> as root node, software hardare version requires openconfig >= 2018
                XMLConfiguration xconf = (XMLConfiguration) XmlConfigParser.loadXmlString(reply);
                vendor = xconf.getString("data.components.component.state.mfg-name", vendor);
                serialNumber = xconf.getString("data.components.component.state.serial-no", serialNumber);
                swVersion = xconf.getString("data.components.component.state.software-version", swVersion);
                hwVersion = xconf.getString("data.components.component.state.hardware-version", hwVersion);

                break;
            } catch (Exception e) {
                log.error("discoverDeviceDetails - Netconf error device {} attempt {} of 3", did(), attempt);

                ///Evaluate here additional sleep

                if (attempt == 3 ) {
                    throw new IllegalStateException(new NetconfException("Failed to retrieve version info.", e));
                }
            }
        }

        ChassisId cid = new ChassisId(Long.valueOf(chassisId, 10));

        log.info("Device retrieved details");
        log.info("VENDOR    {}", vendor);
        log.info("HWVERSION {}", hwVersion);
        log.info("SWVERSION {}", swVersion);
        log.info("SERIAL    {}", serialNumber);
        log.info("CHASSISID {}", chassisId);

        //Discover supported Operational modes --- To be TESTED
        try {
            Thread.sleep(10000L);
        } catch (InterruptedException e) {
            log.error("Error while sleeping - BEFORE SUPPORTED OP-MODES");
            throw new IllegalStateException(new NetconfException("Error while sleeping", e));
        }

        log.info("Discovering supported operational modes...");
        supportedOpModeIds = getSupportedOpModeIds();

        //Discover supported Operational modes --- To be TESTED
        try {
            Thread.sleep(10000L);
        } catch (InterruptedException e) {
            log.error("Error while sleeping - BEFORE OP-MODES");
            throw new IllegalStateException(new NetconfException("Error while sleeping", e));
        }

        log.info("Discovering described operational modes...");
        describedOpModes = discoverOperationalModes();

        //Only add in the store the actually supported operational modes
        for (OcOperationalMode mode : describedOpModes) {
            if (supportedOpModeIds.contains(mode.modeId)) {

                //If the opmode is already present in the database only update list of supporting devices
                //Otherwise register a new opmode
                if (modesManager.isRegisteredMode(mode.modeId)) {
                    modesManager.getFromDatabase(mode.modeId).supportingDevices.add(did());
                } else {
                    log.info("ADDING SUPPORTED DEVICES opmode {} device {}", mode.modeId, did());
                    mode.supportingDevices.add(did());
                    modesManager.addToDatabase(mode);
                }
            }
        }

        //Additional sleeping to avoid contetion on the device
        try {
            Thread.sleep(10000L);
        } catch (InterruptedException e) {
            log.error("Error while sleeping - AFTER OP-MODES");
            throw new IllegalStateException(new NetconfException("Error while sleeping", e));
        }

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

            //here it could be improved getting only PORTS components but device is generating reply errors
            CompletableFuture<String> fut = session.rpc(getDeviceComponentsBuilder());
            String rpcReply = fut.get();

            XMLConfiguration xconf = (XMLConfiguration) XmlConfigParser.loadXmlString(rpcReply);
            xconf.setExpressionEngine(xpe);

            log.debug("REPLY PORTS details {}", rpcReply);

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
        return components.configurationsAt("component").stream()
                .filter(component -> {
                    return !component.getString("name", "unknown").equals("unknown") &&
                            component.getString("state/type", "unknown").equals(OC_PLATFORM_TYPES_PORT);
                })
                .map(component -> {
                            try {
                                // Pass the root document for cross-reference
                                return parsePortComponent(component, components);
                            } catch (Exception e) {
                                return null;
                            }
                        }
                )
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Checks if a given component has a subcomponent of a given type.
     *
     * @param component subtree to parse looking for subcomponents.
     * @param components the full components tree, to cross-ref in
     *  case we need to check (sub)components' types.
     *
     * @return true or false
     */
    private boolean hasSubComponentOfType(
            HierarchicalConfiguration component,
            HierarchicalConfiguration components,
            String type) {
        long count = component.configurationsAt("subcomponents/subcomponent")
                .stream()
                .filter(subcomponent -> {
                    String scName = subcomponent.getString("name");
                    StringBuilder sb = new StringBuilder("component[name='");
                    sb.append(scName);
                    sb.append("']/state/type");
                    String scType = components.getString(sb.toString(), "unknown");
                    return scType.equals(type);
                })
                .count();
        return (count > 0);
    }


    /**
     * Checks if a given component has a subcomponent of type OPTICAL_CHANNEL.
     *
     * @param component subtree to parse
     * @param components the full components tree, to cross-ref in
     *  case we need to check transceivers or optical channels.
     *
     * @return true or false
     */
    private boolean hasOpticalChannelSubComponent(
            HierarchicalConfiguration component,
            HierarchicalConfiguration components) {
        return hasSubComponentOfType(component, components, OC_TRANSPORT_TYPES_OPTICAL_CHANNEL);
    }


    /**
     *  Checks if a given component has a subcomponent of type TRANSCEIVER.
     *
     * @param component subtree to parse
     * @param components the full components tree, to cross-ref in
     *  case we need to check transceivers or optical channels.
     *
     * @return true or false
     */
    private boolean hasTransceiverSubComponent(
            HierarchicalConfiguration component,
            HierarchicalConfiguration components) {
        return hasSubComponentOfType(component, components, OC_PLATFORM_TYPES_TRANSCEIVER);
    }


    /**
     * Parses a component XML doc into a PortDescription.
     *
     * @param component subtree to parse. It must be a component ot type PORT.
     * @param components the full components tree, to cross-ref in
     *  case we need to check transceivers or optical channels.
     *
     * @return PortDescription or null if component does not have onos-index
     */
    private PortDescription parsePortComponent(
            HierarchicalConfiguration component,
            HierarchicalConfiguration components) {
        Map<String, String> annotations = new HashMap<>();
        String name = component.getString("name");
        String type = component.getString("port/optical-port/state/optical-port-type");
        String status = component.getString("state/oper-status");

        log.info("Parsing Component {} type {}", name, type);

        annotations.put(OdtnDeviceDescriptionDiscovery.OC_NAME, name);
        annotations.put(OdtnDeviceDescriptionDiscovery.OC_TYPE, type);
        annotations.put(OdtnDeviceDescriptionDiscovery.OC_STATUS, status);
        annotations.put(OC_TRANSCEIVER_NAME, phoenixTransceiverName(name));
        annotations.put(OC_OPTICAL_CHANNEL_NAME, phoenixOpticalChannelName(name));

        // Assing an ONOS port number
        PortNumber portNum = phoenixPortName(name);

        log.info("PORT: {} assigned ONOS number: {}", name, portNum);
        log.info("PORT: {} associated to TRANSCEIVER {}", name, phoenixTransceiverName(name));
        log.info("PORT: {} associated to OPTICAL_CHANNEL {}", name, phoenixOpticalChannelName(name));

        // The heuristic to know if it is client or line side
        if (!annotations.containsKey(PORT_TYPE)) {
            if (type.equals("oc-opt-types:TERMINAL_CLIENT")) {
                annotations.put(PORT_TYPE, OdtnPortType.CLIENT.value());
            } else if (type.equals("oc-opt-types:TERMINAL_LINE")) {
                annotations.put(PORT_TYPE, OdtnPortType.LINE.value());
            }
        }

        Boolean isEnabled = false;
        if (status.equals(OC_PLATFORM_TYPES_ACTIVE)) {
            isEnabled = true;
        }

        // Build the port
        // NOTE: using portNumber(id, name) breaks things. Intent parsing, port resource management, etc. There seems
        // to be an issue with resource mapping
        if (annotations.get(PORT_TYPE).equals(OdtnPortType.CLIENT.value())) {
            log.info("PORT {} number {} added as CLIENT port", name, portNum);

            return OduCltPortHelper.oduCltPortDescription(portNum,
                    isEnabled,
                    CltSignalType.CLT_10GBE,
                    DefaultAnnotations.builder().putAll(annotations).build());
        }
        if (annotations.get(PORT_TYPE).equals(OdtnPortType.LINE.value())) {
            log.info("PORT {} number {} added as LINE port", name, portNum);

            // TODO: To be configured
            OchSignal signalId = OchSignal.newDwdmSlot(ChannelSpacing.CHL_50GHZ, 1);

            return OchPortHelper.ochPortDescription(
                    portNum,
                    isEnabled,
                    OduSignalType.ODU4, // TODO Client signal to be discovered
                    true,
                    signalId,
                    DefaultAnnotations.builder().putAll(annotations).build());
        }
        log.error("PORT {} number {} is of UNKNOWN type", name, portNum);
        return null;
    }

    protected static PortNumber phoenixPortName(String portName) {

        if (portName == null || !portName.matches("(cfp2|qsfp)-\\d+")) {
            log.error("Port name is not in the expected format: {}", portName);
            throw new IllegalArgumentException("Invalid module format");
        }

        String[] parts = portName.split("-");
        String prefix = parts[0];
        int index = Integer.parseInt(parts[1]);

        int portIndex = 0;
        if (prefix.equals("cfp2")) {
            portIndex = 100 + index;
        } else if (prefix.equals("qsfp")) {
            portIndex = 1000 + index;
        }

        return PortNumber.portNumber(portIndex);
    }

    protected static String phoenixPortNumber(int value) {
        if (value >= 100 && value < 200) {
            return "cfp2-" + (value - 100);
        } else if (value >= 1000 && value < 1100) {
            return "qsfp-" + (value - 1000);
        } else {
            log.error("Port number is not in the expected range: {}", value);
            throw new IllegalArgumentException("Invalid value for module");
        }
    }

    protected static String phoenixTransceiverName(String portName) {
        if (portName == null || !portName.matches("(cfp2|qsfp)-\\d+")) {
            log.error("Port name is not in the expected format: {}", portName);
            throw new IllegalArgumentException("Invalid module format");
        }

        String[] parts = portName.split("-");
        String prefix = parts[0];
        int index = Integer.parseInt(parts[1]);

        String transceiver = prefix + "-transceiver-" + String.valueOf(index);

        return transceiver;
    }

    protected static String phoenixOpticalChannelName(String portName) {
        if (portName == null || !portName.matches("(cfp2|qsfp)-\\d+")) {
            log.error("Port name is not in the expected format: {}", portName);
            throw new IllegalArgumentException("Invalid module format");
        }

        String[] parts = portName.split("-");
        String prefix = parts[0];
        int index = Integer.parseInt(parts[1]);

        String channel = prefix + "-opt-" + String.valueOf(index) + "-1";

        return channel;
    }

    /**
     * Parses operational mode information from OpenConfig XML configuration.
     *
     * @param modes the XML document with components root.
     * @return List of ports
     *
     * //CHECKSTYLE:OFF
     * <pre>{@code
     *   <operational-modes xmlns='http://example.net/yang/openconfig-terminal-device-properties'/>
     *     <mode-descriptor>....
     *     </mode-descriptor>
     *     <mode-descriptor>....
     *     </mode-descriptor>
     *   </operational-modes>
     * }</pre>
     * //CHECKSTYLE:ON
     */
    protected List<OcOperationalMode> parseOperationalModes(HierarchicalConfiguration modes) {
        return modes.configurationsAt("mode-descriptor").stream()
                .map(mode -> {
                            try {
                                // Pass the root document for cross-reference
                                return parseOperationalMode(mode, modes);
                            } catch (Exception e) {
                                return null;
                            }
                        }
                )
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    protected Set<Integer> parseSupportedOpModes(HierarchicalConfiguration modes) {
        return modes.configurationsAt("mode").stream()
                .map(mode -> {
                            try {
                                // Pass the root document for cross-reference
                                return parseSupportedOpMode(mode, modes);
                            } catch (Exception e) {
                                return null;
                            }
                        }
                )
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public List<OcOperationalMode> discoverOperationalModes() {

        try {
            XPathExpressionEngine xpe = new XPathExpressionEngine();
            NetconfSession session = getNetconfSession(did());
            if (session == null) {
                log.error("discoverOperationalModes called with null session for {}", did());
                return null;
            }

            CompletableFuture<String> fut = session.rpc(getOperationalModesBuilder());
            String rpcReply = fut.get();

            log.debug("REPLY OP_MODES {}", rpcReply);

            XMLConfiguration xconf = (XMLConfiguration) XmlConfigParser.loadXmlString(rpcReply);
            xconf.setExpressionEngine(xpe);

            HierarchicalConfiguration modes = xconf.configurationAt("data/operational-modes");
            return parseOperationalModes(modes);
        } catch (Exception e) {
            log.error("Exception discoverOperationalModes() {}", did(), e);
            //return ImmutableList.of();
            return null;
        }
    }

    /**
     * Parses a mode-descriptor XML doc into a PortDescription.
     *
     * @param mode subtree to parse.
     * @param modes the full components tree.
     *
     * @return null
     */
    private OcOperationalMode parseOperationalMode(HierarchicalConfiguration mode, HierarchicalConfiguration modes) {
        //Map<String, String> annotations = new HashMap<>();
        int id = Integer.decode(mode.getString("state/mode-id"));
        String type = mode.getString("state/mode-type");

        log.info("Parsing Operational Mode id {} type {}", id, type);

        //OperationalMode opMode = new OperationalMode(id, type);
        OcOperationalMode opMode = OcOperationalMode.decodeFromXml(mode);
        return opMode;

    }

    private int parseSupportedOpMode(HierarchicalConfiguration mode, HierarchicalConfiguration modes) {
        int opModeId = Integer.decode(mode.getString("state/mode-id"));

        log.info("SUPPORTED OPMODE mode-id {}", opModeId);

        return opModeId;
    }
}
