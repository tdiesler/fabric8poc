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

import io.fabric8.core.utils.SimplePathTemplate;

import java.util.Map;
import java.util.regex.Pattern;

public enum ZkPath {

    //Container Nodes
    CONTAINERS("/fabric/registry/containers"),
    CONTAINER("/fabric/registry/containers/{container}"),

    CONTAINER_TYPE("/fabric/registry/containers/{container}/type"),
    CONTAINER_PARENT("/fabric/registry/containers/{container}/parent"),
    CONTAINER_CHILDREN("/fabric/registry/containers/{container}/children"),
    CONTAINER_STATE("/fabric/registry/containers/{container}/state"),
    CONTAINER_CONNECTED("/fabric/registry/containers/{container}/connected"),
    CONTAINER_PROCESS_ID("/fabric/registry/containers/{container}/pid"),

    CONTAINER_DOMAINS("/fabric/registry/containers/{container}/domains"),
    CONTAINER_DOMAIN("/fabric/registry/containers/{container}/domains/{domain}"),

    CONTAINER_PROVISION("/fabric/registry/containers/{container}/provision"),
    CONTAINER_PROVISION_LIST("/fabric/registry/containers/{container}/provision/list"),
    CONTAINER_PROVISION_CHECKSUMS("/fabric/registry/containers/{container}/provision/checksums"),
    CONTAINER_PROVISION_RESULT("/fabric/registry/containers/{container}/provision/result"),
    CONTAINER_PROVISION_EXCEPTION("/fabric/registry/containers/{container}/provision/exception"),
    CONTAINER_PROVISION_EXTENDER("/fabric/registry/containers/{container}/provision/extender/{extender}"),
    CONTAINER_PROVISION_EXTENDER_BUNDLE("/fabric/registry/containers/{container}/provision/extender/{extender}/bundle/{bundle}"),
    CONTAINER_PROVISION_EXTENDER_STATUS("/fabric/registry/containers/{container}/provision/extender/{extender}/status"),


    CONTAINER_CONFIG("/fabric/registry/containers/{container}/config"),
    CONTAINER_CONFIG_VERSION("/fabric/registry/containers/{container}/config/version"),
    CONTAINER_CONFIG_PROFILES("/fabric/registry/containers/{container}/config/profiles"),
    CONTAINER_CONFIG_CREATE_METADATA("/fabric/registry/containers/{container}/config/metadata"),
    CONTAINER_CONFIG_CREATE_OPTIONS("/fabric/registry/containers/{container}/config/metadata"),

    CONTAINER_ATTRIBUTES("/fabric/registry/containers/{container}/attributes"),
    CONTAINER_ATTRIBUTE("/fabric/registry/containers/{container}/attributes/{attribute}"),
    CONTAINER_ATTRIBUTE_IP("/fabric/registry/containers/{container}/attributes/ip"),
    CONTAINER_ATTRIBUTE_RESOLVER("/fabric/registry/containers/{container}/attributes/resolver"),
    CONTAINER_ATTRIBUTE_ADDRESS("/fabric/registry/containers/{container}/attributes/{type}"),
    CONTAINER_ATTRIBUTE_LOCAL_IP("/fabric/registry/containers/{container}/attributes/localip"),
    CONTAINER_ATTRIBUTE_LOCAL_HOSTNAME("/fabric/registry/containers/{container}/attributes/localhostname"),
    CONTAINER_ATTRIBUTE_PUBLIC_IP("/fabric/registry/containers/{container}/attributes/publicip"),
    CONTAINER_ATTRIBUTE_PUBLIC_HOSTNAME("/fabric/registry/containers/{container}/attributes/publichostname"),
    CONTAINER_ATTRIBUTE_MANUAL_IP("/fabric/registry/containers/{container}/attributes/manualip"),
    CONTAINER_ATTRIBUTE_BINDADDRESS("/fabric/registry/containers/{container}/attributes/bindaddress"),
    CONTAINER_ATTRIBUTE_JMX("/fabric/registry/containers/{container}/attributes/jmx"),
    CONTAINER_ATTRIBUTE_JOLOKIA("/fabric/registry/containers/{container}/attributes/jolokia"),
    CONTAINER_ATTRIBUTE_SSH("/fabric/registry/containers/{container}/attributes/ssh"),
    CONTAINER_ATTRIBUTE_HTTP("/fabric/registry/containers/{container}/attributes/http"),
    CONTAINER_ATTRIBUTE_LOCATION("/fabric/registry/containers/{container}/attributes/loc"),
    CONTAINER_ATTRIBUTE_GEOLOCATION("/fabric/registry/containers/{container}/attributes/geoloc"),

    CONTAINER_ENDPOINTS("/fabric/registry/containers/{container}/endpoints"),
    CONTAINER_ENDPOINT("/fabric/registry/containers/{container}/endpoints/{endpoint}");




    private static final Pattern ENSEMBLE_PROFILE_PATTERN = Pattern.compile("fabric-ensemble-[0-9]+|fabric-ensemble-[0-9]+-[0-9]+");

    /**
     * Path template.
     */
    private SimplePathTemplate path;

    private ZkPath(String path) {
        this.path = new SimplePathTemplate(path);
    }

    /**
     * Gets path.
     *
     * @param args Values of path variables.
     * @return Path
     */
    public String getPath(String... args) {
        return this.path.bindByPosition(args);
    }


    /**
     * Gets path.
     *
     * @param args Values of path variables.
     * @return Path
     */
    public String getPath(Map<String, String> args) {
        return this.path.bindByName(args);
    }
}

