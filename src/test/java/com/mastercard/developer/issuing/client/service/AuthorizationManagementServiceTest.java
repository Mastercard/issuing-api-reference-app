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
import java.time.Instant;
import java.time.LocalDate;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.mastercard.developer.issuing.generated.invokers.ApiClient;
import com.mastercard.developer.issuing.generated.invokers.ApiException;
import com.mastercard.developer.issuing.generated.invokers.ApiResponse;
import com.mastercard.developer.issuing.generated.models.CardDetails;
import com.mastercard.developer.issuing.generated.models.CardProfile;
import com.mastercard.developer.issuing.generated.models.CardSecret;
import com.mastercard.developer.issuing.generated.models.ClientProfile;
import com.mastercard.developer.issuing.generated.models.GetClientDetails;
import com.mastercard.developer.issuing.generated.models.Profile;
import com.mastercard.developer.issuing.generated.models.TokenDetails;

import okhttp3.Call;

/**
 * The Class AuthorizationManagementServiceTest.
 *
 * @author e084506
 */
public class AuthorizationManagementServiceTest {

    /** The service. */
    private AuthorizationManagementService service = new AuthorizationManagementService();

    @Mock
    protected CardManagementService cardManagementServiceMock;

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
     * Test create token.
     *
     * @throws Exception the exception
     */
    @Test
    public void testCreateToken() throws Exception {
        /** Mock Response Object */
        TokenDetails response = new TokenDetails();
        ApiResponse apiResponse = new ApiResponse(200, null, response);
        when(apiClientMock.execute(any(okhttp3.Call.class), any(Type.class))).thenReturn(apiResponse);

        ClientProfile clientProfile = new ClientProfile();
        GetClientDetails getClientDetails = new GetClientDetails();
        Profile profile = new Profile();
        profile.setBirthDate(LocalDate.now());
        getClientDetails.setProfile(profile);
        clientProfile.setClient(getClientDetails);

        clientProfile.getClient()
                     .getProfile()
                     .getBirthDate();
        when(cardManagementServiceMock.getClient()).thenReturn(clientProfile);
        
        CardProfile cardProfile = new CardProfile();
        CardDetails cardDetails = new CardDetails();
        cardDetails.setCvv("456");
        cardDetails.setExpiry("09/25");        
        cardProfile.setCard(cardDetails);
        when(cardManagementServiceMock.getCard()).thenReturn(cardProfile);
        
        TokenDetails actualResponse = service.createToken("BALANCE_INQUIRY");

        assertEquals(response, actualResponse);
    }
}
