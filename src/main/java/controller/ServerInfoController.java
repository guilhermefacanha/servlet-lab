package controller;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

@RequestScoped
@Named
public class ServerInfoController implements Serializable {

    private static final long serialVersionUID = 7433315620392638323L;

    public String getServerMacAddress() {
        String serverMacAddress = "";
        String nodeName = StringUtils.defaultIfBlank(System.getenv("WILDFLY_NODE_NAME"), "LOCAL");
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
                    serverMacAddress = nodeName + "-" + macBuilder.toString();
                }
            }
        } catch (SocketException e) {
            serverMacAddress = nodeName + "- N/A (Error)";
        }

        return serverMacAddress;
    }

}
