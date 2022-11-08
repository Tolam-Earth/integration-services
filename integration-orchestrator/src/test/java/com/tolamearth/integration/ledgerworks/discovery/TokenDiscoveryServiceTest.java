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

package com.tolamearth.integration.ledgerworks.discovery;

import com.tolamearth.integration.ledgerworks.http.LedgerWorksClient;
import com.tolamearth.integration.ledgerworks.data.TokenResponse;
import com.tolamearth.integration.ledgerworks.data.Transaction;
import com.tolamearth.integration.ledgerworks.data.TransactionsResponse;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Slf4j
@MicronautTest(rebuildContext = true)
public class TokenDiscoveryServiceTest {

	@Inject
	PollingTokenDiscoveryService tokenDiscoveryService;

	@Inject
	LedgerWorksClient ledgerWorksClient;

	@MockBean(LedgerWorksClient.class)
	LedgerWorksClient ledgerWorksClient() {
		return mock(LedgerWorksClient.class);
	}

	@Inject
	TokenDiscoveryScheduler tokenDiscoveryScheduler;

	@MockBean(TokenDiscoveryScheduler.class)
	TokenDiscoveryScheduler tokenDiscoveryScheduler() {
		return mock(TokenDiscoveryScheduler.class);
	}

	@Test
	void test_discoverTokens() {

		Transaction expectedTransaction1 = Transaction.builder().transactionId("test-transaction-id-1")
				.name("TOKENMINT").build();
		Transaction expectedTransaction2 = Transaction.builder().transactionId("test-transaction-id-2")
				.name("TOKENMINT").build();

		when(ledgerWorksClient.getToken(anyString())).thenReturn(Mono.just(new TokenResponse("test-account-id")));

		when(ledgerWorksClient.getTransactions(any(), any(), any(), any())).thenReturn(Mono.just(TransactionsResponse
				.builder().transactions(List.of(Transaction.builder().transactionId("test-transaction-id-1").build(),
						Transaction.builder().transactionId("test-transaction-id-2").build()))
				.build()));

		when(ledgerWorksClient.getTransaction(eq("test-transaction-id-1"))).thenReturn(
				Mono.just(TransactionsResponse.builder().transactions(List.of(expectedTransaction1)).build()));

		when(ledgerWorksClient.getTransaction(eq("test-transaction-id-2"))).thenReturn(
				Mono.just(TransactionsResponse.builder().transactions(List.of(expectedTransaction2)).build()));

		StepVerifier.create(tokenDiscoveryService.getMintedTransactions())
				.then(tokenDiscoveryService::discoverMintedTokens).expectNext(expectedTransaction1)
				.expectNext(expectedTransaction2).thenCancel().verify();
	}

	@Test
	void test_discoverTokens_emptyTransactionsResponse() {
		when(ledgerWorksClient.getToken(anyString())).thenReturn(Mono.just(new TokenResponse("test-account-id")));

		when(ledgerWorksClient.getTransactions(any(), any(), any(), any())).thenReturn(Mono.empty());

		StepVerifier.create(tokenDiscoveryService.getMintedTransactions())
				.then(tokenDiscoveryService::discoverMintedTokens).expectNextCount(0).thenCancel().verify();

		verify(ledgerWorksClient, never()).getTransaction(anyString());
	}

	@Test
	void test_discoverTokens_emptyTransactionResponse() {
		when(ledgerWorksClient.getToken(anyString())).thenReturn(Mono.just(new TokenResponse("test-account-id")));

		when(ledgerWorksClient.getTransactions(any(), any(), any(), any())).thenReturn(Mono.just(TransactionsResponse
				.builder().transactions(List.of(Transaction.builder().transactionId("test-transaction-id-1").build(),
						Transaction.builder().transactionId("test-transaction-id-2").build()))
				.build()));

		when(ledgerWorksClient.getTransaction(anyString())).thenReturn(Mono.empty());

		StepVerifier.create(tokenDiscoveryService.getMintedTransactions())
				.then(tokenDiscoveryService::discoverMintedTokens).expectNextCount(0).thenCancel().verify();
	}

	@Test
	void test_discoverTokens_getTokenException() {
		TestPublisher<TokenResponse> exceptionPublisher = TestPublisher.<TokenResponse>create()
				.next(new TokenResponse("test-account-id")).error(new RuntimeException("uh oh!"));

		when(ledgerWorksClient.getToken(anyString())).thenReturn(exceptionPublisher.mono());

		StepVerifier.create(tokenDiscoveryService.getMintedTransactions())
				.then(tokenDiscoveryService::discoverMintedTokens).expectNextCount(0).thenCancel().verify();

		verify(ledgerWorksClient, never()).getTransactions(any(), any(), any(), any());
		verify(ledgerWorksClient, never()).getTransaction(anyString());
	}

	@Test
	void test_discoverTokens_getTransactionsException() {
		TestPublisher<TransactionsResponse> exceptionPublisher = TestPublisher.<TransactionsResponse>create()
				.next(TransactionsResponse.builder()
						.transactions(List.of(Transaction.builder().transactionId("test-transaction-id-1").build()))
						.build())
				.error(new RuntimeException("uh oh!"));

		when(ledgerWorksClient.getToken(anyString())).thenReturn(Mono.just(new TokenResponse("test-account-id")));

		when(ledgerWorksClient.getTransactions(any(), any(), any(), any())).thenReturn(exceptionPublisher.mono());

		StepVerifier.create(tokenDiscoveryService.getMintedTransactions())
				.then(tokenDiscoveryService::discoverMintedTokens).expectNextCount(0).thenCancel().verify();

		verify(ledgerWorksClient, never()).getTransaction(anyString());
	}

	@Test
	void test_discoverTokens_getTransactionException() {
		TestPublisher<TransactionsResponse> exceptionPublisher = TestPublisher.<TransactionsResponse>create()
				.next(TransactionsResponse.builder()
						.transactions(List.of(Transaction.builder().transactionId("test-transaction-id-1").build()))
						.build())
				.error(new RuntimeException("uh oh!"));

		when(ledgerWorksClient.getToken(anyString())).thenReturn(Mono.just(new TokenResponse("test-account-id")));

		when(ledgerWorksClient.getTransactions(any(), any(), any(), any())).thenReturn(Mono.just(TransactionsResponse
				.builder().transactions(List.of(Transaction.builder().transactionId("test-transaction-id-1").build(),
						Transaction.builder().transactionId("test-transaction-id-2").build()))
				.build()));

		when(ledgerWorksClient.getTransaction(eq("test-transaction-id-1"))).thenReturn(exceptionPublisher.mono());

		StepVerifier.create(tokenDiscoveryService.getMintedTransactions())
				.then(tokenDiscoveryService::discoverMintedTokens).expectNextCount(0).thenCancel().verify();
	}

}
