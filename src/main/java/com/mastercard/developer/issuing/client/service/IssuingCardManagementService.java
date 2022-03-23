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
import com.mastercard.developer.issuing.generated.apis.GetCardApi;
import com.mastercard.developer.issuing.generated.apis.GetClientApi;
import com.mastercard.developer.issuing.generated.apis.SearchCardsApi;
import com.mastercard.developer.issuing.generated.apis.UpdateClientApi;
import com.mastercard.developer.issuing.generated.apis.UpdatePinApi;
import com.mastercard.developer.issuing.generated.invokers.ApiClient;
import com.mastercard.developer.issuing.generated.invokers.ApiException;
import com.mastercard.developer.issuing.generated.models.CardProfile;
import com.mastercard.developer.issuing.generated.models.CardSearchCriteria;
import com.mastercard.developer.issuing.generated.models.CardSearchResult;
import com.mastercard.developer.issuing.generated.models.ClientProfile;
import com.mastercard.developer.issuing.generated.models.NewPinDetails;
import com.mastercard.developer.issuing.generated.models.ServiceRequestDetails;
import com.mastercard.developer.issuing.generated.models.UpdatedClient;
import java.time.LocalDate;
import lombok.extern.log4j.Log4j2;

/** The Constant log. */
@Log4j2
public class IssuingCardManagementService extends IssuingBaseService {

  /** The Constant SERVICE_CONTEXT. */
  private static final String SERVICE_CONTEXT = "/card-management";

  /** The Constant SEARCH_CARDS. */
  private static final String SEARCH_CARDS = "search-cards";

  /** The Constant UPDATE_PIN. */
  private static final String UPDATE_PIN = "update-pin";

  /** The Constant UPDATE_CLIENT. */
  private static final String UPDATE_CLIENT = "update-client";

  /** The Constant GET_CARD. */
  private static final String GET_CARD = "get-card";

  /** The Constant GET_CLIENT. */
  private static final String GET_CLIENT = "get-client";

  /** The scenarios. */
  protected String[] scenarios = {SEARCH_CARDS, GET_CARD, UPDATE_PIN, GET_CLIENT, UPDATE_CLIENT};

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
        case SEARCH_CARDS:
          logScenario(scenario);
          searchCard();
          break;
        case GET_CARD:
          logScenario(scenario);
          getCard();
          break;
        case UPDATE_PIN:
          logScenario(scenario);
          updatePin();
          break;
        case GET_CLIENT:
          logScenario(scenario);
          getClient();
          break;
        case UPDATE_CLIENT:
          logScenario(scenario);
          updateClient();
          break;
        default:
          break;
      }
    }
  }

  /** Search card. */
  public CardSearchResult searchCard() {
    CardSearchResult response = null;
    try {
      SearchCardsApi searchCardsApi = new SearchCardsApi(apiClient);

      CardSearchCriteria request =
          ApiClientHelper.getRequestObject(SEARCH_CARDS, CardSearchCriteria.class);

      /** Step 1: Set request validity time */
      request.setDataValidUntilTimestamp(getRequestExpiryTimestamp());

      String xMCCorrelationID = randomUUID();

      String xMCSource = null;

      String xMCClientApplicationUserID = null;

      response =
          searchCardsApi.searchCards(
              request, xMCCorrelationID, xMCSource, xMCClientApplicationUserID);

      if (response.getCards() != null) {
        log.info("card alias={}", response.getCards().get(0).getAlias());
      }
    } catch (ApiException exception) {
      log.error(
          "Exception occurred while calling searchCard API: " + exception.getMessage(), exception);
    }
    return response;
  }

  /**
   * Gets the card.
   *
   * @return the card
   */
  public CardProfile getCard() {
    CardProfile response = null;
    try {
      GetCardApi getCardApi = new GetCardApi(apiClient);

      String cardId = ApiClientHelper.getRequestObject("card-id", String.class);

      String xMCCorrelationID = randomUUID();

      String xMCSource = null;

      String xMCClientApplicationUserID = null;

      String fields = null; // remove response filter

      response =
          getCardApi.getCard(
              cardId, xMCCorrelationID, xMCSource, xMCClientApplicationUserID, fields);

      if (response.getCard() != null && response.getClients() != null) {
        log.info(
            "card ID={} belongs is linked to client Code={} ",
            response.getCard().getId(),
            response.getClients().get(0).getCode());
      }

    } catch (ApiException exception) {
      log.error(
          "Exception occurred while calling getCard API: " + exception.getMessage(), exception);
    }
    return response;
  }

  /** Update pin. */
  public void updatePin() {
    try {
      UpdatePinApi updatePinApi = new UpdatePinApi(apiClient);

      String cardId = ApiClientHelper.getRequestObject("card-id", String.class);
      NewPinDetails request = ApiClientHelper.getRequestObject(UPDATE_PIN, NewPinDetails.class);

      /** Step 1: Set request validity time */
      request.setDataValidUntilTimestamp(getRequestExpiryTimestamp());

      String xMCCorrelationID = randomUUID();

      String xMCSource = null;

      String xMCClientApplicationUserID = null;

      updatePinApi.updatePin(
          cardId, request, xMCCorrelationID, xMCSource, xMCClientApplicationUserID);

    } catch (ApiException exception) {
      log.error(
          "Exception occurred while calling updatePin API: " + exception.getMessage(), exception);
    }
  }

  /**
   * Gets the client.
   *
   * @return the client
   */
  public ClientProfile getClient() {
    ClientProfile response = null;
    try {
      GetClientApi getClientApi = new GetClientApi(apiClient);

      String clientCode = ApiClientHelper.getRequestObject("client-code", String.class);

      String xMCCorrelationID = randomUUID();

      String xMCSource = null;

      String xMCClientApplicationUserID = null;

      String fields = null;
      response =
          getClientApi.getClient(
              clientCode, xMCCorrelationID, xMCSource, xMCClientApplicationUserID, fields);
      if (response.getClient() != null) {
        log.debug("getClient response clientCode={} ", response.getClient().getCode());
      }
    } catch (ApiException exception) {
      log.error(
          "Exception occurred while calling getClient API: " + exception.getMessage(), exception);
    }
    return response;
  }

  /** Update client. */
  public ServiceRequestDetails updateClient() {
    ServiceRequestDetails response = null;
    try {
      UpdateClientApi updateClientApi = new UpdateClientApi(apiClient);

      String clientCode = ApiClientHelper.getRequestObject("client-code", String.class);
      UpdatedClient request = ApiClientHelper.getRequestObject(UPDATE_CLIENT, UpdatedClient.class);

      request.setDataValidUntilTimestamp(getRequestExpiryTimestamp());

      String xMCCorrelationID = randomUUID();

      String xMCSource = null;

      String xMCClientApplicationUserID = null;

      /** Trying to dummy first name */
      request.getClient().getProfile().setFirstName("Johnny");

      /** Trying to set dummy date of birth to today minus 20 years */
      request.getClient().getProfile().setBirthDate(LocalDate.now().minusYears(20));

      response =
          updateClientApi.updateClient(
              clientCode, request, xMCCorrelationID, xMCSource, xMCClientApplicationUserID);

      if (response.getServiceRequests() != null) {
        log.debug(
            "getClient response ServiceRequest number={} ",
            response.getServiceRequests().get(0).getNumber());
      }
    } catch (ApiException exception) {
      log.error(
          "Exception occurred while calling updateClient API: " + exception.getMessage(),
          exception);
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
