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
import java.time.LocalDate;

import com.mastercard.developer.issuing.client.helper.ApiClientHelper;
import com.mastercard.developer.issuing.client.helper.PinBlockTDEAEncrypter;
import com.mastercard.developer.issuing.client.helper.RequestContext;
import com.mastercard.developer.issuing.exception.ReferenceAppGenericException;
import com.mastercard.developer.issuing.generated.apis.UserAuthenticationApi;
import com.mastercard.developer.issuing.generated.invokers.ApiClient;
import com.mastercard.developer.issuing.generated.models.CardDetails;
import com.mastercard.developer.issuing.generated.models.CardProfile;
import com.mastercard.developer.issuing.generated.models.CardSecret;
import com.mastercard.developer.issuing.generated.models.ClientAuthentication;
import com.mastercard.developer.issuing.generated.models.ClientProfile;
import com.mastercard.developer.issuing.generated.models.ClientSecret;
import com.mastercard.developer.issuing.generated.models.EncryptedPinBlock;
import com.mastercard.developer.issuing.generated.models.TokenDetails;

import lombok.extern.log4j.Log4j2;

/** The Constant log. */
@Log4j2
public class AuthorizationManagementService extends BaseService {

    /** The Constant SERVICE_CONTEXT. */
    private static final String SERVICE_CONTEXT = "/authorization-management";

    /** The Constant AUTHENTICATION_TOKEN. */
    private static final String AUTHENTICATION_TOKEN = "authentication-token";

    /** The Constant AUTHENTICATION_TOKEN. */
    private static final String PIN_CHANGE_TOKEN = "pin-change-token";

    /** The Constant AUTHENTICATION_TOKEN. */
    private static final String PIN_RESET_TOKEN = "pin-reset-token";

    /** The scenarios. */
    private static final String[] SCENARIOS = { PIN_RESET_TOKEN, PIN_CHANGE_TOKEN, AUTHENTICATION_TOKEN };

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
            case PIN_RESET_TOKEN:
                logScenario(scenario);
                TokenDetails pinResetTokenDetails = createToken(AuthTokenIntent.PIN_RESET);
                ApiClientHelper.saveResponseObject(scenario, pinResetTokenDetails);
                break;
            case PIN_CHANGE_TOKEN:
                logScenario(scenario);
                TokenDetails pinChangeTokenDetails = createToken(AuthTokenIntent.PIN_RESET);
                ApiClientHelper.saveResponseObject(scenario, pinChangeTokenDetails);
                break;
            case AUTHENTICATION_TOKEN:
                logScenario(scenario);
                TokenDetails tokenDetails = createToken(AuthTokenIntent.BALANCE_INQUIRY);
                ApiClientHelper.saveResponseObject(scenario, tokenDetails);
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
            UserAuthenticationApi userAuthenticationApi = new UserAuthenticationApi(apiClient);

            String cardId = ApiClientHelper.getRequestObject("card-id", String.class);
            String cardNumber = ApiClientHelper.getRequestObject("card-number", String.class);
            log.info(">>> Start preparation to call createToken for cardId={}", cardId);

            ClientAuthentication request = ApiClientHelper.getRequestObject(AUTHENTICATION_TOKEN, ClientAuthentication.class);

            /** Update intent */
            request.setIntent(intent);

            switch (intent) {
            case AuthTokenIntent.PIN_RESET:
                updateClientSecrets(request);
                break;
            case AuthTokenIntent.PIN_CHANGE:
                updateOldPin(request, cardNumber);
                break;
            case AuthTokenIntent.BALANCE_INQUIRY:
                updateClientSecrets(request);
                break;
            default:
                break;
            }

            /** Step 1: Set request validity time */
            request.setDataValidUntilTimestamp(getRequestExpiryTimestamp());

            /** Set request header values */
            String xMCIdempotencyKey = randomUUID();
            String xMCCorrelationID = randomUUID();
            String xMCBankCode = null;
            String xMCSource = null;
            String xMCClientApplicationUserID = null;

            log.info(">>> Calling createToken for cardId={}", cardId);

            response = userAuthenticationApi.createToken(xMCIdempotencyKey, cardId, request, xMCBankCode, xMCCorrelationID, xMCSource,
                    xMCClientApplicationUserID);
            token = response.getToken();

            log.info("Received token = {}", token);

        } catch (Exception exception) {
            RequestContext.put(ApiClientHelper.EXCEPTION, exception);
            log.error("Exception occurred while calling an API: " + exception.getMessage(), exception);
            throw new ReferenceAppGenericException("Exception occurred while calling an API: ", exception);
        }
        return response;
    }

    private void updateClientSecrets(ClientAuthentication request) {
        /** Option 1 - Update Date of birth in the request */
        CardManagementService cardManagementService = new CardManagementService();
        log.info(">>> Fetch Client Date of Birth by Calling Get Client API");
        ClientProfile clientProfile = cardManagementService.getClient();
        LocalDate birthDate = clientProfile.getClient()
                                           .getProfile()
                                           .getBirthDate();
        ClientSecret clientSecret = request.getClient();
        if (clientSecret == null) {
            clientSecret = new ClientSecret();
            request.setClient(clientSecret);
        }
        clientSecret.setBirthDate(birthDate);

        /** Option 2 - Update Card CVV & Expiry */
        log.info(">>> Fetch Card Details by Calling Get Card API");
        CardProfile cardProfile = cardManagementService.getCard();
        CardDetails cardDetails = cardProfile.getCard();
        CardSecret cardSecret = request.getCard();
        if (cardSecret == null) {
            cardSecret = new CardSecret();
            request.setCard(cardSecret);
        }
        cardSecret.setCvv(cardDetails.getCvv());
        cardSecret.setExpiry(cardDetails.getExpiry());
    }

    private void updateOldPin(ClientAuthentication request, String cardNumber) throws GeneralSecurityException {
        /** Option 3 - Need to set old PIN manually */
        String clearPin = "1234";
        PinBlockTDEAEncrypter pinBlockTDEAEncrypter = PinBlockTDEAEncrypter.getInstance();
        EncryptedPinBlock encryptedPinBlock = pinBlockTDEAEncrypter.encryptPin(clearPin, cardNumber);

        CardSecret cardSecret = request.getCard();
        if (cardSecret == null) {
            cardSecret = new CardSecret();
            request.setCard(cardSecret);
        }
        cardSecret.setPin(encryptedPinBlock);
        request.setCard(cardSecret);
        /** Remove client attributes */
        request.setClient(null);
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
