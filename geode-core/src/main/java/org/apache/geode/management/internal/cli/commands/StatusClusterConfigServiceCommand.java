/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.management.internal.cli.commands;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.shell.core.annotation.CliCommand;

import org.apache.geode.cache.execute.ResultCollector;
import org.apache.geode.distributed.DistributedMember;
import org.apache.geode.internal.cache.InternalCache;
import org.apache.geode.management.cli.CliMetaData;
import org.apache.geode.management.cli.Result;
import org.apache.geode.management.cli.Result.Status;
import org.apache.geode.management.internal.cli.CliUtil;
import org.apache.geode.management.internal.cli.functions.CliFunctionResult;
import org.apache.geode.management.internal.cli.functions.FetchSharedConfigurationStatusFunction;
import org.apache.geode.management.internal.cli.i18n.CliStrings;
import org.apache.geode.management.internal.cli.result.ResultBuilder;
import org.apache.geode.management.internal.cli.result.TabularResultData;
import org.apache.geode.management.internal.configuration.domain.SharedConfigurationStatus;
import org.apache.geode.management.internal.security.ResourceOperation;
import org.apache.geode.security.ResourcePermission.Operation;
import org.apache.geode.security.ResourcePermission.Resource;

public class StatusClusterConfigServiceCommand extends InternalGfshCommand {
  private static final FetchSharedConfigurationStatusFunction fetchSharedConfigStatusFunction =
      new FetchSharedConfigurationStatusFunction();

  @SuppressWarnings("unchecked")
  @CliCommand(value = CliStrings.STATUS_SHARED_CONFIG, help = CliStrings.STATUS_SHARED_CONFIG_HELP)
  @CliMetaData(relatedTopic = CliStrings.TOPIC_GEODE_LOCATOR)
  @ResourceOperation(resource = Resource.CLUSTER, operation = Operation.READ)
  public Result statusSharedConfiguration() {
    final InternalCache cache = (InternalCache) getCache();
    final Set<DistributedMember> locators = new HashSet<>(
        cache.getDistributionManager().getAllHostedLocatorsWithSharedConfiguration().keySet());
    if (locators.isEmpty()) {
      return ResultBuilder.createInfoResult(CliStrings.NO_LOCATORS_WITH_SHARED_CONFIG);
    } else {
      return ResultBuilder.buildResult(getSharedConfigurationStatus(locators));
    }
  }

  private TabularResultData getSharedConfigurationStatus(Set<DistributedMember> locators) {
    boolean isSharedConfigRunning = false;
    ResultCollector<?, ?> rc =
        CliUtil.executeFunction(fetchSharedConfigStatusFunction, null, locators);
    List<CliFunctionResult> results = (List<CliFunctionResult>) rc.getResult();
    TabularResultData table = ResultBuilder.createTabularResultData();
    table.setHeader("Status of shared configuration on locators");

    for (CliFunctionResult result : results) {
      table.accumulate(CliStrings.STATUS_SHARED_CONFIG_NAME_HEADER, result.getMemberIdOrName());
      String status = (String) result.getSerializables()[0];
      table.accumulate(CliStrings.STATUS_SHARED_CONFIG_STATUS, status);
      if (SharedConfigurationStatus.RUNNING.name().equals(status)) {
        isSharedConfigRunning = true;
      }
    }

    if (!isSharedConfigRunning) {
      table.setStatus(Status.ERROR);
    }
    return table;
  }
}
