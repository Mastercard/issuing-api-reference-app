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
import com.mastercard.developer.issuing.client.helper.RequestContext;
import com.mastercard.developer.issuing.generated.apis.AcquirerControlApi;
import com.mastercard.developer.issuing.generated.apis.CardStatusApi;
import com.mastercard.developer.issuing.generated.invokers.ApiClient;
import com.mastercard.developer.issuing.generated.invokers.ApiException;
import com.mastercard.developer.issuing.generated.models.AcquirerCardUsageControl;
import com.mastercard.developer.issuing.generated.models.AcquirerCardUsageProfile;
import com.mastercard.developer.issuing.generated.models.ControlServiceRequests;
import com.mastercard.developer.issuing.generated.models.UpdatedCardStatus;
import com.mastercard.developer.issuing.generated.models.UpdatedCardStatusDetails;

import lombok.extern.log4j.Log4j2;

/** The Constant log. */

/** The Constant log. */
@Log4j2
public class CardControlsService extends BaseService {

    /** The Constant SERVICE_CONTEXT. */
    private static final String SERVICE_CONTEXT = "/card-controls";

    /** The Constant CARD_ID. */
    private static final String CARD_ID = "card-id";

    /** The Constant UPDATE_CARD_STATUS. */
    private static final String UPDATE_CARD_STATUS = "update-card-status";

    /** The Constant UPDATE_ACQ_CARD_CONTROLS. */
    private static final String UPDATE_ACQ_CARD_CONTROLS = "update-acquirer-level-controls";

    /** The Constant GET_ACQ_CONTROLS. */
    private static final String GET_ACQ_CONTROLS = "get-acquirer-level-controls";

    /** The scenarios. */
    protected String[] scenarios = { UPDATE_CARD_STATUS, UPDATE_ACQ_CARD_CONTROLS, GET_ACQ_CONTROLS };

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
            case UPDATE_CARD_STATUS:
                logScenario(scenario + " - BLOCKED");
                updateCardStatus("update-card-status-blocked");
                logScenario(scenario + " - NORMAL");
                UpdatedCardStatusDetails updatedCardStatusDetails = updateCardStatus("update-card-status-normal");
                ApiClientHelper.saveResponseObject(scenario, updatedCardStatusDetails);
                break;
            case GET_ACQ_CONTROLS:
                logScenario(scenario);
                AcquirerCardUsageProfile acquirerCardUsageProfile = getAcquirerControl();
                ApiClientHelper.saveResponseObject(scenario, acquirerCardUsageProfile);
                break;
            case UPDATE_ACQ_CARD_CONTROLS:
                logScenario(scenario);
                ControlServiceRequests controlServiceRequests = updateAcquirerControl();
                ApiClientHelper.saveResponseObject(scenario, controlServiceRequests);
                break;
            default:
                break;
            }
        }
    }

    /**
     * Update card status.
     *
     * @param requestFilePrefix the request file prefix
     * @return the updated card status details
     */
    public UpdatedCardStatusDetails updateCardStatus(String requestFilePrefix) {

        String cardId = null;
        UpdatedCardStatus request = null;
        UpdatedCardStatusDetails response = null;
        CardStatusApi cardStatusApi = null;

        /** Set request header values */
        String xMCCorrelationID = randomUUID();
        String xMCBankCode = null;
        String xMCSource = null;
        String xMCClientApplicationUserID = null;

        try {
            cardStatusApi = new CardStatusApi(apiClient);

            cardId = ApiClientHelper.getRequestObject(CARD_ID, String.class);

            request = ApiClientHelper.getRequestObject(requestFilePrefix, UpdatedCardStatus.class);

            /** Step 1: Set request validity time */
            request.setDataValidUntilTimestamp(getRequestExpiryTimestamp());

            response = cardStatusApi.update(cardId, request, xMCBankCode, xMCCorrelationID, xMCSource, xMCClientApplicationUserID);
            if (response.getCards() != null) {
                log.debug("updateCardStatus response Status={}", response.getCards()
                                                                         .get(0)
                                                                         .getStatus());
            }
        } catch (ApiException exception) {
            RequestContext.put(ApiClientHelper.EXCEPTION, exception);
            log.error("Exception occurred while calling updateCardStatus API: " + exception.getMessage(), exception);

            if (requestFilePrefix.equalsIgnoreCase("update-card-status-normal")) {
                xMCCorrelationID = randomUUID();
                request.setReason("BANK_DECISION");
                log.info("Retry with reason: BANK_DECISION");
                try {
                    response = cardStatusApi.update(cardId, request, xMCBankCode, xMCCorrelationID, xMCSource, xMCClientApplicationUserID);
                    log.debug("updateCardStatus response in catch block {}", response);
                } catch (ApiException e) {
                    RequestContext.put(ApiClientHelper.EXCEPTION, exception);
                    log.error("Exception occurred while calling an API: " + exception.getMessage(), exception);
                }
            }
        }
        return response;
    }

    /** Update card Acquirer level controls. */
    public ControlServiceRequests updateAcquirerControl() {

        ControlServiceRequests response = null;
        try {
            String cardId = ApiClientHelper.getRequestObject(CARD_ID, String.class);

            AcquirerCardUsageControl request = ApiClientHelper.getRequestObject(UPDATE_ACQ_CARD_CONTROLS, AcquirerCardUsageControl.class);

            request.setDataValidUntilTimestamp(getRequestExpiryTimestamp());

            AcquirerControlApi controlCardUsageApi = new AcquirerControlApi(apiClient);

            /** Set request header values */
            String xMCIdempotencyKey = randomUUID();
            String xMCCorrelationID = randomUUID();
            String xMCBankCode = null;
            String xMCSource = null;
            String xMCClientApplicationUserID = null;

            response = controlCardUsageApi.createAcquirerControl(xMCIdempotencyKey, cardId, request, xMCBankCode, xMCCorrelationID, xMCSource,
                    xMCClientApplicationUserID);

            if (response.getServiceRequestNumbers() != null) {
                log.debug("updateAcquirerControl response ServiceRequest Number={}", response.getServiceRequestNumbers()
                                                                                             .get(0));
            }
        } catch (ApiException exception) {
            RequestContext.put(ApiClientHelper.EXCEPTION, exception);
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

    /** Update card status. */

    private AcquirerCardUsageProfile getAcquirerControl() {
        AcquirerCardUsageProfile response = null;
        try {
            AcquirerControlApi getAcquirerControlApi = new AcquirerControlApi(apiClient);

            String cardId = ApiClientHelper.getRequestObject(CARD_ID, String.class);

            /** Set request header values */
            String xMCCorrelationID = randomUUID();
            String xMCBankCode = null;
            String xMCSource = null;
            String xMCClientApplicationUserID = null;
            Integer limit = null;
            Integer offset = null;
            String fields = null;

            String acquirerControls = "all";

            response = getAcquirerControlApi.getAcquirerControl(cardId, xMCBankCode, xMCCorrelationID, xMCSource, xMCClientApplicationUserID, limit,
                    offset, fields, acquirerControls);

            log.debug("getAcquirerControl response {}", response);

        } catch (ApiException exception) {
            RequestContext.put(ApiClientHelper.EXCEPTION, exception);
            log.error("Exception occurred while calling getCardControls API: " + exception.getMessage(), exception);
        }
        return response;
    }

}
