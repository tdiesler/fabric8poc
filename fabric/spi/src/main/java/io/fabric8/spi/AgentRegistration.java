package io.fabric8.spi;

import java.beans.ConstructorProperties;

import org.jboss.gravia.utils.IllegalArgumentAssertion;

/**
 * An agent registration
 *
 * @author thomas.diesler@jboss.com
 * @since 07-Jun-2014
 *
 * @Immutable
 */
public final class AgentRegistration {

    private final AgentIdentity identity;
    private final String runtimeType;
    private final String targetHost;
    private final String jmxEndpoint;
    private final String jolokiaEndpoint;

    @ConstructorProperties( { "identity", "runtimeType", "targetHost", "jmxEndpoint", "jolokiaEndpoint" } )
    public AgentRegistration(AgentIdentity identity, String runtimeType, String targetHost, String jmxEndpoint, String jolokiaEndpoint) {
        IllegalArgumentAssertion.assertNotNull(identity, "identity");
        IllegalArgumentAssertion.assertNotNull(runtimeType, "runtimeType");
        IllegalArgumentAssertion.assertNotNull(targetHost, "targetHost");
        IllegalArgumentAssertion.assertNotNull(jmxEndpoint, "jmxEndpoint");
        IllegalArgumentAssertion.assertNotNull(jolokiaEndpoint, "jolokiaEndpoint");
        this.identity = identity;
        this.runtimeType = runtimeType;
        this.targetHost = targetHost;
        this.jmxEndpoint = jmxEndpoint;
        this.jolokiaEndpoint = jolokiaEndpoint;
    }

    public AgentIdentity getIdentity() {
        return identity;
    }

    public String getRuntimeType() {
        return runtimeType;
    }

    public String getTargetHost() {
        return targetHost;
    }

    public String getJmxEndpoint() {
        return jmxEndpoint;
    }

    public String getJolokiaEndpoint() {
        return jolokiaEndpoint;
    }

    @Override
    public int hashCode() {
        return identity.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj == this)return true;
        if (!(obj instanceof AgentRegistration)) return false;
        AgentRegistration other = (AgentRegistration) obj;
        return identity.equals(other.identity);
    }

    public String toString() {
        return "AgentRegistration[id=" + identity + ",type=" + runtimeType + ",host=" + targetHost + ",jmx=" + jmxEndpoint + ",jolokia=" + jolokiaEndpoint + "]";
    }
}