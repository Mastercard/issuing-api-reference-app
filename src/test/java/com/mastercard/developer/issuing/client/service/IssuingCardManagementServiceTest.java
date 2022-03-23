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

import com.mastercard.developer.issuing.generated.invokers.ApiClient;
import com.mastercard.developer.issuing.generated.invokers.ApiException;
import com.mastercard.developer.issuing.generated.invokers.ApiResponse;
import com.mastercard.developer.issuing.generated.models.CardProfile;
import com.mastercard.developer.issuing.generated.models.CardSearchResult;
import com.mastercard.developer.issuing.generated.models.ClientProfile;
import com.mastercard.developer.issuing.generated.models.ServiceRequestDetails;
import java.lang.reflect.Type;
import okhttp3.Call;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** The Class IssuingCardManagementServiceTest. */
public class IssuingCardManagementServiceTest {

  /** The service. */
  private IssuingCardManagementService service = new IssuingCardManagementService();

  /** The api client mock. */
  @Mock protected ApiClient apiClientMock;

  /** The mock call. */
  @Mock protected Call mockCall;

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
   * Test search card.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchCard() throws Exception {
    /** Mock Response Object */
    CardSearchResult response = new CardSearchResult();
    ApiResponse apiResponse = new ApiResponse(200, null, response);
    when(apiClientMock.execute(any(okhttp3.Call.class), any(Type.class))).thenReturn(apiResponse);

    CardSearchResult expectedResponse = service.searchCard();

    assertEquals(response, expectedResponse);
  }

  /**
   * Test get card.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetCard() throws Exception {
    /** Mock Response Object */
    CardProfile response = new CardProfile();
    ApiResponse apiResponse = new ApiResponse(200, null, response);
    when(apiClientMock.execute(any(okhttp3.Call.class), any(Type.class))).thenReturn(apiResponse);

    CardProfile expectedResponse = service.getCard();

    assertEquals(response, expectedResponse);
  }

  /**
   * Test update pin.
   *
   * @throws Exception the exception
   */
  @Test
  public void testUpdatePin() throws Exception {
    boolean success = service.updatePin();
    assertEquals(success, true);
  }

  /**
   * Test get client.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetClient() throws Exception {
    /** Mock Response Object */
    ClientProfile response = new ClientProfile();
    ApiResponse apiResponse = new ApiResponse(200, null, response);
    when(apiClientMock.execute(any(okhttp3.Call.class), any(Type.class))).thenReturn(apiResponse);

    ClientProfile expectedResponse = service.getClient();

    assertEquals(response, expectedResponse);
  }

  /**
   * Test update client.
   *
   * @throws Exception the exception
   */
  @Test
  public void testUpdateClient() throws Exception {
    /** Mock Response Object */
    ServiceRequestDetails response = new ServiceRequestDetails();
    ApiResponse apiResponse = new ApiResponse(200, null, response);
    when(apiClientMock.execute(any(okhttp3.Call.class), any(Type.class))).thenReturn(apiResponse);

    ServiceRequestDetails expectedResponse = service.updateClient();

    assertEquals(response, expectedResponse);
  }
}
