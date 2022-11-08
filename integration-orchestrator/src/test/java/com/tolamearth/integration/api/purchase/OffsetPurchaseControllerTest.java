/*
 * Copyright 2022 Tolam Earth
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.tolamearth.integration.api.purchase;

import com.tolamearth.integration.api.ApiError;
import com.tolamearth.integration.api.ApiNftId;
import com.tolamearth.integration.api.ErrorCode;
import com.tolamearth.integration.ledgerworks.discovery.TokenDiscoveryScheduler;
import com.tolamearth.integration.marketplace.buyer.BuyerService;
import io.micronaut.context.annotation.Property;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@MicronautTest
@Property(name = "api.integration-api-version", value = "/integration/v1")
@Property(name = "api.conversion-client-url", value = "http://localhost:7070")
class OffsetPurchaseControllerTest {

	@Inject
	@Client("/")
	HttpClient client;

	@Inject
	TokenDiscoveryScheduler tokenDiscoveryScheduler;

	@MockBean(TokenDiscoveryScheduler.class)
	TokenDiscoveryScheduler tokenDiscoveryScheduler() {
		return mock(TokenDiscoveryScheduler.class);
	}

	@Inject
	BuyerService marketplaceBuyerService;

	@MockBean(BuyerService.class)
	BuyerService marketplaceBuyerService() {
		return mock(BuyerService.class);
	}

	final String integrationApiVersion = "/integration/v1";

	final String ACCOUNT_ID = "0.0.1234";

	final String TOKEN_ID = "0.0.2345";

	final String SERIAL_NUMBER = "3456";

	final long PRICE = 10L;

	final String LOCATION = UriBuilder.of(integrationApiVersion).path("/offsets").path(TOKEN_ID + "," + SERIAL_NUMBER)
			.path("transactions").build().toString();

	final OffsetPurchaseRequest request = new OffsetPurchaseRequest(ACCOUNT_ID,
			new Asset(new ApiNftId(TOKEN_ID, SERIAL_NUMBER), PRICE));

	final URI uri = UriBuilder.of(integrationApiVersion).path(OffsetPurchaseController.BUYER).build();

	@Test
	void purchaseOffset() throws IOException {
		try (BlockingHttpClient blockingClient = client.toBlocking()) {
			HttpResponse<OffsetPurchaseResponse> response = blockingClient.exchange(HttpRequest.POST(uri, request),
					OffsetPurchaseResponse.class);
			assertAll(() -> assertEquals(HttpStatus.ACCEPTED, response.getStatus()),
					() -> assertEquals(LOCATION, response.header(HttpHeaders.LOCATION)),
					() -> assertEquals("PENDING", response.body().getStatus()));

			verify(marketplaceBuyerService, times(1)).makeAsyncPurchase(eq(request));
		}
	}

	@Test
	void test_purchaseOffset_BadRequest() {
		HttpClientResponseException thrown = Assertions.assertThrows(HttpClientResponseException.class,
				() -> client.toBlocking().exchange(HttpRequest.POST(uri, null), Argument.of(Object.class),
						Argument.of(ApiError.class)));

		assertNotNull(thrown);
		Optional<ApiError> errorResponse = thrown.getResponse().getBody(Argument.of(ApiError.class));
		assertTrue(errorResponse.isPresent());
		Assertions.assertEquals(ErrorCode.HTTP_STATUS_400_ERROR_1003, errorResponse.get());

		Mockito.verify(marketplaceBuyerService, never()).makeAsyncPurchase(any());
	}

}
