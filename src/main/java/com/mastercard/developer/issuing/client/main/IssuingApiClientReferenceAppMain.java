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
package com.mastercard.developer.issuing.client.main;

import com.mastercard.developer.issuing.client.service.IssuingAuthorizationManagementService;
import com.mastercard.developer.issuing.client.service.IssuingCardControlsService;
import com.mastercard.developer.issuing.client.service.IssuingCardIssuanceService;
import com.mastercard.developer.issuing.client.service.IssuingCardManagementService;
import com.mastercard.developer.issuing.client.service.IssuingTransactionManagementService;

import lombok.extern.log4j.Log4j2;

/** The Constant log. */
@Log4j2
public class IssuingApiClientReferenceAppMain {

    /** The Constant authorizationManagementService. */
    private static final IssuingAuthorizationManagementService authorizationManagementService = new IssuingAuthorizationManagementService();

    /** The Constant cardControlsService. */
    private static final IssuingCardControlsService cardControlsService = new IssuingCardControlsService();

    /** The Constant cardIssuanceService. */
    private static final IssuingCardIssuanceService cardIssuanceService = new IssuingCardIssuanceService();

    /** The Constant cardManagementService. */
    private static final IssuingCardManagementService cardManagementService = new IssuingCardManagementService();

    /** The Constant transactionManagementService. */
    private static final IssuingTransactionManagementService transactionManagementService = new IssuingTransactionManagementService();

    /**
     * The main method.
     *
     * @param args the arguments
     * @throws Exception the exception
     */
    public static void main(String[] args) throws Exception {

        if (args.length == 0 || "list".equalsIgnoreCase(args[0])) {
            log.info("List of scenarios available for execution/testing: ");
            authorizationManagementService.printScenarios();
            cardControlsService.printScenarios();
            cardIssuanceService.printScenarios();
            cardManagementService.printScenarios();
            transactionManagementService.printScenarios();

        } else if ("all".equalsIgnoreCase(args[0])) {
            log.info("Executing all scenario.");

            String[] scenario = { "prepaid-card-issuance", "debit-card-issuance", "get-card", "search-cards", "get-client", "update-client", "update-card-status",
                    "topup-prepaid-card", "transaction-history", "balance-inquiry" }; 

            for (int index = 0; index < 1; index++) {
                cardIssuanceService.callApis(scenario);
                cardManagementService.callApis(scenario);
                cardControlsService.callApis(scenario);
                authorizationManagementService.callApis(scenario);
                transactionManagementService.callApis(scenario);
            }

        } else {
            cardIssuanceService.callApis(args);
            cardManagementService.callApis(args);
            cardControlsService.callApis(args);
            authorizationManagementService.callApis(args);
            transactionManagementService.callApis(args);
        }
    }
}
