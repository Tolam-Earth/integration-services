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

package com.tolamearth.integration.api.history;

import com.tolamearth.integration.api.ApiError;
import com.tolamearth.integration.api.ErrorCode;
import com.tolamearth.integration.core.assets.*;
import com.tolamearth.integration.ledgerworks.discovery.TokenDiscoveryScheduler;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest
@Property(name = "api.integration-api-version", value = "/integration/v1")
public class OffsetHistoryControllerTest {

	@Inject
	@Client("/")
	HttpClient client;

	@Inject
	TokenRepository tokenRepository;

	@MockBean
	@Replaces(TokenRepository.class)
	TokenRepository tokenRepository() {
		return mock(TokenRepository.class);
	}

	@Inject
	TokenDiscoveryScheduler tokenDiscoveryScheduler;

	@MockBean(TokenDiscoveryScheduler.class)
	TokenDiscoveryScheduler tokenDiscoveryScheduler() {
		return mock(TokenDiscoveryScheduler.class);
	}

	final String OFFSET_HISTORY_URI_TEMPLATE = "/integration/v1/offsets/%s,%s/transactions";

	@Test
	void test_getOffsetTransactionHistory() {
		String expectedTokenId = "test-token-id";
		String expectedSerialNum = "test-serial-num";
		int expectedTransactionSize = 2;

		TokenTransaction expectedTokenTransaction1 = TokenTransaction.builder().transactionTime("11111.11111")
				.eventType(EventType.LISTED).id(TokenTransactionId.builder()
						.nftId(new NftId(expectedTokenId, expectedSerialNum)).transactionId("transaction-id-1").build())
				.owner("owner-1").listPrice(100L).build();

		TokenTransaction expectedTokenTransaction2 = TokenTransaction.builder().transactionTime("2222.2222")
				.eventType(EventType.PURCHASED).id(TokenTransactionId.builder()
						.nftId(new NftId(expectedTokenId, expectedSerialNum)).transactionId("transaction-id-2").build())
				.owner("owner-2").purchasePrice(200L).build();

		List<TokenTransaction> expectedTransactions = List.of(expectedTokenTransaction1, expectedTokenTransaction2);

		when(tokenRepository.findById(any())).thenReturn(Mono.just(Token.builder()
				.nftId(new NftId(expectedTokenId, expectedSerialNum)).transactions(expectedTransactions).build()));

		String uri = String.format(OFFSET_HISTORY_URI_TEMPLATE, expectedTokenId, expectedSerialNum);
		OffsetHistoryResponse result = client.toBlocking().retrieve(HttpRequest.GET(uri), OffsetHistoryResponse.class);

		assertNotNull(result);
		assertEquals(expectedTokenId, result.getNftId().getTokenId());
		assertEquals(expectedSerialNum, result.getNftId().getSerialNumber());
		assertNotNull(result.getTransactions());
		assertEquals(expectedTransactionSize, result.getTransactions().size());

		for (int i = 0; i < expectedTransactionSize; i++) {
			assertEquals(expectedTransactions.get(i).getId().getTransactionId(),
					result.getTransactions().get(i).getTransactionId());
			assertEquals(expectedTransactions.get(i).getEventType().name(),
					result.getTransactions().get(i).getMsgType());
			assertEquals(expectedTransactions.get(i).getOwner(), result.getTransactions().get(i).getOwner());
			assertEquals(expectedTransactions.get(i).getTransactionTime(),
					result.getTransactions().get(i).getTransactionTime());
			if (expectedTransactions.get(i).getEventType().equals(EventType.LISTED)) {
				assertEquals(expectedTransactions.get(i).getListPrice(), result.getTransactions().get(i).getPrice());
			}
			else if (expectedTransactions.get(i).getEventType().equals(EventType.PURCHASED)) {
				assertEquals(expectedTransactions.get(i).getPurchasePrice(),
						result.getTransactions().get(i).getPrice());
			}

		}
	}

	// @Test
	void test_getOffsetTransactionHistory_filterMintedTransactions() {
		String expectedTokenId = "test-token-id";
		String expectedSerialNum = "test-serial-num";

		TokenTransaction expectedTokenTransaction1 = TokenTransaction.builder().transactionTime("11111.11111")
				.eventType(EventType.MINTED).id(TokenTransactionId.builder()
						.nftId(new NftId(expectedTokenId, expectedSerialNum)).transactionId("transaction-id-1").build())
				.owner("owner-1").listPrice(100L).build();

		when(tokenRepository.findById(any()))
				.thenReturn(Mono.just(Token.builder().nftId(new NftId(expectedTokenId, expectedSerialNum))
						.transactions(List.of(expectedTokenTransaction1)).build()));

		String uri = String.format(OFFSET_HISTORY_URI_TEMPLATE, expectedTokenId, expectedSerialNum);
		OffsetHistoryResponse result = client.toBlocking().retrieve(HttpRequest.GET(uri), OffsetHistoryResponse.class);

		assertNotNull(result);
		assertEquals(expectedTokenId, result.getNftId().getTokenId());
		assertEquals(expectedSerialNum, result.getNftId().getSerialNumber());
		;
		assertNull(result.getTransactions());
	}

	@Test
	void test_getOffsetTransactionHistory_BlankPathVariables() {
		String expectedTokenId = "";
		String expectedSerialNum = "";

		String uri = String.format(OFFSET_HISTORY_URI_TEMPLATE, expectedTokenId, expectedSerialNum);

		HttpClientResponseException thrown = Assertions.assertThrows(HttpClientResponseException.class, () -> client
				.toBlocking().retrieve(HttpRequest.GET(uri), Argument.of(Object.class), Argument.of(ApiError.class)));

		assertNotNull(thrown);
		Optional<ApiError> errorResponse = thrown.getResponse().getBody(Argument.of(ApiError.class));
		assertTrue(errorResponse.isPresent());
		Assertions.assertEquals(ErrorCode.HTTP_STATUS_400_ERROR_1003, errorResponse.get());
	}

	@Test
	void test_getOffsetTransactionHistory_NotFoundOnRepository() {
		String expectedTokenId = "test-token-id";
		String expectedSerialNum = "test-serial-num";

		when(tokenRepository.findById(any())).thenReturn(Mono.empty());
		String uri = String.format(OFFSET_HISTORY_URI_TEMPLATE, expectedTokenId, expectedSerialNum);

		HttpClientResponseException thrown = Assertions.assertThrows(HttpClientResponseException.class, () -> client
				.toBlocking().retrieve(HttpRequest.GET(uri), Argument.of(Object.class), Argument.of(ApiError.class)));

		assertNotNull(thrown);
		Optional<ApiError> errorResponse = thrown.getResponse().getBody(Argument.of(ApiError.class));
		assertTrue(errorResponse.isPresent());
		Assertions.assertEquals(ErrorCode.HTTP_STATUS_400_ERROR_1003, errorResponse.get());
	}

}
