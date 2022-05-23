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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.lang.reflect.Type;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.mastercard.developer.issuing.generated.invokers.ApiClient;
import com.mastercard.developer.issuing.generated.invokers.ApiException;
import com.mastercard.developer.issuing.generated.invokers.ApiResponse;
import com.mastercard.developer.issuing.generated.models.UpdatedCardStatusDetails;

import okhttp3.Call;

/** The Class CardControlsServiceTest. */
public class CardControlsServiceTest {

    /** The service. */
    private CardControlsService service = new CardControlsService();

    /** The api client mock. */
    @Mock
    protected ApiClient apiClientMock;

    /** The mock call. */
    @Mock
    protected Call mockCall;

    /**
     * Sets the up.
     *
     * @throws ApiException the api exception
     */
    @Before
    public void setUp() throws ApiException {
        MockitoAnnotations.initMocks(this);
        MockTestHelper.initializeApiClient(service, apiClientMock, mockCall);
        when(apiClientMock.escapeString(any(String.class))).thenReturn("1234567890123456");
    }

    /**
     * Test update card status.
     *
     * @throws ApiException the api exception
     */
    @Test
    public void testUpdateCardStatus() throws ApiException {
        /** Mock Response Object */
        UpdatedCardStatusDetails response = new UpdatedCardStatusDetails();
        ApiResponse apiResponse = new ApiResponse(200, null, response);
        when(apiClientMock.execute(any(okhttp3.Call.class), any(Type.class))).thenReturn(apiResponse);

        UpdatedCardStatusDetails actualResponse = service.updateCardStatus("update-card-status-blocked");

        assertEquals(response, actualResponse);
    }
}
