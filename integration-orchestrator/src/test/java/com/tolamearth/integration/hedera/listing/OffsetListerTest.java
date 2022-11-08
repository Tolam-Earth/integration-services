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

package com.tolamearth.integration.hedera.listing;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.ContractExecuteTransaction;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.tolamearth.integration.hedera.HederaConfiguration;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
public class OffsetListerTest {

	private static final Logger log = LoggerFactory.getLogger(OffsetListerTest.class);

	@Inject
	@Client("http://localhost:7070/hem/v1")
	HttpClient client;

	String whitelist_list_uri = "/offsets/list";

	String tokenId = "0.0.48697573"; // your generated tokenId from nft seed script in
										// hem-smart-contracts

	long serialNumber = 5; // look up your token id on hashscan and grab a serial number
							// that has not yet been listed or purchased

	long price = 1; // lower the better so we don't spend much hbar

	String sellerAccountId = "0.0.48527136"; // the SELLER account id that was generated
												// by the hem-smart-contracts scripts

	String sellerPrivateKey = "302e020100300506032b657004220420d2c9c07d429a08fdda833f7913fd7ce36bf8b8128076dc90418cd287ddadf3ae"; // the
																																	// SELLER
																																	// private
																																	// key
																																	// that
																																	// was
																																	// generated
																																	// by
																																	// the
																																	// hem-smart-contracts
																																	// scripts

	@Value("${hedera.adminOperatorId}")
	String operatorAccountId;

	@Value("${hedera.adminPrivateKey}")
	String operatorPrivateKey;

	@Value("${hedera.offsetsContractId}")
	String smartContractId;

	@Value("${hedera.gasAmount}")
	Long gas;

	@Test
	@Disabled("Only enable this when needing to set up Listings for integration testing.")
	void whiteListOffset() {
		List<ListingTransactionService.PricedNft> nfts = List
				.of(new ListingTransactionService.PricedNft(this.tokenId, this.serialNumber, this.price));

		com.hedera.hashgraph.sdk.Client sellerClient = createHederaClient(this.sellerAccountId, this.sellerPrivateKey);
		ListingTransactionService sellerService = new ListingTransactionService(new HederaConfiguration("testnet",
				this.operatorAccountId, this.operatorPrivateKey, this.smartContractId, this.gas), sellerClient);
		ContractExecuteTransaction transaction = sellerService.buildAndFreezeListTransaction(this.sellerAccountId,
				nfts);

		WhitelistClient.PricedNft pricedNft = new WhitelistClient.PricedNft(this.tokenId, this.serialNumber,
				this.price);
		WhitelistClient.ListingRequest request = new WhitelistClient.ListingRequest(
				transaction.getTransactionId().toString(), this.sellerAccountId, List.of(pricedNft));

		HttpResponse<WhitelistClient.ListingResponse> whitelistResponse = client.toBlocking()
				.exchange(HttpRequest.POST(whitelist_list_uri, request), WhitelistClient.ListingResponse.class);

		assertEquals(HttpStatus.CREATED, whitelistResponse.getStatus());

		sellerService.executeContract(transaction);
	}

	com.hedera.hashgraph.sdk.Client createHederaClient(String id, String key) {
		AccountId operatorId = AccountId.fromString(id);
		PrivateKey privateKey = PrivateKey.fromString(key);
		com.hedera.hashgraph.sdk.Client hederaClient = com.hedera.hashgraph.sdk.Client.forTestnet();
		hederaClient.setOperator(operatorId, privateKey);
		return hederaClient;
	}

}