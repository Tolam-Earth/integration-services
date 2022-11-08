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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hedera.hashgraph.sdk.*;
import com.tolamearth.integration.hedera.HederaConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class ListingTransactionService {

	private static final Logger log = LoggerFactory.getLogger(ListingTransactionService.class);

	private final HederaConfiguration hederaConfigurationProperties;

	private final Client hederaClient;

	public ListingTransactionService(HederaConfiguration hederaConfigurationProperties, Client hederaClient) {
		this.hederaConfigurationProperties = hederaConfigurationProperties;
		this.hederaClient = hederaClient;
	}

	public ContractExecuteTransaction createContractExecuteTransaction(String functionName) {
		String contractId = hederaConfigurationProperties.offsetsContractId();
		Long contractFees = hederaConfigurationProperties.gasAmount();
		log.info("Using contract ID {} (\"{}\") with {} hbar in fees", contractId, functionName, contractFees);
		return new ContractExecuteTransaction().setContractId(ContractId.fromString(contractId))
				.setGas(hederaConfigurationProperties.gasAmount());
	}

	protected final String toSolidityAddress(String tokenId) {
		return TokenId.fromString(tokenId).toSolidityAddress();
	}

	public ContractExecuteTransaction buildAndFreezeListTransaction(String accountId, List<PricedNft> pricedNfts) {
		ContractExecuteTransaction transaction = createContractExecuteTransaction("list_offset");
		var tokenIds = pricedNfts.stream().map(PricedNft::tokenId).map(this::toSolidityAddress).toArray(String[]::new);
		var prices = pricedNfts.stream().map(PricedNft::price).map(BigInteger::valueOf).toArray(BigInteger[]::new);
		var serialNumbers = pricedNfts.stream().mapToLong(PricedNft::serialNumber).toArray();
		transaction.setFunction("list_offset",
				new ContractFunctionParameters().addAddress(AccountId.fromString(accountId).toSolidityAddress())
						.addAddressArray(tokenIds).addInt64Array(serialNumbers).addUint256Array(prices));
		transaction.freezeWith(hederaClient);
		return transaction;
	}

	public <T extends Transaction<T>> void executeContract(Transaction<T> transaction) {
		log.info("Attempting to execute {}", transaction.getTransactionId());
		try {
			TransactionResponse response = transaction.execute(hederaClient);
			log.info(response.getReceipt(hederaClient).toString());
			log.info("Successfully executed {}", transaction.getTransactionId());
		}
		catch (TimeoutException | PrecheckStatusException | ReceiptStatusException e) {
			throw new RuntimeException(e);
		}
	}

	record PricedNft(@JsonProperty("token_id") String tokenId, @JsonProperty("serial_number") Long serialNumber,
			@JsonProperty("price") Long price) {

		public Nft withoutPrice() {
			return new Nft(tokenId, serialNumber);
		}
	}

	record Nft(@JsonProperty("token_id") String tokenId, @JsonProperty("serial_number") Long serialNumber) {
	}

}