/*
 *  Copyright (c) 2022 Mastercard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mastercard.developer.issuing.client.service;

import com.mastercard.developer.exception.ReferenceAppGenericException;
import com.mastercard.developer.issuing.generated.invokers.ApiClient;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;

/** The Constant log. */
@Log4j2
public abstract class IssuingBaseService {

  /**
   * Gets the scenarios.
   *
   * @return the scenarios
   */
  public abstract String[] getScenarios();

  /**
   * Gets the api client.
   *
   * @return the api client
   */
  public abstract ApiClient getApiClient();

  /**
   * Sets the api client.
   *
   * @param apiClient the new api client
   */
  public abstract void setApiClient(ApiClient apiClient);

  /**
   * Contains.
   *
   * @param scenarios the scenarios
   * @return true, if successful
   * @throws Exception the exception
   */
  public boolean contains(String[] scenarios) throws ReferenceAppGenericException {
    if (getScenarios() == null) {
      throw new ReferenceAppGenericException("List Scenarios must be defined");
    }
    boolean present = false;
    if (scenarios != null && scenarios.length > 0) {
      List<String> scenarioList = Arrays.asList(getScenarios());
      for (String scenario : scenarios) {
        if (scenarioList.contains(scenario)) {
          present = true;
          break;
        }
      }
    }
    return present;
  }

  /** Prints the scenarios. */
  public void printScenarios() {
    List<String> scenarioList = Arrays.asList(getScenarios());
    scenarioList.forEach(log::info);
  }

  /**
   * Gets the request validity timestamp.
   *
   * @return the request validity timestamp
   */
  protected OffsetDateTime getRequestExpiryTimestamp() {
    return OffsetDateTime.now().plusSeconds(10);
  }

  /**
   * Random UUID.
   *
   * @return the string
   */
  protected String randomUUID() {
    return UUID.randomUUID().toString();
  }

  /**
   * Log scenario.
   *
   * @param scenario the scenario
   */
  protected void logScenario(String scenario) {
    log.info(
        "\n\n ================================================ Scenario: {} ================================================\n\n",
        scenario);
  }
}
