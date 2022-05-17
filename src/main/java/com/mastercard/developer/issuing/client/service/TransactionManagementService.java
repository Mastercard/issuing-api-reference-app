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

import java.time.format.DateTimeParseException;

import com.mastercard.developer.issuing.client.helper.ApiClientHelper;
import com.mastercard.developer.issuing.client.helper.RequestContext;
import com.mastercard.developer.issuing.generated.apis.BalanceInquiryApi;
import com.mastercard.developer.issuing.generated.apis.TopUpApi;
import com.mastercard.developer.issuing.generated.apis.TransactionHistoryApi;
import com.mastercard.developer.issuing.generated.invokers.ApiClient;
import com.mastercard.developer.issuing.generated.invokers.ApiException;
import com.mastercard.developer.issuing.generated.models.BalanceDetails;
import com.mastercard.developer.issuing.generated.models.BalanceTransaction;
import com.mastercard.developer.issuing.generated.models.Topup;
import com.mastercard.developer.issuing.generated.models.TopupTransaction;
import com.mastercard.developer.issuing.generated.models.TransactionDetails;
import com.mastercard.developer.issuing.generated.models.TransactionSearch;

import lombok.extern.log4j.Log4j2;

/** The Constant log. */
@Log4j2
public class TransactionManagementService extends BaseService {

    /** The Constant SERVICE_CONTEXT. */
    private static final String SERVICE_CONTEXT = "/transaction-management";

    /** The Constant CARD_ID. */
    private static final String CARD_ID = "card-id";

    /** The Constant TOPUP_PREPAID_CARD. */
    private static final String TOPUP_PREPAID_CARD = "topup-prepaid-card";

    /** The Constant TRANSACTION_HISTORY. */
    private static final String TRANSACTION_HISTORY = "transaction-history";

    /** The Constant BALANCE_INQUIRY. */
    private static final String BALANCE_INQUIRY = "balance-inquiry";

    /** The scenarios. */
    protected String[] scenarios = { TOPUP_PREPAID_CARD, TRANSACTION_HISTORY, BALANCE_INQUIRY };

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
            case TOPUP_PREPAID_CARD:
                logScenario(scenario);
                Topup topup = topupPrepaidCard();
                ApiClientHelper.saveResponseObject(scenario, topup);
                break;
            case TRANSACTION_HISTORY:
                logScenario(scenario);
                TransactionDetails transactionDetails = transactionHistory();
                ApiClientHelper.saveResponseObject(scenario, transactionDetails);
                break;
            case BALANCE_INQUIRY:
                logScenario(scenario);
                BalanceDetails balanceDetails = balanceInquiry();
                ApiClientHelper.saveResponseObject(scenario, balanceDetails);
                break;
            default:
                break;
            }
        }
    }

    /** Topup prepaid card. */
    public Topup topupPrepaidCard() {
        Topup response = null;
        try {
            TopUpApi topUpApi = new TopUpApi(apiClient);

            String cardId = ApiClientHelper.getRequestObject(CARD_ID, String.class);
            TopupTransaction request = ApiClientHelper.getRequestObject(TOPUP_PREPAID_CARD, TopupTransaction.class);

            /** Prerequisite - Create Token and set token * */
            /** Step 1: Set request validity time */
            request.setDataValidUntilTimestamp(getRequestExpiryTimestamp());

            /** Set request header values */
            String xMCIdempotencyKey = randomUUID();
            String xMCCorrelationID = randomUUID();
            String xMCBankCode = null;
            String xMCSource = null;
            String xMCClientApplicationUserID = null;

            response = topUpApi.topupAccount(xMCIdempotencyKey, cardId, request, xMCBankCode, xMCCorrelationID, xMCSource,
                    xMCClientApplicationUserID);

            if (response.getTransactionMetaData() != null) {
                log.debug("topupPrepaidCard response getAuthorizationId={} ", response.getTransactionMetaData()
                                                                                      .getAuthorizationId());
            }

        } catch (ApiException exception) {
            RequestContext.put("Exception", exception);
            log.error("Exception occurred while calling topupPrepaidCard API: " + exception.getMessage(), exception);
        } catch (DateTimeParseException dateTimeParseException) {
            RequestContext.put("Exception", dateTimeParseException);
            log.error(
                    "Exception occurred while parsing the topupPrepaidCard API response datetime type field: " + dateTimeParseException.getMessage(),
                    dateTimeParseException);
        }
        return response;
    }

    /** Transaction history. */
    public TransactionDetails transactionHistory() {
        TransactionDetails response = null;
        try {
            TransactionHistoryApi transactionHistoryApi = new TransactionHistoryApi(apiClient);

            String cardId = ApiClientHelper.getRequestObject(CARD_ID, String.class);
            TransactionSearch request = ApiClientHelper.getRequestObject(TRANSACTION_HISTORY, TransactionSearch.class);

            /**
             * Optional - Create Token and set token, required only if configured as mandatory in CSR portal *
             */
            request.setDataValidUntilTimestamp(getRequestExpiryTimestamp());

            /** Set request header values */
            String xMCCorrelationID = randomUUID();
            String xMCBankCode = null;
            String xMCSource = null;
            String xMCClientApplicationUserID = null;

            /** Set request query parameter values */
            Integer offset = null;
            Integer limit = null;
            String fields = null;
            response = transactionHistoryApi.getHistory(cardId, request, xMCBankCode, xMCCorrelationID, xMCSource, xMCClientApplicationUserID, offset,
                    limit, fields);

            if (response.getTransactionMetaData() != null) {
                log.debug("transactionHistory response getAuthorizationId={} ", response.getTransactionMetaData()
                                                                                        .getAuthorizationId());
            }

        } catch (ApiException exception) {
            RequestContext.put("Exception", exception);
            log.error("Exception occurred while calling transactionHistory API: " + exception.getMessage(), exception);
        } catch (DateTimeParseException dateTimeParseException) {
            RequestContext.put("Exception", dateTimeParseException);
            log.error("Exception occurred while parsing the transactionHistory API response datetime type field: "
                    + dateTimeParseException.getMessage(), dateTimeParseException);
        }
        return response;
    }

    /** Balance inquiry. */
    public BalanceDetails balanceInquiry() {
        BalanceDetails response = null;
        try {
            BalanceInquiryApi balanceInquiryApi = new BalanceInquiryApi(apiClient);

            String cardId = ApiClientHelper.getRequestObject(CARD_ID, String.class);
            BalanceTransaction request = ApiClientHelper.getRequestObject(BALANCE_INQUIRY, BalanceTransaction.class);

            /** Prerequisite - Create Token and set token * */
            request.setDataValidUntilTimestamp(getRequestExpiryTimestamp());

            /** Set request header values */
            String xMCIdempotencyKey = randomUUID();
            String xMCCorrelationID = randomUUID();
            String xMCBankCode = null;
            String xMCSource = null;
            String xMCClientApplicationUserID = null;
            String fields = null;

            response = balanceInquiryApi.getBalance(xMCIdempotencyKey, cardId, request, xMCBankCode, xMCCorrelationID, xMCSource,
                    xMCClientApplicationUserID, fields);

            if (response.getTransactionMetaData() != null) {
                log.debug("balanceInquiry response getAuthorizationId={} ", response.getTransactionMetaData()
                                                                                    .getAuthorizationId());
            }
        } catch (ApiException exception) {
            RequestContext.put("Exception", exception);
            log.error("Exception occurred while calling balanceInquiry API: " + exception.getMessage(), exception);
        } catch (DateTimeParseException dateTimeParseException) {
            RequestContext.put("Exception", dateTimeParseException);
            log.error("Exception occurred while parsing the balanceInquiry API response datetime type field: " + dateTimeParseException.getMessage(),
                    dateTimeParseException);
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