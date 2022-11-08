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

package com.tolamearth.integration.hedera;

import com.hedera.hashgraph.sdk.*;
import com.tolamearth.integration.api.purchase.OffsetPurchaseRequest;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.util.concurrent.TimeoutException;

@Slf4j
@Singleton
@RequiredArgsConstructor
public class HederaTransactionService implements TransactionService {

	private final HederaConfiguration hederaConfig;

	private final Client hederaClient;

	/**
	 * Create a purchase transaction directly with Hedera network
	 */
	public ContractExecuteTransaction generateContractTransaction(OffsetPurchaseRequest request, BigInteger rate) {
		log.info("Generating Hedera transaction.");
		log.info("Conversion rate: {}", rate);

		long price = request.getAsset().getPrice();
		long payableAmount = rate.multiply(BigInteger.valueOf(price)).longValue();

		return new ContractExecuteTransaction().setContractId(ContractId.fromString(hederaConfig.offsetsContractId()))
				.setFunction("purchase_offset", new ContractFunctionParameters()
						.addAddress(AccountId.fromString(request.getAccountId()).toSolidityAddress())
						.addAddressArray(new String[] {
								TokenId.fromString(request.getAsset().getNftId().getTokenId()).toSolidityAddress() })
						.addInt64Array(new long[] { Long.parseLong(request.getAsset().getNftId().getSerialNumber()) }))
				.setPayableAmount(Hbar.fromTinybars(payableAmount)).setGas(hederaConfig.gasAmount());
	}

	/**
	 * Execute the purchase contract (calls purchase_offset contract function directly)
	 */
	public void executeTransaction(ContractExecuteTransaction transaction) {
		transaction.signWithOperator(hederaClient);
		log.info("Attempting to execute Hedera transaction {}", transaction.getTransactionId());
		try {
			TransactionResponse response = transaction.execute(hederaClient);
			log.info(response.getReceipt(hederaClient).toString());
			log.info("Successfully executed {}", transaction.getTransactionId());
		}
		catch (TimeoutException | PrecheckStatusException | ReceiptStatusException e) {
			throw new RuntimeException(e);
		}
	}

	public void freezeTransaction(ContractExecuteTransaction transaction) {
		transaction.freezeWith(hederaClient);
	}

}