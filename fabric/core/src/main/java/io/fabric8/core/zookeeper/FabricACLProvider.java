/*
 * Copyright (C) 2010 - 2014 JBoss by Red Hat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 */
package io.fabric8.core.zookeeper;

import io.fabric8.spi.scr.AbstractComponent;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Service;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

//@Component(name = "io.fabric8.zookeeper.acl", policy = ConfigurationPolicy.OPTIONAL, immediate = true)
//@Service({ACLProvider.class })
public class FabricACLProvider extends AbstractComponent implements ACLProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(FabricACLProvider.class);
    private final ConcurrentMap<String, String> acls = new ConcurrentHashMap<String, String>();

    public FabricACLProvider() {
        acls.put("/", "world:anyone:acdrw");
        acls.put("/fabric", "auth::acdrw,world:anyone:");
    }

    @Activate
    void activate(Map<String, ?> configuration) {
        updateInternal(configuration);
        activateComponent();
    }

    @Modified
    void modified(Map<String, ?> configuration) {
        updateInternal(configuration);
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    private void updateInternal(Map<String, ?> configuration) {
        for (Map.Entry<String, ?> entry : configuration.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("acls.")) {
                String value = String.valueOf(entry.getValue());
                acls.put(key.substring("acls.".length()), value);
            }
        }
    }

    @Override
    public List<ACL> getDefaultAcl() {
        //assertValid();
        return getAclForPath("/");
    }

    @Override
    public List<ACL> getAclForPath(String path) {
        //assertValid();
        String acl = findNodeAcls(adjustPath(path));
        if (acl == null) {
            throw new IllegalStateException("Could not find matching ACLs for " + path);
        }
        return parseACLs(acl);
    }

    /**
     * Returns the ACL string for the specified path.
     */
    private String findNodeAcls(String path) {
        String longestPath = "";
        for (String acl : acls.keySet()) {
            if (acl.length() > longestPath.length() && path.startsWith(acl)) {
                longestPath = acl;
            }
        }
        return acls.get(longestPath);
    }

    /**
     * Normalizes the specified path, by removing trailing slashes and adding leading ones if needed.
     */
    private String adjustPath(String path) {
        if (path == null) {
            throw new IllegalArgumentException();
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return path;
    }

    /**
     * Parses a {@link String} representation of the {@link org.apache.zookeeper.data.ACL} list.
     */
    private List<ACL> parseACLs(String aclString) {
        List<ACL> acl;
        String acls[] = aclString.split(",");
        acl = new ArrayList<ACL>();
        for (String a : acls) {
            int firstColon = a.indexOf(':');
            int lastColon = a.lastIndexOf(':');
            if (firstColon == -1 || lastColon == -1 || firstColon == lastColon) {
                LOGGER.warn(a + " does not have the form scheme:id:perm");
                continue;
            }
            ACL newAcl = new ACL();
            newAcl.setId(new Id(a.substring(0, firstColon), a.substring(firstColon + 1, lastColon)));
            newAcl.setPerms(getPermFromString(a.substring(lastColon + 1)));
            acl.add(newAcl);
        }
        return acl;
    }

    /**
     * Returns the int value of the permission {@link String}.
     */
    private int getPermFromString(String permString) {
        int perm = 0;
        for (int i = 0; i < permString.length(); i++) {
            switch (permString.charAt(i)) {
            case 'r':
                perm |= ZooDefs.Perms.READ;
                break;
            case 'w':
                perm |= ZooDefs.Perms.WRITE;
                break;
            case 'c':
                perm |= ZooDefs.Perms.CREATE;
                break;
            case 'd':
                perm |= ZooDefs.Perms.DELETE;
                break;
            case 'a':
                perm |= ZooDefs.Perms.ADMIN;
                break;
            default:
                LOGGER.warn("Unknown perm type: " + permString.charAt(i));
            }
        }
        return perm;
    }
}
