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
import com.tolamearth.integration.api.ApiNftId;
import com.tolamearth.integration.api.purchase.Asset;
import com.tolamearth.integration.api.purchase.OffsetPurchaseRequest;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class HederaTransactionServiceTest {

	Client mockHederaClient = mock(Client.class);

	final String ACCOUNT_ID = "0.0.48483625";

	final String TOKEN_ID = "0.0.48478161";

	final String SERIAL_NUMBER = "3456";

	final long PRICE = 10L;

	final OffsetPurchaseRequest request = new OffsetPurchaseRequest(ACCOUNT_ID,
			new Asset(new ApiNftId(TOKEN_ID, SERIAL_NUMBER), PRICE));

	final String NETWORK = "testnet";

	final String OPERATOR_ID = "0.0.48483625";

	final String OFFSETS_CONTRACT_ID = "0.0.48478161";

	final String PRIVATE_KEY = "a78269392d128e95ffc8488bdcd9986147c34b1455666e9b0940f66911a48dc4";

	final long GAS = 1_000_000L;

	HederaTransactionService hederaTransactionService = new HederaTransactionService(
			new HederaConfiguration(NETWORK, OPERATOR_ID, PRIVATE_KEY, OFFSETS_CONTRACT_ID, GAS), mockHederaClient);

	@Test
	void test_generateContractTransaction() {
		BigInteger expectedConversionRate = BigInteger.ONE;
		long payableAmount = expectedConversionRate.multiply(BigInteger.valueOf(request.getAsset().getPrice()))
				.longValue();
		Hbar expectedPayableAmountHbar = Hbar.fromTinybars(payableAmount);

		ContractExecuteTransaction contractExecuteTransaction = hederaTransactionService
				.generateContractTransaction(request, expectedConversionRate);

		assertNotNull(contractExecuteTransaction.getContractId());
		assertEquals(OFFSETS_CONTRACT_ID, contractExecuteTransaction.getContractId().toString());
		assertEquals(expectedPayableAmountHbar, contractExecuteTransaction.getPayableAmount());
		assertEquals(GAS, contractExecuteTransaction.getGas());
		assertNotNull(contractExecuteTransaction.getFunctionParameters());
	}

	@Test
	void test_executeTransaction() throws PrecheckStatusException, TimeoutException, ReceiptStatusException {
		ContractExecuteTransaction mockTransaction = mock(ContractExecuteTransaction.class);
		TransactionResponse mockResponse = mock(TransactionResponse.class);
		TransactionReceipt mockReceipt = mock(TransactionReceipt.class);

		when(mockTransaction.execute(eq(mockHederaClient))).thenReturn(mockResponse);
		when(mockResponse.getReceipt(eq(mockHederaClient))).thenReturn(mockReceipt);
		when(mockReceipt.toString()).thenReturn("mock-receipt");

		hederaTransactionService.executeTransaction(mockTransaction);

		verify(mockTransaction, times(1)).execute(eq(mockHederaClient));
		verify(mockResponse, times(1)).getReceipt(eq(mockHederaClient));
	}

}
