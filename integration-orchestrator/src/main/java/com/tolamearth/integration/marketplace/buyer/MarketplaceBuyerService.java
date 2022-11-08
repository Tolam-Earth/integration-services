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
import com.tolamearth.integration.api.purchase.OffsetPurchaseRequest;
import com.tolamearth.integration.hedera.TransactionService;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Singleton
@RequiredArgsConstructor
public class MarketplaceBuyerService implements BuyerService {

	private final PurchaseOffsetClient purchaseOffsetClient;

	private final ConversionClient conversionClient;

	private final TransactionService transactionService;

	@Override
	public void makeAsyncPurchase(OffsetPurchaseRequest request) {
		conversionClient.getTinybarToCents()
				.map(conversionRateResponse -> transactionService.generateContractTransaction(request,
						conversionRateResponse.rate()))
				.flatMap(transaction -> marketplacePurchaseOffset(request, transaction)).map(transaction -> {
					transactionService.executeTransaction(transaction);
					return transaction;
				}).doOnSuccess(transaction -> log.info("Successfully executed NFT purchase."))
				.doOnError(error -> log.error("Error making NFT purchase. Exception: ", error)).subscribe();
	}

	private Mono<ContractExecuteTransaction> marketplacePurchaseOffset(OffsetPurchaseRequest request,
			ContractExecuteTransaction transaction) {
		final var asset = request.getAsset();
		final var nftId = asset.getNftId();
		final var accountId = request.getAccountId();

		transactionService.freezeTransaction(transaction); // Must freeze transaction
															// before getting transaction
															// id.
		final var transactionId = transaction.getTransactionId().toString();

		final var nft = new PurchaseOffsetClient.Nft(nftId.getTokenId(), Long.parseLong(nftId.getSerialNumber()));
		final var purchaseRequest = new PurchaseOffsetClient.PurchaseRequest(transactionId, accountId, List.of(nft));

		log.info("Purchase request to marketplace: {}", purchaseRequest);

		return purchaseOffsetClient.purchaseNfts(purchaseRequest).thenReturn(transaction);
	}

}
