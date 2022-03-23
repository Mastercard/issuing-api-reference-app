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
import com.mastercard.developer.issuing.client.helper.ApiClientHelper;
import com.mastercard.developer.issuing.client.helper.PinBlockTDEAEncrypter;
import com.mastercard.developer.issuing.generated.apis.AuthenticationTokenApi;
import com.mastercard.developer.issuing.generated.invokers.ApiClient;
import com.mastercard.developer.issuing.generated.models.CardSecret;
import com.mastercard.developer.issuing.generated.models.ClientAuthentication;
import com.mastercard.developer.issuing.generated.models.EncryptedPinBlock;
import com.mastercard.developer.issuing.generated.models.TokenDetails;
import lombok.extern.log4j.Log4j2;

/** The Constant log. */
@Log4j2
public class IssuingAuthorizationManagementService extends IssuingBaseService {

  /** The Constant SERVICE_CONTEXT. */
  private static final String SERVICE_CONTEXT = "/authorization-management";

  /** The Constant AUTHENTICATION_TOKEN. */
  private static final String AUTHENTICATION_TOKEN = "authentication-token";

  /** The scenarios. */
  private static final String[] SCENARIOS = {AUTHENTICATION_TOKEN};

  /** The api client. */
  private ApiClient apiClient = ApiClientHelper.getApiClient(SERVICE_CONTEXT);

  /**
   * Gets the scenarios.
   *
   * @return the scenarios
   */
  public String[] getScenarios() {
    return SCENARIOS;
  }

  /**
   * Call apis.
   *
   * @param scenarios the scenarios
   * @throws Exception the exception
   */
  public void callApis(String[] scenarios) throws ReferenceAppGenericException {

    if (scenarios == null || scenarios.length == 0) {
      scenarios = getScenarios();
    }
    for (String scenario : scenarios) {

      switch (scenario) {
        case AUTHENTICATION_TOKEN:
          logScenario(scenario);
          createToken("PIN_RESET");
          break;
        default:
          break;
      }
    }
  }

  /**
   * Creates the token.
   *
   * @param intent the intent
   * @return the string
   * @throws Exception the exception
   */
  public TokenDetails createToken(String intent) throws ReferenceAppGenericException {
    String token = null;
    TokenDetails response = null;
    try {
      AuthenticationTokenApi authenticationTokenApi = new AuthenticationTokenApi(apiClient);

      String cardId = ApiClientHelper.getRequestObject("card-id", String.class);
      ClientAuthentication request =
          ApiClientHelper.getRequestObject(AUTHENTICATION_TOKEN, ClientAuthentication.class);

      /** Update intent */
      request.setIntent(intent);

      if (!intent.equalsIgnoreCase("PIN_RESET")) {
        String clearPin = "1234";
        PinBlockTDEAEncrypter pinBlockTDEAEncrypter = PinBlockTDEAEncrypter.getInstance();
        EncryptedPinBlock encryptedPinBlock = pinBlockTDEAEncrypter.encryptPin(clearPin, cardId);
        CardSecret cardSecret = new CardSecret();
        cardSecret.setPin(encryptedPinBlock);
        request.setCard(cardSecret);

        // Remove client attributes
        request.setClient(null);
      }

      /** Step 1: Set request validity time */
      request.setDataValidUntilTimestamp(getRequestExpiryTimestamp());

      String xMCCorrelationID = randomUUID();

      /** values */
      String xMCSource = null;

      String xMCClientApplicationUserID = null;

      response =
          authenticationTokenApi.createToken(
              cardId, request, xMCCorrelationID, xMCSource, xMCClientApplicationUserID);
      token = response.getToken();

      log.info("Received token = {}", token);

    } catch (Exception exception) {
      log.error("Exception occurred while calling an API: " + exception.getMessage(), exception);
      throw new ReferenceAppGenericException(
          "Exception occurred while calling an API: ", exception);
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
