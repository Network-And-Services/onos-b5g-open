package org.onosproject.drivers.odtn.openroadm;

import com.google.common.collect.ImmutableList;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.onlab.packet.ChassisId;
import org.onlab.util.Frequency;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.*;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.intent.OpticalPathIntent;
import org.onosproject.net.optical.device.OchPortHelper;
import org.onosproject.net.optical.device.OmsPortHelper;
import org.onosproject.netconf.DatastoreId;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Driver Implementation of the DeviceDescrption discovery for OpenROADM v12.
 */
public class SeasonDeviceDescription extends OpenRoadmNetconfHandlerBehaviour
        implements DeviceDescriptionDiscovery {

    // These annotations are added to the device and ports
    public final class AnnotationKeys {
        public static final String SEASON_PORT_TYPE = "season-port-type";

        public static final String OPENROADM_NODEID = "openroadm-node-id";
        public static final String OPENROADM_CIRCUIT_PACK_NAME =
                "openroadm-circuit-pack-name";
        public static final String OPENROADM_PORT_NAME = "openroadm-port-name";
        public static final String OPENROADM_PARTNER_CIRCUIT_PACK_NAME =
                "openroadm-partner-circuit-pack-name";
        public static final String OPENROADM_PARTNER_PORT_NAME =
                "openroadm-partner-port-name";
        public static final String OPENROADM_LOGICAL_CONNECTION_POINT =
                "openroadm-logical-connection-point";
        public static final String OPENROADM_PORT_POWER_CAP_MIN_RX =
                "openroadm-port-power-cap-min-rx";
        public static final String OPENROADM_PORT_POWER_CAP_MIN_TX =
                "openroadm-port-power-cap-min-tx";
        public static final String OPENROADM_PORT_POWER_CAP_MAX_RX =
                "openroadm-port-power-cap-max-rx";
        public static final String OPENROADM_PORT_POWER_CAP_MAX_TX =
                "openroadm-port-power-cap-max-tx";
        private AnnotationKeys() {
            // utility class
        }
    }

    public static final ChannelSpacing CHANNEL_SPACING =
            ChannelSpacing.CHL_50GHZ;

    public static final String OPENROADM_DEVICE_OPEN = //
            "<org-openroadm-device xmlns=\"http://org/openroadm/device\">";
    public static final String OPENROADM_DEVICE_CLOSE = //
            "</org-openroadm-device>";
    public static final String CAPABILITY_OPENROADM_DEVICE =
            "http://org/openroadm/device?module=org-openroadm-device&amp;revision=";
    public static final String CAPABILITY_OPENROADM_ALARM =
            "http://org/openroadm/alarm?module=org-openroadm-alarm&amp;revision=";

    /**
     * Builds a request to get OpenRoadm Device main node (within root).
     *
     *  @param nodeTag the tag with the name to get e.g. <info/>
     *
     * @return A string with the Netconf RPC for a get with subtree info
     */
    private String getDeviceXmlNodeBuilder(final String nodeTag) {
        StringBuilder filter = new StringBuilder();
        filter.append(OPENROADM_DEVICE_OPEN);
        filter.append(nodeTag);
        filter.append(OPENROADM_DEVICE_CLOSE);
        return filteredGetBuilder(filter.toString());
    }

    /**
     * Builds a request to get Device details (<info>).
     *
     * @return A string with the Netconf RPC for a get with subtree info
     */
    private String getDeviceDetailsBuilder() {
        return getDeviceXmlNodeBuilder("<info/>");
    }

    /**
     * Builds a request to get specific circuit pack data (by name).
     *
     * @return A string with the Netconf RPC
     */
    private String getDeviceCircuitPackByNameBuilder(String cpName) {
        StringBuilder filter = new StringBuilder();
        filter.append(OPENROADM_DEVICE_OPEN);
        filter.append("<circuit-packs>");
        filter.append("<circuit-pack-name>");
        filter.append(cpName);
        filter.append("</circuit-pack-name>");
        filter.append("</circuit-packs>");
        filter.append(OPENROADM_DEVICE_CLOSE);
        return filteredGetBuilder(filter.toString());
    }

    /**
     * Builds a request to get specific port data.
     * Ports are uniquely identified by the couple (circuit-pack-name, port-name)
     *
     * @return A string with the Netconf filter for the get-config operation.
     */
    private String getDevicePortBuilder(String cpName, String portName) {
        StringBuilder filter = new StringBuilder();
        filter.append(OPENROADM_DEVICE_OPEN);
        filter.append("<circuit-packs>");
        filter.append("<circuit-pack-name>");
        filter.append(cpName);
        filter.append("</circuit-pack-name>");
        filter.append("<ports>");
        filter.append("<port-name>");
        filter.append(portName);
        filter.append("</port-name>");
        filter.append("</ports>");
        filter.append("</circuit-packs>");
        filter.append(OPENROADM_DEVICE_CLOSE);
        return filteredGetBuilder(filter.toString());
    }

    /**
     * Builds a request to get External Links data (<external-link>).
     *
     * @return A string with the Netconf filter for the get-config operation.
     */
    private String getDeviceExternalLinksBuilder() {
        StringBuilder rb = new StringBuilder();
        rb.append(OPENROADM_DEVICE_OPEN);
        rb.append("<external-link/>");
        rb.append(OPENROADM_DEVICE_CLOSE);
        return rb.toString();
    }

    /**
     * Builds a request to get Device Degrees, config and operational data.
     *
     * @return A string with the Netconf RPC for a get with subtree
     */
    private String getDeviceDegreesBuilder() {
        return getDeviceXmlNodeBuilder("<degree/>");
    }

    /**
     * Builds a request to get Device SharedRiskGroups, config and operational
     * data.
     *
     * @return A string with the Netconf RPC for a get with subtree
     */
    private String getDeviceSharedRiskGroupsBuilder() {
        return getDeviceXmlNodeBuilder("<shared-risk-group/>");
    }

    /**
     * Builds a request to get Ports data.
     * Changed to XPath and added one based on classic filters since some agents
     * do not support xpath filtering.
     *
     * @return A string with the Netconf RPC
     */
    private String getDeviceExternalPortsBuilderXPath() {
        StringBuilder filter = new StringBuilder();
        filter.append(
                "/org-openroadm-device/circuit-packs/ports[port-qual='roadm-external']");
        return xpathFilteredGetBuilder(filter.toString());
    }

    /**
     * Builds a request to get Ports data.
     *
     * @return A string with the Netconf RPC
     */
    private String getDeviceExternalPortsBuilder() {
        StringBuilder filter = new StringBuilder();
        filter.append(OPENROADM_DEVICE_OPEN);
        filter.append("<circuit-packs>");
        filter.append(" <ports>");
        filter.append("  <port-qual>roadm-external</port-qual>");
        filter.append(" </ports>");
        filter.append("</circuit-packs>");
        filter.append(OPENROADM_DEVICE_CLOSE);
        return filteredGetBuilder(filter.toString());
    }

    /**
     * Builds a request to get External Links data.
     *
     * @return A string with the Netconf RPC
     */
    private String getDeviceExternalLinksBuilderXpath() {
        StringBuilder filter = new StringBuilder();
        filter.append("/org-openroadm-device/external-link");
        return xpathFilteredGetBuilder(filter.toString());
    }

    /**
     * Builds a request to get External Links data.
     *
     * @param nodeId OpenROADM node identifier.
     * @param circuitPackName name of the circuit part of the port.
     * @param portName name of the port.
     * @return A string with the Netconf RPC
     */
    private String getDeviceExternalLinkForPortBuilderXPath(
            String nodeId, String circuitPackName, String portName) {
        StringBuilder filter = new StringBuilder();
        filter.append("/org-openroadm-device/external-link[");
        filter.append("./source/node-id='");
        filter.append(nodeId);
        filter.append("' and ");
        filter.append("./source/circuit-pack-name='");
        filter.append(circuitPackName);
        filter.append("' and ");
        filter.append("./source/port-name='");
        filter.append(portName);
        filter.append("']");
        return xpathFilteredGetBuilder(filter.toString());
    }

    private String getDeviceExternalLinkForPortBuilder(String nodeId,
                                                       String circuitPackName,
                                                       String portName) {
        StringBuilder filter = new StringBuilder();
        filter.append(OPENROADM_DEVICE_OPEN);
        filter.append("<external-link>");
        filter.append(" <source>");
        filter.append("  <node-id>");
        filter.append(nodeId);
        filter.append("</node-id>");
        filter.append("  <circuit-pack-name>");
        filter.append(circuitPackName);
        filter.append("</circuit-pack-name>");
        filter.append("  <port-name>");
        filter.append(portName);
        filter.append("</port-name>");
        filter.append(" </source>");
        filter.append("</external-link>");
        filter.append(OPENROADM_DEVICE_CLOSE);
        return xpathFilteredGetBuilder(filter.toString());
    }



    /**
     * Returns a DeviceDescription with Device info.
     *
     * @return DeviceDescription or null
     */
    @Override
    public DeviceDescription discoverDeviceDetails() {
        boolean defaultAvailable = true;
        NetconfDevice ncDevice = getNetconfDevice();
        if (ncDevice == null) {
            log.error("ONOS Error: Device reachable, deviceID {} is not in Map", did());
            return null;
        }
        DefaultAnnotations.Builder annotationsBuilder =
                DefaultAnnotations.builder();

        // Some defaults
        String vendor = "UNKNOWN";
        String hwVersion = "2.2.0";
        String swVersion = "2.2.0";
        String serialNumber = "0x0000";
        String chassisId = "0";
        String nodeType = "rdm";

        // Get the session, if null, at least we can use the defaults.
        NetconfSession session = getNetconfSession(did());
        if (session != null) {
            try {
                String reply = session.rpc(getDeviceDetailsBuilder()).get();
                XMLConfiguration xconf =
                        (XMLConfiguration) XmlConfigParser.loadXmlString(reply);
                String nodeId =
                        xconf.getString("data.org-openroadm-device.info.node-id", "");
                if (nodeId.equals("")) {
                    log.error("[OPENROADM] {} org-openroadm-device node-id undefined, returning", did());
                    return null;
                }
                annotationsBuilder.set(AnnotationKeys.OPENROADM_NODEID, nodeId);
                nodeType = xconf.getString("data.org-openroadm-device.info.node-type", "");
                if (nodeType.equals("")) {
                    log.error("[OPENROADM] {} empty node-type", did());
                    return null;
                }
                vendor = xconf.getString(
                        "data.org-openroadm-device.info.vendor", vendor);
                hwVersion = xconf.getString(
                        "data.org-openroadm-device.info.model", hwVersion);
                swVersion = xconf.getString(
                        "data.org-openroadm-device.info.softwareVersion", swVersion);
                serialNumber = xconf.getString(
                        "data.org-openroadm-device.info.serial-id", serialNumber);
                chassisId = xconf.getString(
                        "data.org-openroadm-device.info.node-number", chassisId);

                // GEOLOCATION
                String longitudeStr = xconf.getString(
                        "data.org-openroadm-device.info.geoLocation.longitude");
                String latitudeStr = xconf.getString(
                        "data.org-openroadm-device.info.geoLocation.latitude");
                if (longitudeStr != null && latitudeStr != null) {
                    annotationsBuilder
                            .set(org.onosproject.net.AnnotationKeys.LONGITUDE,
                                    longitudeStr)
                            .set(org.onosproject.net.AnnotationKeys.LATITUDE,
                                    latitudeStr);
                }
            } catch (NetconfException | InterruptedException | ExecutionException e) {
                log.error("[OPENROADM] {} exception", did());
                return null;
            }
        } else {
            log.debug("[OPENROADM] - No  session {}", did());
        }

        log.debug("[OPENROADM] {} - VENDOR {} HWVERSION {} SWVERSION {} SERIAL {} CHASSIS {}",
                did(), vendor, hwVersion, swVersion, serialNumber, chassisId);
        ChassisId cid = new ChassisId(Long.valueOf(chassisId, 10));
        /*
         * OpenROADM defines multiple devices (node types). This driver has been tested with
         * ROADMS, (node type, "rdm"). Other devices can also be discovered, and this code is here
         * for future developments - untested - it is likely that the XML documents
         * are model specific.
         */
        org.onosproject.net.Device.Type type;
        if (nodeType.equals("rdm")) {
            type = org.onosproject.net.Device.Type.ROADM;
        } else if (nodeType.equals("ila")) {
            type = org.onosproject.net.Device.Type.OPTICAL_AMPLIFIER;
        } else if (nodeType.equals("xpdr")) {
            type = org.onosproject.net.Device.Type.TERMINAL_DEVICE;
        } else if (nodeType.equals("extplug")) {
            type = org.onosproject.net.Device.Type.OTHER;
        } else {
            log.error("[OPENROADM] {} unsupported node-type", did());
            return null;
        }
        DeviceDescription desc = new DefaultDeviceDescription(
                did().uri(), type, vendor, hwVersion, swVersion, serialNumber, cid,
                defaultAvailable, annotationsBuilder.build());
        return desc;
    }

    /**
     * Get config and status info for a specific port as a
     * list of XML hierarchical configs.
     * @param session the NETConf session to the OpenROADM device.
     * @param cpName the name of the circuit-pack hosting the port
     * @param portName the name of the requested port
     * @return the hierarchical conf. for the port.
     */
    HierarchicalConfiguration getPortState(NetconfSession session, String cpName, String portName) {
        try {
            String reply = session.rpc(getDevicePortBuilder(cpName, portName)).get();
            XMLConfiguration cpConf =
                    (XMLConfiguration) XmlConfigParser.loadXmlString(reply);
            cpConf.setExpressionEngine(new XPathExpressionEngine());
            List<HierarchicalConfiguration> port = cpConf.configurationsAt(
                    "/data/org-openroadm-device/circuit-packs/ports");
            // Security check but it shouldn't happen
            if (port.size() > 1) {
                log.warn("[OPENROADM] More than one port with the same name. Using first one");
            }
            return port.get(0);
        } catch (NetconfException | InterruptedException | ExecutionException e) {
            log.error("[OPENROADM] {} exception getting port {}/{}: {}", did(), cpName, portName, e);
            return null;
        }
    }

    /**
     * Get the external links as a list of XML hieriarchical configs.
     *  @param session the NETConf session to the OpenROADM device.
     *  @return a list of hierarchical conf. each one external link.
     */
    List<HierarchicalConfiguration> getExternalLinks(NetconfSession session) {
        try {
            String reply = session.getConfig(DatastoreId.RUNNING, getDeviceExternalLinksBuilder());
            XMLConfiguration extLinksConf =
                    (XMLConfiguration) XmlConfigParser.loadXmlString(reply);
            extLinksConf.setExpressionEngine(new XPathExpressionEngine());
            return extLinksConf.configurationsAt(
                    "/data/org-openroadm-device/external-link");
        } catch (NetconfException e) {
            log.error("[OPENROADM] {} exception getting external links", did());
            return ImmutableList.of();
        }
    }


    /**
     * Get config and status info for a specific circuit pack as a
     * list of XML hierarchical configs.
     * @param session the NETConf session to the OpenROADM device.
     * @param cpName the name of the requested circuit-pack
     * @return the hierarchical conf. for the circuit pack.
     */
    HierarchicalConfiguration getCircuitPackByName(NetconfSession session, String cpName) {
        try {
            String reply = session.rpc(getDeviceCircuitPackByNameBuilder(cpName)).get();
            XMLConfiguration cpConf =
                    (XMLConfiguration) XmlConfigParser.loadXmlString(reply);
            cpConf.setExpressionEngine(new XPathExpressionEngine());
            List<HierarchicalConfiguration> cPacks = cpConf.configurationsAt(
                    "/data/org-openroadm-device/circuit-packs");
            // <circuit-pack-name> is the key for the list.
            // It shouldn't happen they are > 1
            if (cPacks.size() > 1) {
                log.warn("[OPENROADM] More than one circuit pack with the same name. Using first one");
            }
            return cPacks.get(0);
        } catch (NetconfException | InterruptedException | ExecutionException e) {
            log.error("[OPENROADM] {} exception getting circuit pack {}: {}", did(), cpName, e);
            return null;
        }
    }


    /**
     * Get config and status info for the degrees from the device as a list
     * of XML hierarchical configs.
     *  @param session the NETConf session to the OpenROADM device.
     *  @return a list of hierarchical conf. each one degree.
     */
    List<HierarchicalConfiguration> getDegrees(NetconfSession session) {
        try {
            String reply = session.rpc(getDeviceDegreesBuilder()).get();
            XMLConfiguration conf =
                    (XMLConfiguration) XmlConfigParser.loadXmlString(reply);
            conf.setExpressionEngine(new XPathExpressionEngine());
            return conf.configurationsAt(
                    "/data/org-openroadm-device/degree");
        } catch (NetconfException | InterruptedException | ExecutionException e) {
            log.error("[OPENROADM] {} exception getting degrees: {}", did(), e);
            return ImmutableList.of();
        }
    }


    /**
     * Get config and status info for the SRGs from the device as a list
     * of XML hierarchical configs.
     *  @param session the NETConf session to the OpenROADM device.
     *  @return a list of hierarchical conf. each one SRG.
     */
    List<HierarchicalConfiguration> getSrgs(NetconfSession session) {
        try {
            String reply = session.rpc(getDeviceSharedRiskGroupsBuilder()).get();
            XMLConfiguration conf =
                    (XMLConfiguration) XmlConfigParser.loadXmlString(reply);
            conf.setExpressionEngine(new XPathExpressionEngine());
            return conf.configurationsAt(
                    "/data/org-openroadm-device/shared-risk-group");
        } catch (NetconfException | InterruptedException | ExecutionException e) {
            log.error("[OPENROADM] {} exception getting SRGs: {}", did(), e);
            return ImmutableList.of();
        }
    }

    /**
     * Returns a list of PortDescriptions for the device.
     *
     * @return a list of descriptions.
     */
    /*
     * Assumption: ROADM SRG (client) ports are OCh carrying ODU4 (should be
     *             configurable)
     */

    @Override
    public List<PortDescription> discoverPortDetails() {
        NetconfSession session = getNetconfSession(did());
        if (session == null) {
            log.error("discoverPortDetails null session for {}", did());
            return ImmutableList.of();
        }
        if (!getDevice().annotations().keys().contains("openroadm-node-id")) {
            log.error("Unable to run PortDiscovery: missing openroadm-node-id annotation." +
                    " Probable failure during DeviceDiscovery. Aborting!");
            return ImmutableList.of();
        }

        List<HierarchicalConfiguration> externalLinks = getExternalLinks(session);
        List<PortDescription> list = new ArrayList<PortDescription>();
        discoverDegreePorts(session, list, externalLinks);
        discoverSrgPorts(session, list, externalLinks);
        return list;
    }

    /**
     * Parses degree (ROADM) port information.
     *
     * @param session the NETConf session to the OpenROADM device.
     * @param list  List of port descriptions to append to.
     * @param externalLinks Hierarchical configuration containing all the external
     *        links.
     * Port-id is obtained from degree-number and from the index contained
     * in the <connection-ports> leaf.
     * For OpenROADM Device model 2.2 both <degree-number> and <index> inside
     * <connection-ports> are key for the related lists, so composing them
     * assures identificator uniqueness.
     * Degree IDs are chosen as 10 * degree-number + index to avoid overlapping
     * with SRGs IDs.
     * The above formula allows making a two-digit number starting from two
     * one-digit numbers (actually, only port index needs to be single digit and
     * this assumption is assured by what stated in the model:
     *     OpenROADM Device model 2.2 (line 675):
     *        (connection-ports) description "Port associated with degree: One if bi-directional; two if uni-directional"
     * If digits are A and B, to have a number in the form AB (i.e. 11, 12, 21,
     * 31, 42, ...) A must be multiplied by 10.
     *
     * Note that both bidirectional and unidirectional ports IDs are taken from
     * the datastore.
     * Ex. DEG1 bidirectional port
     *          ONOS port ID: 11
     *     DEG3 unidirectional ports
     *          ONOS port IDs: 31 and 32
     */
    private List<PortDescription>
    discoverDegreePorts(NetconfSession session,
                        List<PortDescription> list,
                        List<HierarchicalConfiguration> externalLinks) {
        int degreeNumber = 0;
        String nodeId = getDevice().annotations().value(AnnotationKeys.OPENROADM_NODEID);
        List<HierarchicalConfiguration> degrees = getDegrees(session);
        for (HierarchicalConfiguration degree : degrees) {
            degreeNumber = degree.getInt("degree-number", 0);
            // From OpenROADM Device model 2.2: degree-number must be > 0
            if (degreeNumber == 0) {
                log.warn("[OPENROADM] Device {} <degree-number> MUST be > 0", did());
                continue;
            }

            // Collect degree optical parameters, common to all ports.
            // Note that this might change in the future when supporting
            // more recent OpenROADM models.
            Map<String, String> degreeAnnotations = new HashMap<>();
            String maxWavelengths = degree.getString("max-wavelengths", "");
            String slotWidthGranularity = degree.getString("mc-capabilities/slot-width-granularity", "");
            String centerFreqGranularity = degree.getString("mc-capabilities/center-freq-granularity", "");
            String minSlots = degree.getString("mc-capabilities/min-slots", "");
            String maxSlots = degree.getString("mc-capabilities/max-slots", "");
            degreeAnnotations.put("openroadm-port-max-wavelengths", maxWavelengths);
            degreeAnnotations.put("openroadm-port-slot-width-granularity", slotWidthGranularity);
            degreeAnnotations.put("openroadm-port-center-freq-granularity", centerFreqGranularity);
            degreeAnnotations.put("openroadm-port-min-slots", minSlots);
            degreeAnnotations.put("openroadm-port-max-slots", maxSlots);

            List<HierarchicalConfiguration> connectionPorts =
                    degree.configurationsAt("connection-ports");
            if (connectionPorts.size() == 0) {
                log.warn("[OPENROADM] Device {} there are not connection-ports for degree-number {}", did(), degreeNumber);
            }
            for (HierarchicalConfiguration cport : connectionPorts) {
                int portIndex = cport.getInt("index", 0);
                long portNum = degreeNumber * 10 + portIndex;
                PortNumber pNum = PortNumber.portNumber(portNum);
                String cpName = cport.getString("circuit-pack-name", "");
                String portName = cport.getString("port-name", "");
                PortNumber reversepNum = findDegreeReversePort(degreeNumber, portIndex, connectionPorts);
                HierarchicalConfiguration eLink = parseExternalLink(externalLinks, cpName, portName);
                HierarchicalConfiguration port = getPortState(session, cpName, portName);
                DefaultAnnotations annotations = createPortAnnotations(degreeAnnotations,
                        cpName,
                        reversepNum,
                        port,
                        eLink);
                // Relationship : START and STOP Freq not being used (See
                // LambdaQuery)
                PortDescription pd =  OmsPortHelper.omsPortDescription(pNum,
                        true /* enabled */,
                        OpenRoadmCBandLambdaQuery.C_BAND_FIRST_CENTER_FREQ,
                        OpenRoadmCBandLambdaQuery.C_BAND_LAST_CENTER_FREQ,
                        CHANNEL_SPACING.frequency(),
                        annotations);
                if (pd != null) {
                    list.add(pd);
                }
            }
        }

        return list;
    }

    /**
     * Parses SRG (ROADM) port information.
     *
     * @param session the NETConf session to the OpenROADM device.
     * @param list  List of port descriptions to append to.
     * @param externalLinks Hierarchical configuration containing all the external
     *        links.
     *
     * Port-id is obtained from srg-number and the number of the client
     * port contained in the <logical-connection-point> leaf.
     * OpenROADM Device Whitepaper for release 2.2, sect. 7.2.2.2.1:
     *     "For the ROADM SRG add/drop port, the logical connection point should
     *      be set to the format “SRG<n>-PP<m>”, where <n> is the SRG number
     *      and <m> is the add/drop port pair identifier. For example SRG1
     *      add/drop port #7 would have the logical connection point set to
     *      SRG1-PP7".
     * The method extract <m> following the sustring PP and use it in conjuncion
     * with the degree-number taken from the <degree> branch (If the datastore is
     * consistent this should be the same number in SRG<n>).
     * To avoid overlapping with IDs assigned to degrees, the srg-number is multiplied
     * by 1000. The to cover the uni-directional case (that needs two IDs, one per
     * direction) the port index is multiplied by 10.
     * Using 1000 as multiplier avoids overlapping with degree port IDs as long as
     * the number of degree in the ROADM is less than 100. Current optical
     * technologies don't allow ROADMs having such a high number of degrees.
     *
     * For unidirectional links the logical connection point is assumed to
     * have the form DEGn-PPi[-TX/-RX] and to the RX link is assigned an ID
     * following (+1) the TX one.
     * Ex. SRG1 second port bidirectional link (SRG1-PP2)
     *                      ONOS port ID: 1020
     *     SRG2 third port, unidirectional link (SRG2-PP3-TX, SRG2-PP3-RX)
     *                      ONOS port IDs: 2030 and 2031
     */
    private List<PortDescription>
    discoverSrgPorts(NetconfSession session,
                     List<PortDescription> list,
                     List<HierarchicalConfiguration> externalLinks) {
        int srgNumber = 0;
        List<HierarchicalConfiguration> srgs = getSrgs(session);
        for (HierarchicalConfiguration s : srgs) {
            srgNumber = s.getInt("srg-number", 0);
            // From OpenROADM Device model 2.2: srg-number must be > 0
            if (srgNumber == 0) {
                log.warn("[OPENROADM] Device {} <srg-number> MUST be > 0", did());
                continue;
            }

            // Collect SRG optical parameters, common to all ports.
            // Note that this might change in the future when supporting
            // more recent OpenROADM models.
            Map<String, String> srgAnnotations = new HashMap<>();
            String slotWidthGranularity = s.getString("mc-capabilities/slot-width-granularity", "");
            String centerFreqGranularity = s.getString("mc-capabilities/center-freq-granularity", "");
            String minSlots = s.getString("mc-capabilities/min-slots", "");
            String maxSlots = s.getString("mc-capabilities/max-slots", "");
            srgAnnotations.put("openroadm-port-slot-width-granularity", slotWidthGranularity);
            srgAnnotations.put("openroadm-port-center-freq-granularity", centerFreqGranularity);
            srgAnnotations.put("openroadm-port-min-slots", minSlots);
            srgAnnotations.put("openroadm-port-max-slots", maxSlots);

            List<HierarchicalConfiguration> srgCircuitPacks =
                    s.configurationsAt("circuit-packs");
            for (HierarchicalConfiguration scp : srgCircuitPacks) {
                String srgCpName = scp.getString("circuit-pack-name");
                HierarchicalConfiguration cpConf = getCircuitPackByName(session, srgCpName);

                List<HierarchicalConfiguration> ports = cpConf.configurationsAt("ports[port-qual='roadm-external']");
                for (HierarchicalConfiguration p : ports) {
                    String portName = p.getString("port-name");
                    String lcp = p.getString("logical-connection-point", "unknown");
                    int ppIndex = lcp.indexOf("PP");
                    if (ppIndex == -1) {
                        log.warn("[OPENROADM] {}: cannot find port index for circuit-pack {}", did(), srgCpName);
                    } else {
                        long portNum, revPortNum;
                        String[] split = lcp.split("-");
                        // 1000 is chosen as base value to avoid overlapping
                        // with IDs for degree ports that have 10 as base value
                        long basePort = srgNumber * 1000 + Long.parseLong(split[1].replace("PP", "")) * 10;
                        if (split.length > 2) {
                            // Unidirectional port
                            portNum = basePort + (split[2].equals("RX") ? 1 : 0);
                            revPortNum = basePort + (split[2].equals("RX") ? 0 : 1);
                        } else {
                            // Bidirectional port
                            portNum = basePort;
                            revPortNum = 0;
                        }
                        PortNumber pNum = PortNumber.portNumber(portNum);
                        PortNumber reversepNum = PortNumber.portNumber(revPortNum);
                        HierarchicalConfiguration eLink = parseExternalLink(externalLinks, srgCpName, portName);
                        DefaultAnnotations annotations = createPortAnnotations(srgAnnotations,
                                srgCpName,
                                reversepNum,
                                p,
                                eLink);
                        OchSignal signalId =
                                OchSignal.newDwdmSlot(ChannelSpacing.CHL_50GHZ, 4);
                        PortDescription pd =  OchPortHelper.ochPortDescription(pNum,
                                true /* enabled */,
                                OduSignalType.ODU4,
                                true /* tunable */,
                                signalId,
                                annotations);
                        if (pd != null) {
                            list.add(pd);
                        }
                    }
                }
            }
        }

        return list;
    }

    /**
     * Locate (if present) the external link for a given <circuit-pack, port> pair
     * within the Hierarchical Configuration.
     *
     * @param extLinks Hierarchical configuration containing all the external
     *        links.
     * @param circuitPackName name of the circuit pack of the port.
     * @param portName name of the port.
     * @return the external link.
     */
    private HierarchicalConfiguration parseExternalLink(List<HierarchicalConfiguration> extLinks,
                                                        String circuitPackName,
                                                        String portName) {
        String nodeId = getDevice().annotations().value(AnnotationKeys.OPENROADM_NODEID);

        HierarchicalConfiguration eLink = null;
        try {
            for (HierarchicalConfiguration extLink : extLinks) {
                String eln =
                        checkNotNull(extLink.getString("external-link-name"));
                String esnid =
                        checkNotNull(extLink.getString("source/node-id"));
                String escpn =
                        checkNotNull(extLink.getString("source/circuit-pack-name"));
                String espn =
                        checkNotNull(extLink.getString("source/port-name"));
                if (nodeId.equals(esnid) && circuitPackName.equals(escpn) &&
                        portName.equals(espn)) {
                    eLink = extLink;
                }
            }
        } catch (NullPointerException e) {
            log.error("[OPENROADM] {} invalid external-links", did());
        }
        return eLink;
    }


    /**
     * Given a degree port (external), return its partner/reverse port.
     *
     * @param degreeNumber Number identifying the current degree.
     * @param portIndex the index of the port (degree branch) for which we are
     *        looking for the reverse port.
     * @param connPorts list of connection-ports as hierarchical configuration.
     * @return the port number for the reverse port.
     */
    protected PortNumber
    findDegreeReversePort(int degreeNumber, int portIndex,
                          List<HierarchicalConfiguration> connPorts) {
        // bidirectional port.
        if (connPorts.size() == 1) {
            return PortNumber.portNumber(0);
        }

        for (HierarchicalConfiguration cp : connPorts) {
            int revPortIndex = cp.getInt("index", -1);
            if (revPortIndex != portIndex) {
                return PortNumber.portNumber(10 * degreeNumber + revPortIndex);
            }
        }
        // We should not reach here
        return PortNumber.portNumber(0);
    }

    /**
     * Creates annotations for the port.
     *
     * @param opticalAnnotations annotations for some optical parameters.
     * @param circuitPackName name of circuit pack containing the port.
     * @param reversepNum the portNumber of the partner port.
     * @param port the hierarchical configuration of the port to parse
     * @param extLink Hierarchical configuration for the external links.
     * @return DefaultAnnotation.
     */
    private DefaultAnnotations
    createPortAnnotations(Map<String, String> opticalAnnotations,
                          String circuitPackName,
                          PortNumber reversepNum,
                          HierarchicalConfiguration port,
                          HierarchicalConfiguration extLink) {
        String nodeId = getDevice().annotations().value(AnnotationKeys.OPENROADM_NODEID);
        Map<String, String> annotations = new HashMap<>();

        annotations.putAll(opticalAnnotations);

        String portName = port.getString("port-name");

        annotations.put(AnnotationKeys.OPENROADM_NODEID, nodeId);
        annotations.put(AnnotationKeys.OPENROADM_CIRCUIT_PACK_NAME, circuitPackName);
        annotations.put(org.onosproject.net.AnnotationKeys.PORT_NAME, portName);
        annotations.put(AnnotationKeys.OPENROADM_PORT_NAME, portName);
        annotations.put(AnnotationKeys.OPENROADM_PARTNER_CIRCUIT_PACK_NAME, port.getString("partner-port/circuit-pack-name", ""));
        annotations.put(AnnotationKeys.OPENROADM_PARTNER_PORT_NAME, port.getString("partner-port/port-name", ""));
        annotations.put(AnnotationKeys.OPENROADM_LOGICAL_CONNECTION_POINT, port.getString("logical-connection-point", ""));
        annotations.put(AnnotationKeys.OPENROADM_PORT_POWER_CAP_MIN_RX, port.getString("roadm-port/port-power-capability-min-rx", ""));
        annotations.put(AnnotationKeys.OPENROADM_PORT_POWER_CAP_MIN_TX, port.getString("roadm-port/port-power-capability-min-tx", ""));
        annotations.put(AnnotationKeys.OPENROADM_PORT_POWER_CAP_MAX_RX, port.getString("roadm-port/port-power-capability-max-rx", ""));
        annotations.put(AnnotationKeys.OPENROADM_PORT_POWER_CAP_MAX_TX, port.getString("roadm-port/port-power-capability-max-tx", ""));

        // Annotate the reverse port, this is needed for bidir intents
        // (Partner port is present in the datastore only for
        // unidirectional ports).
        if (reversepNum.toLong() != 0) {
            annotations.put(OpticalPathIntent.REVERSE_PORT_ANNOTATION_KEY,
                    Long.toString(reversepNum.toLong()));
        }

        // for backwards compatibility
        annotations.put("logical-connection-point",
                port.getString("logical-connection-point", ""));
        // Annotate external link if we found one for this port
        if (extLink != null) {
            String ednid = extLink.getString("destination/node-id");
            String edcpn = extLink.getString("destination/circuit-pack-name");
            String edpn = extLink.getString("destination/port-name");
            annotations.put("openroadm-external-node-id", ednid);
            annotations.put("openroadm-external-circuit-pack-name", edcpn);
            annotations.put("openroadm-external-port-name", edpn);
        }

        //For SEASON fiber ports
        log.info("[SEASON] degree port discovery port-name {}", portName);
        if (portName.contains("SRG")) {
            annotations.put("openroadm-port-type", "if-OCH");
        }

        if (portName.contains("DEG")) {
            annotations.put("openroadm-port-type", "if-OMS");
        }

        //Specific for SEASON testbed
        if (portName.contains("DEG1") || portName.contains("DEG2")) {
            annotations.put("openroadm-port-type", "if-OTS");
        }

        return DefaultAnnotations.builder().putAll(annotations).build();
    }
}
