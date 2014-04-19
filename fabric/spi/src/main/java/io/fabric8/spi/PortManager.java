package io.fabric8.spi;

import java.net.InetAddress;


/**
 * A host wide port manager
 *
 * @author thomas.diesler@jboss.com
 * @since 18-Apr-2014
 */
public interface PortManager {

    int nextAvailablePort(int portValue, InetAddress bindAddr);
}