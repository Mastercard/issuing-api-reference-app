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
import com.mastercard.developer.issuing.generated.models.BalanceDetails;
import com.mastercard.developer.issuing.generated.models.Topup;
import com.mastercard.developer.issuing.generated.models.TransactionDetails;

import okhttp3.Call;

/** The Class IssuingTransactionManagementServiceTest. */
public class IssuingTransactionManagementServiceTest {

    /** The service. */
    private IssuingTransactionManagementService service = new IssuingTransactionManagementService();

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
    }

    /**
     * Test topup prepaid card.
     *
     * @throws Exception the exception
     */
    @Test
    public void testTopupPrepaidCard() throws Exception {
	/** Mock Response Object */
	Topup response = new Topup();
	ApiResponse apiResponse = new ApiResponse(200, null, response);
	when(apiClientMock.execute(any(okhttp3.Call.class), any(Type.class))).thenReturn(apiResponse);

	Topup actualResponse = service.topupPrepaidCard();

	assertEquals(response, actualResponse);
    }

    /**
     * Test transaction history.
     *
     * @throws Exception the exception
     */
    @Test
    public void testTransactionHistory() throws Exception {
	/** Mock Response Object */
	TransactionDetails response = new TransactionDetails();
	ApiResponse apiResponse = new ApiResponse(200, null, response);
	when(apiClientMock.execute(any(okhttp3.Call.class), any(Type.class))).thenReturn(apiResponse);

	TransactionDetails actualResponse = service.transactionHistory();

	assertEquals(response, actualResponse);
    }

    /**
     * Test balance inquiry.
     *
     * @throws Exception the exception
     */
    @Test
    public void testBalanceInquiry() throws Exception {
	/** Mock Response Object */
	BalanceDetails response = new BalanceDetails();
	ApiResponse apiResponse = new ApiResponse(200, null, response);
	when(apiClientMock.execute(any(okhttp3.Call.class), any(Type.class))).thenReturn(apiResponse);

	BalanceDetails actualResponse = service.balanceInquiry();

	assertEquals(response, actualResponse);
    }
}
