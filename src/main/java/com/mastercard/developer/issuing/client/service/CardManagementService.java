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

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.LocalDate;

import com.mastercard.developer.issuing.client.helper.ApiClientHelper;
import com.mastercard.developer.issuing.client.helper.PinBlockTDEAEncrypter;
import com.mastercard.developer.issuing.client.helper.RequestContext;
import com.mastercard.developer.issuing.exception.ReferenceAppGenericException;
import com.mastercard.developer.issuing.generated.apis.CardApi;
import com.mastercard.developer.issuing.generated.apis.ClientApi;
import com.mastercard.developer.issuing.generated.invokers.ApiClient;
import com.mastercard.developer.issuing.generated.invokers.ApiException;
import com.mastercard.developer.issuing.generated.models.CardProfile;
import com.mastercard.developer.issuing.generated.models.CardSearchCriteria;
import com.mastercard.developer.issuing.generated.models.CardSearchResult;
import com.mastercard.developer.issuing.generated.models.ClientProfile;
import com.mastercard.developer.issuing.generated.models.EncryptedPinBlock;
import com.mastercard.developer.issuing.generated.models.NewPinDetails;
import com.mastercard.developer.issuing.generated.models.ServiceRequestDetails;
import com.mastercard.developer.issuing.generated.models.TokenDetails;
import com.mastercard.developer.issuing.generated.models.UpdatedClient;

import lombok.extern.log4j.Log4j2;

/** The Constant log. */
@Log4j2
public class CardManagementService extends BaseService {

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
    protected String[] scenarios = { SEARCH_CARDS, GET_CARD, UPDATE_PIN, GET_CLIENT, UPDATE_CLIENT };

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
     * @throws GeneralSecurityException
     * @throws ReferenceAppGenericException
     */
    public void callApis(String[] scenarios) throws ReferenceAppGenericException, GeneralSecurityException {

        if (scenarios == null || scenarios.length == 0) {
            scenarios = getScenarios();
        }

        for (String scenario : scenarios) {
            switch (scenario) {
            case SEARCH_CARDS:
                logScenario(scenario);
                CardSearchResult cardSearchResult = searchCard();
                ApiClientHelper.saveResponseObject(scenario, cardSearchResult);
                break;
            case GET_CARD:
                logScenario(scenario);
                CardProfile cardProfile = getCard();
                ApiClientHelper.saveResponseObject(scenario, cardProfile);
                break;
            case UPDATE_PIN:
                logScenario(scenario);
                Boolean success = updatePin();
                ApiClientHelper.saveResponseObject(scenario, success);
                break;
            case GET_CLIENT:
                logScenario(scenario);
                ClientProfile clientProfile = getClient();
                ApiClientHelper.saveResponseObject(scenario, clientProfile);
                break;
            case UPDATE_CLIENT:
                logScenario(scenario);
                ServiceRequestDetails serviceRequestDetails = updateClient();
                ApiClientHelper.saveResponseObject(scenario, serviceRequestDetails);

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
            CardApi cardApi = new CardApi(apiClient);

            CardSearchCriteria request = ApiClientHelper.getRequestObject(SEARCH_CARDS, CardSearchCriteria.class);

            /** Step 1: Set request validity time */
            request.setDataValidUntilTimestamp(getRequestExpiryTimestamp());

            /** Set request header values */
            String xMCCorrelationID = randomUUID();
            String xMCBankCode = null;
            String xMCSource = null;
            String xMCClientApplicationUserID = null;

            response = cardApi.searchCards(request, xMCBankCode, xMCCorrelationID, xMCSource, xMCClientApplicationUserID);

            if (response.getCards() != null) {
                log.info("card alias={}", response.getCards()
                                                  .get(0)
                                                  .getAlias());
            }
        } catch (ApiException exception) {
            RequestContext.put(ApiClientHelper.EXCEPTION, exception);
            log.error("Exception occurred while calling searchCard API: " + exception.getMessage(), exception);
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
            CardApi cardApi = new CardApi(apiClient);

            String cardId = ApiClientHelper.getRequestObject("card-id", String.class);

            /** Set request header values */
            String xMCCorrelationID = randomUUID();
            String xMCBankCode = null;
            String xMCSource = null;
            String xMCClientApplicationUserID = null;

            String fields = null; // remove response filter

            response = cardApi.getCard(cardId, xMCBankCode, xMCCorrelationID, xMCSource, xMCClientApplicationUserID, fields);

            if (response.getCard() != null && response.getClients() != null) {
                log.info("card ID={} belongs is linked to client Code={} ", response.getCard()
                                                                                    .getId(),
                        response.getClients()
                                .get(0)
                                .getCode());
            }

        } catch (ApiException exception) {
            RequestContext.put(ApiClientHelper.EXCEPTION, exception);
            log.error("Exception occurred while calling getCard API: " + exception.getMessage(), exception);
        }
        return response;
    }

    /**
     * Update pin.
     * 
     * @throws ReferenceAppGenericException
     * @throws GeneralSecurityException
     */
    public boolean updatePin() throws ReferenceAppGenericException, GeneralSecurityException {
        Boolean success = null;
        try {
            CardApi cardApi = new CardApi(apiClient);

            String cardNumber = ApiClientHelper.getRequestObject("card-number", String.class);
            String cardId = ApiClientHelper.getRequestObject("card-id", String.class);
            NewPinDetails request = ApiClientHelper.getRequestObject(UPDATE_PIN, NewPinDetails.class);

            /** Prerequisite - Create Token and set token */
            log.info(">> Create Token for card id: {}", cardId);

            AuthorizationManagementService authorizationManagementService = new AuthorizationManagementService();
            TokenDetails pinChangeTokenDetails = authorizationManagementService.createToken("PIN_RESET");
            log.info("Received token for PIN_RESET intent: {}", pinChangeTokenDetails.getToken());
            request.setToken(pinChangeTokenDetails.getToken());

            /** Generate random 4 digit PIN and build encrypted PIN block */
            String clearPin = String.format("%04d", new SecureRandom().nextInt(10000));
            /**
             * Use below log statement to view PIN in clear. But should be removed before moving the code to higher environment.<BR>
             * log.debug("Generated random PIN: {}", clearPin);
             */

            log.info("Build and encrypt PIN block.");
            PinBlockTDEAEncrypter pinBlockTDEAEncrypter = PinBlockTDEAEncrypter.getInstance();
            EncryptedPinBlock encryptedPinBlock = pinBlockTDEAEncrypter.encryptPin(clearPin, cardNumber);
            request.setPin(encryptedPinBlock);

            log.info("Updated PIN block in the request.");

            /** Set request validity time */
            request.setDataValidUntilTimestamp(getRequestExpiryTimestamp());

            /** Set request header values */
            String xMCCorrelationID = randomUUID();
            String xMCBankCode = null;
            String xMCSource = null;
            String xMCClientApplicationUserID = null;

            log.info(">> Update PIN using the token {} for card id: {}", request.getToken(), cardId);
            cardApi.updatePin(cardId, request, xMCBankCode, xMCCorrelationID, xMCSource, xMCClientApplicationUserID);
            success = true;

        } catch (ApiException exception) {
            RequestContext.put(ApiClientHelper.EXCEPTION, exception);
            log.error("Exception occurred while calling updatePin API: " + exception.getMessage(), exception);
            success = null;
        }
        return success;
    }

    /**
     * Gets the client.
     *
     * @return the client
     */
    public ClientProfile getClient() {
        ClientProfile response = null;
        try {
            ClientApi getClientApi = new ClientApi(apiClient);

            String clientCode = ApiClientHelper.getRequestObject("client-code", String.class);

            /** Set request header values */
            String xMCCorrelationID = randomUUID();
            String xMCBankCode = null;
            String xMCSource = null;
            String xMCClientApplicationUserID = null;

            String fields = null;
            response = getClientApi.getClient(clientCode, xMCBankCode, xMCCorrelationID, xMCSource, xMCClientApplicationUserID, fields);

            if (response.getClient() != null) {
                log.debug("getClient response clientCode={} ", response.getClient()
                                                                       .getCode());
            }
        } catch (ApiException exception) {
            RequestContext.put(ApiClientHelper.EXCEPTION, exception);
            log.error("Exception occurred while calling getClient API: " + exception.getMessage(), exception);
        }
        return response;
    }

    /** Update client. */
    public ServiceRequestDetails updateClient() {
        ServiceRequestDetails response = null;
        try {
            ClientApi clientApi = new ClientApi(apiClient);

            String clientCode = ApiClientHelper.getRequestObject("client-code", String.class);
            UpdatedClient request = ApiClientHelper.getRequestObject(UPDATE_CLIENT, UpdatedClient.class);

            request.setDataValidUntilTimestamp(getRequestExpiryTimestamp());

            /** Set request header values */
            String xMCCorrelationID = randomUUID();
            String xMCBankCode = null;
            String xMCSource = null;
            String xMCClientApplicationUserID = null;

            /** Trying to dummy first name */
            request.getClient()
                   .getProfile()
                   .setFirstName("Johnny");

            /** Trying to set dummy date of birth to today minus 20 years */
            request.getClient()
                   .getProfile()
                   .setBirthDate(LocalDate.now()
                                          .minusYears(20));

            response = clientApi.updateClient(clientCode, request, xMCBankCode, xMCCorrelationID, xMCSource, xMCClientApplicationUserID);

            if (response.getServiceRequests() != null) {
                log.debug("getClient response ServiceRequest number={} ", response.getServiceRequests()
                                                                                  .get(0)
                                                                                  .getNumber());
            }
        } catch (ApiException exception) {
            RequestContext.put(ApiClientHelper.EXCEPTION, exception);
            log.error("Exception occurred while calling updateClient API: " + exception.getMessage(), exception);
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
