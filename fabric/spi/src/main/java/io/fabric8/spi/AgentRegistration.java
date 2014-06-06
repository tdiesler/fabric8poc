package io.fabric8.spi;

import io.fabric8.spi.utils.ManagementUtils;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

import javax.management.remote.JMXConnector;

/**
 * An agent registration
 *
 * @author thomas.diesler@jboss.com
 * @since 07-Jun-2014
 */
public final class AgentRegistration {

    private final AgentIdentity identity;
    private final InetAddress targetHost;
    private final String jmxServerUrl;
    private final String jmxUsername;
    private final String jmxPassword;

    public AgentRegistration(AgentIdentity identity, InetAddress targetHost, String jmxServerUrl, String jmxUsername, String jmxPassword) {
        this.identity = identity;
        this.targetHost = targetHost;
        this.jmxServerUrl = jmxServerUrl;
        this.jmxUsername = jmxUsername;
        this.jmxPassword = jmxPassword;
    }

    public AgentIdentity getIdentity() {
        return identity;
    }

    public InetAddress getTargetHost() {
        return targetHost;
    }

    public String getJmxServerUrl() {
        return jmxServerUrl;
    }

    public String getJmxUsername() {
        return jmxUsername;
    }

    public String getJmxPassword() {
        return jmxPassword;
    }

    public JMXConnector getJMXConnector(long timeout, TimeUnit unit) {
        return ManagementUtils.getJMXConnector(jmxServerUrl, jmxUsername, jmxPassword, timeout, unit);
    }

    @Override
    public int hashCode() {
        return identity.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof AgentRegistration)) return false;
        AgentRegistration other = (AgentRegistration) obj;
        return identity.equals(other.identity);
    }

    public String toString() {
        return "AgentRegistration[id=" + identity + ",host=" + targetHost + ",jmx=" + jmxServerUrl + "]";
    }
}