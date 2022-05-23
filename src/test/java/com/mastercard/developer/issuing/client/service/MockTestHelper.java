/**
 * Copyright (c) 2022 Mastercard
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mastercard.developer.issuing.client.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.mastercard.developer.issuing.generated.invokers.ApiClient;
import com.mastercard.developer.issuing.generated.invokers.ApiException;

import lombok.extern.log4j.Log4j2;
import okhttp3.Call;

/** The Class MockTestHelper. */
@Log4j2
public final class MockTestHelper {

    /**
     * Initialize api client.
     *
     * @param service       the service
     * @param apiClientMock the api client mock
     * @param mockCall      the mock call
     * @throws ApiException the api exception
     */
    public static void initializeApiClient(BaseService service, ApiClient apiClientMock, Call mockCall) throws ApiException {
        log.info("================================================================================================\n");

        service.setApiClient(apiClientMock);

        when(apiClientMock.buildCall(anyString(), anyString(), anyList(), anyList(), any(), anyMap(), anyMap(), anyMap(), any(), any())).thenReturn(
                mockCall);

        String cardId = "CE0D1750A41F5605E05337905B0AABE6";
        when(apiClientMock.escapeString(cardId)).thenReturn(cardId);

        String clientCode = "32323221271000002";
        when(apiClientMock.escapeString(clientCode)).thenReturn(clientCode);
    }
}
