/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.container.karaf;

import io.fabric8.api.Constants;
import io.fabric8.spi.AbstractManagedContainer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.management.remote.JMXConnector;

import org.jboss.gravia.runtime.RuntimeType;

/**
 * The Karaf managed container
 *
 * @since 26-Feb-2014
 */
public class KarafManagedContainer extends AbstractManagedContainer<KarafCreateOptions> {

    KarafManagedContainer(KarafCreateOptions options) {
        super(options);
    }

    @Override
    protected void doConfigure() throws Exception {
        File karafHome = getContainerHome();
        String comment = "Modified by " + getClass().getName();

        // etc/org.ops4j.pax.web.cfg
        File paxwebFile = new File(karafHome, "etc/org.ops4j.pax.web.cfg");
        if (paxwebFile.exists()) {
            Properties props = new Properties();
            props.load(new FileReader(paxwebFile));
            String propertyKey = "org.osgi.service.http.port";
            int confHttpPort = Integer.parseInt((String) props.get(propertyKey));
            int httpPort = freePortValue(confHttpPort);
            if (confHttpPort != httpPort) {
                props.setProperty(propertyKey, new Integer(httpPort).toString());
                FileWriter fileWriter = new FileWriter(paxwebFile);
                try {
                    props.store(fileWriter, comment);
                } finally {
                    fileWriter.close();
                }
            }
            putAttribute(Constants.ATTRIBUTE_KEY_HTTP_PORT, httpPort);
        }

        // etc/org.apache.karaf.management.cfg
        File managementFile = new File(karafHome, "etc/org.apache.karaf.management.cfg");
        if (managementFile.exists()) {
            Properties props = new Properties();
            props.load(new FileReader(managementFile));
            int confRegistryPort = Integer.parseInt((String) props.get("rmiRegistryPort"));
            int registryPort = freePortValue(confRegistryPort);
            int confServerPort = Integer.parseInt((String) props.get("rmiServerPort"));
            int serverPort = freePortValue(confServerPort);
            if (confRegistryPort != registryPort || confServerPort != serverPort) {
                props.setProperty("rmiRegistryPort", new Integer(registryPort).toString());
                props.setProperty("rmiServerPort", new Integer(serverPort).toString());
                FileWriter fileWriter = new FileWriter(managementFile);
                try {
                    props.store(fileWriter, comment);
                } finally {
                    fileWriter.close();
                }
            }
            String jmxServerURL = "service:jmx:rmi://127.0.0.1:" + serverPort + "/jndi/rmi://127.0.0.1:" + registryPort + "/karaf-root";
            putAttribute(Constants.ATTRIBUTE_KEY_JMX_SERVER_URL, jmxServerURL);
        }
    }

    @Override
    protected void doStart() throws Exception {

        File karafHome = getContainerHome();
        if (!karafHome.isDirectory())
            throw new IllegalStateException("Not a valid Karaf home dir: " + karafHome);

        List<String> cmd = new ArrayList<String>();
        cmd.add("java");

        // JavaVM args
        String javaArgs = getCreateOptions().getJavaVmArguments();
        cmd.addAll(Arrays.asList(javaArgs.split("\\s+")));

        // Karaf properties
        cmd.add("-Dkaraf.home=" + karafHome);
        cmd.add("-Dkaraf.base=" + karafHome);
        cmd.add("-Dkaraf.etc=" + karafHome + "/etc");
        cmd.add("-Dkaraf.data=" + karafHome + "/data");
        cmd.add("-Dkaraf.instances=" + karafHome + "/instances");
        cmd.add("-Dkaraf.startLocalConsole=false");
        cmd.add("-Dkaraf.startRemoteShell=false");

        // Java properties
        cmd.add("-Djava.io.tmpdir=" + new File(karafHome, "data/tmp"));
        cmd.add("-Djava.util.logging.config.file=" + new File(karafHome, "etc/java.util.logging.properties"));
        cmd.add("-Djava.endorsed.dirs=" + new File(karafHome, "lib/endorsed"));

        // Classpath
        StringBuffer classPath = new StringBuffer();
        File karafLibDir = new File(karafHome, "lib");
        String[] libs = karafLibDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith("karaf");
            }
        });
        for (String lib : libs) {
            String separator = classPath.length() > 0 ? File.pathSeparator : "";
            classPath.append(separator + new File(karafHome, "lib/" + lib));
        }
        cmd.add("-classpath");
        cmd.add(classPath.toString());

        // Main class
        cmd.add("org.apache.karaf.main.Main");

        // Output the startup command
        StringBuffer cmdstr = new StringBuffer();
        for (String tok : cmd) {
            cmdstr.append(tok + " ");
        }

        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        processBuilder.directory(karafHome);
        processBuilder.redirectErrorStream(true);
        startProcess(processBuilder);
    }

    @Override
    public JMXConnector getJMXConnector(Map<String, Object> env, long timeout, TimeUnit unit) {
        JMXConnector connector;
        if (RuntimeType.getRuntimeType() == RuntimeType.WILDFLY) {
            ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(null);
                connector = super.getJMXConnector(env, timeout, unit);
            } finally {
                Thread.currentThread().setContextClassLoader(contextLoader);
            }
        } else {
            connector = super.getJMXConnector(env, timeout, unit);
        }
        return connector;
    }
}
