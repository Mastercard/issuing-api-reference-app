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

import com.mastercard.developer.issuing.client.helper.ApiClientHelper;
import com.mastercard.developer.issuing.generated.apis.PrepaidCardIssuanceApi;
import com.mastercard.developer.issuing.generated.invokers.ApiClient;
import com.mastercard.developer.issuing.generated.invokers.ApiException;
import com.mastercard.developer.issuing.generated.models.Mobile;
import com.mastercard.developer.issuing.generated.models.NewCardDetails;
import com.mastercard.developer.issuing.generated.models.NewClient;
import com.mastercard.developer.issuing.generated.models.PrepaidCardApplication;
import com.mastercard.developer.issuing.generated.models.PrepaidCardProfile;
import com.mastercard.developer.issuing.generated.models.Profile;
import lombok.extern.log4j.Log4j2;

/** The Constant log. */
@Log4j2
public class IssuingCardIssuanceService extends IssuingBaseService {

  /** The Constant SERVICE_CONTEXT. */
  private static final String SERVICE_CONTEXT = "/card-issuance";

  /** The Constant PREPAID_CARD_ISSUANCE. */
  private static final String PREPAID_CARD_ISSUANCE = "prepaid-card-issuance";

  /** The scenarios. */
  protected String[] scenarios = {PREPAID_CARD_ISSUANCE};

  /** The api client. */
  private ApiClient apiClient = ApiClientHelper.getApiClient(SERVICE_CONTEXT);

  /**
   * Gets the scenarios.
   *
   * @return the scenarios
   */
  public String[] getScenarios() {
    return scenarios;
  }

  /**
   * Call apis.
   *
   * @param scenarios the scenarios
   */
  public void callApis(String[] scenarios) {
    if (scenarios == null || scenarios.length == 0) {
      scenarios = getScenarios();
    }

    for (String scenario : scenarios) {

      switch (scenario) {
        case PREPAID_CARD_ISSUANCE:
          logScenario(scenario);
          prepaidCardIssuance();
          break;
        default:
          break;
      }
    }
  }

  /** Prepaid card issuance. */
  public PrepaidCardProfile prepaidCardIssuance() {
    PrepaidCardProfile response = null;
    try {
      PrepaidCardIssuanceApi prepaidCardIssuanceApi = new PrepaidCardIssuanceApi(apiClient);

      PrepaidCardApplication request =
          ApiClientHelper.getRequestObject(PREPAID_CARD_ISSUANCE, PrepaidCardApplication.class);

      /** Step 1: Set request validity time */
      request.setDataValidUntilTimestamp(getRequestExpiryTimestamp());

      String xMCCorrelationID = randomUUID();

      String xMCSource = null;

      String xMCClientApplicationUserID = null;

      String xMCIdempotencyKey = randomUUID();

      String xMCBankCode = "323232";

      String alias = String.valueOf(System.currentTimeMillis());
      request.getCard().setAlias(alias);

      response =
          prepaidCardIssuanceApi.createPrepaidCard(
              xMCIdempotencyKey,
              request,
              xMCBankCode,
              xMCCorrelationID,
              xMCSource,
              xMCClientApplicationUserID);

      /** Verify and persist new client and card details */
      Profile profile = request.getClient().getProfile();
      log.info(
          "Requested client details cardAlias={}, FirstName={}, LastName={}, BirthDate={}",
          alias,
          profile.getFirstName(),
          profile.getLastName(),
          profile.getBirthDate());

      if (response.getCards() != null && response.getClient() != null) {
        NewCardDetails newCard = response.getCards().get(0);
        NewClient newClient = response.getClient();
        Mobile mobile = newClient.getMobile();
        String cardID = newCard.getId();
        String cvv = newCard.getCvv();

        log.info(
            "New card generated with cardID={}, cardAlias={}, ISD={}, Mobile={}, cvv={}",
            cardID,
            newCard.getAlias(),
            mobile.getIsd(),
            mobile.getNumber(),
            cvv);
      }

    } catch (ApiException exception) {
      log.error("Exception occurred while calling an API: " + exception.getMessage(), exception);
    }
    return response;
  }

  /**
   * Gets the api client.
   *
   * @return the api client
   */
  public ApiClient getApiClient() {
    return apiClient;
  }

  /**
   * Sets the api client.
   *
   * @param apiClient the new api client
   */
  public void setApiClient(ApiClient apiClient) {
    this.apiClient = apiClient;
  }
}
