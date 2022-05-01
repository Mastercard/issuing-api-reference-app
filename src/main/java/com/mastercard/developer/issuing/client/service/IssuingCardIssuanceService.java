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
import com.mastercard.developer.issuing.generated.apis.DebitCardApi;
import com.mastercard.developer.issuing.generated.apis.PrepaidCardApi;
import com.mastercard.developer.issuing.generated.invokers.ApiClient;
import com.mastercard.developer.issuing.generated.invokers.ApiException;
import com.mastercard.developer.issuing.generated.models.DebitCardDetails;
import com.mastercard.developer.issuing.generated.models.DebitCardProfile;
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

    private static final String DEBIT_CARD_ISSUANCE = "debit-card-issuance";

    /** The scenarios. */
    protected String[] scenarios = { PREPAID_CARD_ISSUANCE };

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
                PrepaidCardProfile prepaidCardProfile = prepaidCardIssuance();
                ApiClientHelper.saveResponseObject(scenario, prepaidCardProfile);
                break;
            case DEBIT_CARD_ISSUANCE:
                logScenario(scenario);
                DebitCardProfile debitCardProfile = debitCardIssuance();
                ApiClientHelper.saveResponseObject(scenario, debitCardProfile);
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
            PrepaidCardApi prepaidCardApi = new PrepaidCardApi(apiClient);

            PrepaidCardApplication request = ApiClientHelper.getRequestObject(PREPAID_CARD_ISSUANCE, PrepaidCardApplication.class);

            /** Step 1: Set request validity time */
            request.setDataValidUntilTimestamp(getRequestExpiryTimestamp());

            /** Set request header values */
            String xMCIdempotencyKey = randomUUID();
            String xMCCorrelationID = randomUUID();
            String xMCBankCode = null;
            String xMCSource = null;
            String xMCClientApplicationUserID = null;

            String alias = String.valueOf(System.currentTimeMillis());
            request.getCard()
                   .setAlias(alias);

            response = prepaidCardApi.createPrepaidCard(xMCIdempotencyKey, request, xMCBankCode, xMCCorrelationID, xMCSource,
                    xMCClientApplicationUserID);

            /** Verify and persist new client and card details */
            Profile profile = request.getClient()
                                     .getProfile();
            log.info("Requested client details cardAlias={}, FirstName={}, LastName={}, BirthDate={}", alias, profile.getFirstName(),
                    profile.getLastName(), profile.getBirthDate());

            String clientCode1 = "";
            String cardId1 = "";

            if (response.getCards() != null && response.getClient() != null) {
                NewCardDetails newCard = response.getCards()
                                                 .get(0);
                NewClient newClient = response.getClient();
                clientCode1 = newClient.getCode();
                Mobile mobile = newClient.getMobile();
                cardId1 = newCard.getId();
                String cvv = newCard.getCvv();

                log.info("New card 1 generated with cardID={}, cardAlias={}, ISD={}, Mobile={}, cvv={}", cardId1, newCard.getAlias(), mobile.getIsd(),
                        mobile.getNumber(), cvv);
            }

            /** Lets retry same request, with different correlation id for traceability */
            xMCCorrelationID = randomUUID();
            /** In case of retry using same idempotency key, API will return the same response as previous call */
            response = prepaidCardApi.createPrepaidCard(xMCIdempotencyKey, request, xMCBankCode, xMCCorrelationID, xMCSource,
                    xMCClientApplicationUserID);

            log.info("response 2", response);

            String clientCode2 = "";
            String cardId2 = "";
            if (response.getCards() != null && response.getClient() != null) {
                NewCardDetails newCard = response.getCards()
                                                 .get(0);
                NewClient newClient = response.getClient();
                clientCode2 = newClient.getCode();
                Mobile mobile = newClient.getMobile();
                cardId2 = newCard.getId();
                String cvv = newCard.getCvv();

                log.info("New card 2 generated with cardID={}, cardAlias={}, ISD={}, Mobile={}, cvv={}", cardId2, newCard.getAlias(), mobile.getIsd(),
                        mobile.getNumber(), cvv);
            }

            /** client code and card id of both requests should be same */
            if (clientCode1 != null && cardId1 != null && !clientCode1.equals(clientCode2) || !cardId1.equals(cardId2)) {
                throw new ApiException("Retry with same idempotency key, generated new client/card.");
            } else {
                log.info(
                        "Retry with same idempotency key, client/card generated only once and response of first call is returned to all subsequent calls.");
            }

        } catch (ApiException exception) {
            log.error("Exception occurred while calling an API: " + exception.getMessage(), exception);
        }
        return response;
    }

    /** Debit card issuance. */
    public DebitCardProfile debitCardIssuance() {
        DebitCardProfile response = null;
        try {
            DebitCardApi debitCardApi = new DebitCardApi(apiClient);

            DebitCardDetails request = ApiClientHelper.getRequestObject(DEBIT_CARD_ISSUANCE, DebitCardDetails.class);

            /** Step 1: Set request validity time */
            request.setDataValidUntilTimestamp(getRequestExpiryTimestamp());

            /** Set request header values */
            String xMCIdempotencyKey = randomUUID();
            String xMCCorrelationID = randomUUID();
            String xMCBankCode = null; /** Sandbox bank "323232" */
            String xMCSource = null;
            String xMCClientApplicationUserID = null;

            /** Set new value of card alias for each request */
            String alias = String.valueOf(System.currentTimeMillis());
            request.getCard()
                   .setAlias(alias);

            response = debitCardApi.createDebitCard(xMCIdempotencyKey, request, xMCBankCode, xMCCorrelationID, xMCSource, xMCClientApplicationUserID);

            /** Verify and persist new client and card details */
            Profile profile = request.getClient()
                                     .getProfile();
            log.info("Requested client details cardAlias={}, FirstName={}, LastName={}, BirthDate={}", alias, profile.getFirstName(),
                    profile.getLastName(), profile.getBirthDate());

            String clientCode1 = null;
            String cardId1 = null;

            if (response.getCards() != null && response.getClient() != null) {
                NewCardDetails newCard = response.getCards()
                                                 .get(0);
                NewClient newClient = response.getClient();
                clientCode1 = newClient.getCode();
                Mobile mobile = newClient.getMobile();
                cardId1 = newCard.getId();
                String cvv = newCard.getCvv();

                log.info("New card generated with cardID={}, cardAlias={}, ISD={}, Mobile={}, cvv={}", cardId1, newCard.getAlias(), mobile.getIsd(),
                        mobile.getNumber(), cvv);
            }

            /** Lets retry same request, with different correlation id for traceability */
            xMCCorrelationID = randomUUID();
            /** In case of retry using same idempotency key, API will return the same response as previous call */
            response = debitCardApi.createDebitCard(xMCIdempotencyKey, request, xMCBankCode, xMCCorrelationID, xMCSource, xMCClientApplicationUserID);

            String clientCode2 = null;
            String cardId2 = null;
            if (response.getCards() != null && response.getClient() != null) {
                NewCardDetails newCard = response.getCards()
                                                 .get(0);
                NewClient newClient = response.getClient();
                clientCode2 = newClient.getCode();
                Mobile mobile = newClient.getMobile();
                cardId2 = newCard.getId();
                String cvv = newCard.getCvv();

                log.info("New card generated with cardID={}, cardAlias={}, ISD={}, Mobile={}, cvv={}", cardId2, newCard.getAlias(), mobile.getIsd(),
                        mobile.getNumber(), cvv);
            }

            if (clientCode1 != null && cardId1 != null && !clientCode1.equals(clientCode2) || !cardId1.equals(cardId2)) {
                throw new ApiException("Retry with same idempotency key, generated new client/card.");
            } else {
                log.info(
                        "Retry with same idempotency key, client/card generated only once and response of first call is returned to all subsequent calls.");
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
