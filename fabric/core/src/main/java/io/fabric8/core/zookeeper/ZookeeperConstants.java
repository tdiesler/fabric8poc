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

public interface ZookeeperConstants {

    String ZOOKEEPER_PID = "io.fabric8.zookeeper";
    String ZOOKEEPER_SERVER_PID = "io.fabric8.zookeeper.server";
    String ZOOKEEPER_ACL_PID = "io.fabric8.zookeeper.acl";

    String ZOOKEEPER_URL = "zookeeper.url";
    String ZOOKEEPER_PASSWORD = "zookeeper.password";
    String ENSEMBLE_ID = "ensemble.id";

    String SESSION_TIMEOUT = "sessionTimeOutMs";
    String CONNECTION_TIMEOUT = "connectionTimeOutMs";

    String RETRY_POLICY_MAX_RETRIES = "retryPolicy.maxRetries";
    String RETRY_POLICY_INTERVAL_MS = "retryPolicy.retryIntervalMs";

    int DEFAULT_CONNECTION_TIMEOUT_MS = 15000;
    int DEFAULT_SESSION_TIMEOUT_MS = 60000;
    int MAX_RETRIES_LIMIT = 3;
    int DEFAULT_RETRY_INTERVAL = 500;
}
