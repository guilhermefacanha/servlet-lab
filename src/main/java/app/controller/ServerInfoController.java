package app.controller;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

@Slf4j
@RequestScoped
@Named
public class ServerInfoController implements Serializable {

    private static final long serialVersionUID = 7433315620392638323L;

    public String getServerMacAddress() {
        String serverMacAddress = "";
        log.info(" Retrieving server information ...");
        String nodeName = StringUtils.defaultIfBlank(System.getenv("WILDFLY_NODE_NAME"), "");
        log.info(" WILDFLY_NODE_NAME: {}", nodeName);
        if (StringUtils.isBlank(nodeName)) {
            nodeName = StringUtils.defaultIfBlank(System.getProperty("jboss.node.name"), StringUtils.defaultIfBlank(System.getProperty("jboss.tx.node.id"), "LOCAL"));
            log.info(" jboss node: {}", nodeName);
        }
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();
                // Skip loopback, virtual, and down interfaces
                if (ni.isLoopback() || ni.isVirtual() || !ni.isUp()) {
                    continue;
                }
                byte[] hardwareAddress = ni.getHardwareAddress();
                if (hardwareAddress != null) {
                    StringBuilder macBuilder = new StringBuilder();
                    for (int i = 0; i < hardwareAddress.length; i++) {
                        macBuilder.append(String.format("%02X%s", hardwareAddress[i], (i < hardwareAddress.length - 1) ? ":" : ""));
                    }
                    // Return the first valid MAC address found
                    serverMacAddress = nodeName + " - " + macBuilder.toString();
                }
            }
        } catch (SocketException e) {
            serverMacAddress = nodeName + "- N/A (Error)";
        }

        return serverMacAddress;
    }

}
