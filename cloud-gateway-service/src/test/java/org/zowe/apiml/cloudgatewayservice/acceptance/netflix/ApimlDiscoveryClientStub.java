/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.acceptance.netflix;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.AbstractDiscoveryClientOptionalArgs;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.Applications;
import org.springframework.cloud.netflix.eureka.CloudEurekaClient;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

public class ApimlDiscoveryClientStub extends CloudEurekaClient {
    private ApplicationRegistry applicationRegistry;

    public ApimlDiscoveryClientStub(ApplicationInfoManager applicationInfoManager, EurekaClientConfig config, AbstractDiscoveryClientOptionalArgs<?> args, ApplicationEventPublisher publisher, ApplicationRegistry applicationRegistry) {
        super(applicationInfoManager, config, args, publisher);

        this.applicationRegistry = applicationRegistry;
    }

    @Override
    public Applications getApplications() {
        if (applicationRegistry != null) {
           return applicationRegistry.getApplications();
        } else {
            return new Applications();
        }
    }

    @Override
    public List<InstanceInfo> getInstancesByVipAddress(String vipAddress, boolean secure) {
        return applicationRegistry.getInstances();
    }

    @Override
    public List<InstanceInfo> getInstancesByVipAddress(String vipAddress, boolean secure, String region) {
        return applicationRegistry.getInstances();
    }
}
