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

package com.tolamearth.integration.marketplace.buyer;

import com.hedera.hashgraph.sdk.ContractExecuteTransaction;
import com.hedera.hashgraph.sdk.TransactionId;
import com.tolamearth.integration.api.ApiNftId;
import com.tolamearth.integration.api.purchase.Asset;
import com.tolamearth.integration.api.purchase.OffsetPurchaseRequest;
import com.tolamearth.integration.hedera.TransactionService;
import com.tolamearth.integration.ledgerworks.discovery.TokenDiscoveryScheduler;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.publisher.TestPublisher;
import java.math.BigInteger;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

@MicronautTest
class MarketplaceBuyerServiceTest {

	@Inject
	MarketplaceBuyerService marketplaceBuyerService;

	@Inject
	ConversionClient conversionClient;

	@Inject
	PurchaseOffsetClient purchaseOffsetClient;

	@Inject
	TransactionService transactionService;

	@Inject
	TokenDiscoveryScheduler tokenDiscoveryScheduler;

	final String ACCOUNT_ID = "0.0.1234";

	final String TOKEN_ID = "0.0.2345";

	final String SERIAL_NUMBER = "3456";

	final long PRICE = 10L;

	final OffsetPurchaseRequest request = new OffsetPurchaseRequest(ACCOUNT_ID,
			new Asset(new ApiNftId(TOKEN_ID, SERIAL_NUMBER), PRICE));

	@Test
	void test_makePurchase() {
		BigInteger expectedRate = BigInteger.valueOf(1);

		ContractExecuteTransaction contract = mock(ContractExecuteTransaction.class);
		TransactionId transactionId = mock(TransactionId.class);

		when(contract.getTransactionId()).thenReturn(transactionId);
		when(transactionId.toString()).thenReturn("hedera-transaction-id");
		when(conversionClient.getTinybarToCents())
				.thenReturn(Mono.just(new ConversionClient.ConversionRateResponse(expectedRate)));
		when(transactionService.generateContractTransaction(eq(request), eq(expectedRate))).thenReturn(contract);
		when(purchaseOffsetClient.purchaseNfts(any())).thenReturn(Mono.just(new PurchaseOffsetClient.PurchaseResponse(
				new PurchaseOffsetClient.PurchaseRequest("transaction-id", "account-id", new ArrayList<>()))));

		marketplaceBuyerService.makeAsyncPurchase(request);

		verify(conversionClient, times(1)).getTinybarToCents();
		verify(transactionService, times(1)).generateContractTransaction(eq(request), eq(expectedRate));
		verify(purchaseOffsetClient, times(1)).purchaseNfts(any());
	}

	@Test
	void test_purchaseOffset_MonoException() {
		var conversionResponse = TestPublisher.<ConversionClient.ConversionRateResponse>create()
				.next(new ConversionClient.ConversionRateResponse(BigInteger.ONE))
				.error(new RuntimeException("uh oh!"));

		when(conversionClient.getTinybarToCents()).thenReturn(conversionResponse.mono());

		marketplaceBuyerService.makeAsyncPurchase(request);

		verify(conversionClient, times(1)).getTinybarToCents();
		verify(transactionService, never()).generateContractTransaction(any(), any());
		verify(purchaseOffsetClient, never()).purchaseNfts(any());
	}

	@MockBean
	@Replaces(ConversionClient.class)
	ConversionClient conversionClient() {
		return mock(ConversionClient.class);
	}

	@MockBean
	@Replaces(PurchaseOffsetClient.class)
	PurchaseOffsetClient purchaseOffsetClient() {
		return mock(PurchaseOffsetClient.class);
	}

	@MockBean(TransactionService.class)
	TransactionService transactionService() {
		return mock(TransactionService.class);
	}

	@MockBean(TokenDiscoveryScheduler.class)
	TokenDiscoveryScheduler tokenDiscoveryScheduler() {
		return mock(TokenDiscoveryScheduler.class);
	}

}
