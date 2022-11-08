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

package com.tolamearth.integration.armm;

import com.google.protobuf.Timestamp;
import com.tolamearth.integration.core.assets.EventType;
import com.tolamearth.integration.core.assets.Token;
import com.tolamearth.integration.core.assets.TokenTransaction;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
public class ArmmProtobufMapper {

	private ArmmProtobufMapper() {
	}

	public static ArmmEvent fromMintedToken(Token token) {
		validateToken(token);
		TokenTransaction tokenTransaction = token.getTransactions().get(0);
		Timestamp transactionTime = getTimestamp(tokenTransaction);

		ArmmEvent.TokenDetail detail = ArmmEvent.TokenDetail.newBuilder().setOwner(tokenTransaction.getOwner())
				.setCountry(token.getCountry()).setDeviceId(token.getDeviceId()).setGuardianId(token.getGuardianId())
				.setFirstSubdivision(token.getFirstSubdivision()).setProjectCategory(token.getProjectCategory())
				.setProjectType(token.getProjectType()).setVintageYear(token.getVintageYear()).build();

		ArmmEvent.Transaction transaction = ArmmEvent.Transaction.newBuilder()
				.setEventType(fromTokenTransactionEventType(tokenTransaction.getEventType()))
				.setNftId(ArmmEvent.NftId.newBuilder().setTokenId(token.getNftId().getTokenId())
						.setSerialNumber(token.getNftId().getSerialNumber()).build())
				.setTransactionId(tokenTransaction.getId().getTransactionId()).setTransactionTime(transactionTime)
				.setTokenDetail(detail).build();

		return ArmmEvent.newBuilder().addTransactions(transaction).build();
	}

	public static ArmmEvent fromMarketplaceToken(Token token) {
		validateToken(token);
		TokenTransaction tokenTransaction = token.getTransactions().get(0);
		Timestamp transactionTime = getTimestamp(tokenTransaction);

		ArmmEvent.TokenState tokenState = ArmmEvent.TokenState.newBuilder().setOwner(tokenTransaction.getOwner())
				.setListingPrice(tokenTransaction.getListPrice()).setPurchasePrice(tokenTransaction.getPurchasePrice())
				.build();

		ArmmEvent.Transaction transaction = ArmmEvent.Transaction.newBuilder()
				.setEventType(fromTokenTransactionEventType(tokenTransaction.getEventType()))
				.setNftId(ArmmEvent.NftId.newBuilder().setTokenId(token.getNftId().getTokenId())
						.setSerialNumber(token.getNftId().getSerialNumber()).build())
				.setTransactionId(tokenTransaction.getId().getTransactionId()).setTransactionTime(transactionTime)
				.setTokenState(tokenState).build();

		return ArmmEvent.newBuilder().addTransactions(transaction).build();
	}

	private static Timestamp getTimestamp(TokenTransaction tokenTransaction) {
		String[] timeParts = tokenTransaction.getTransactionTime().split("\\.");
		if (timeParts.length < 2) {
			log.error("{} token has a malformed timestamp: {}", tokenTransaction.getEventType(),
					tokenTransaction.getToken());
			throw new IllegalStateException(tokenTransaction + " has a malformed timestamp");
		}

		return Timestamp.newBuilder().setSeconds(Long.valueOf(timeParts[0])).setNanos(Integer.valueOf(timeParts[1]))
				.build();
	}

	private static ArmmEvent.EventType fromTokenTransactionEventType(EventType eventType) {
		return switch (eventType) {
			case MINTED -> ArmmEvent.EventType.MINTED;
			case LISTED -> ArmmEvent.EventType.LISTED;
			case PURCHASED -> ArmmEvent.EventType.PURCHASED;
			default -> throw new IllegalStateException("Unexpected event type value: " + eventType);
		};
	}

	private static void validateToken(Token token) {
		Objects.requireNonNull(token);
		Objects.requireNonNull(token.getTransactions());
		if (token.getTransactions().size() != 1) {
			log.error("MINTED token is in an unexpected state: {}", token);
			throw new IllegalStateException("MINTED token is in an unexpected state.");
		}
	}

}
