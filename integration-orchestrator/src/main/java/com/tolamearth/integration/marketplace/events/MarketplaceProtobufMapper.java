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

package com.tolamearth.integration.marketplace.events;

import com.google.protobuf.Timestamp;
import com.tolamearth.integration.core.assets.*;
import com.tolamearth.integration.marketplace.MarketplaceEvent;
import lombok.NonNull;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MarketplaceProtobufMapper {

	private MarketplaceProtobufMapper() {
	}

	public static List<Token> fromMarketplaceEvent(@NonNull MarketplaceEvent event) {
		Objects.requireNonNull(event.getTransactionsList(), "Transactions List must not be null.");
		if (event.getTransactionsList().isEmpty()) {
			throw new IllegalStateException("Marketplace Event has no transactions.");
		}
		return event.getTransactionsList().stream().map(transaction -> {
			TokenTransaction tokenTransaction = TokenTransaction.builder()
					.id(new TokenTransactionId(fromMarketPlaceNftId(transaction.getNftId()),
							transaction.getTransactionId()))
					.eventType(fromMarketPlaceEventType(event.getEventType()))
					.transactionTime(fromTimestamp(transaction.getTransactionTime())).owner(transaction.getOwner())
					.listPrice(transaction.getListPrice()).purchasePrice(transaction.getPurchasePrice()).build();

			return Token.builder()
					.nftId(new NftId(transaction.getNftId().getTokenId(), transaction.getNftId().getSerialNumber()))
					.build().addTransaction(tokenTransaction);
		}).collect(Collectors.toList());
	}

	public static EventType fromMarketPlaceEventType(MarketplaceEvent.EventType eventType) {
		return switch (eventType) {
			case LISTED -> EventType.LISTED;
			case PURCHASED -> EventType.PURCHASED;
			default -> throw new IllegalStateException("Unexpected event type value: " + eventType);
		};
	}

	public static NftId fromMarketPlaceNftId(MarketplaceEvent.NftId marketplaceId) {
		return new NftId(marketplaceId.getTokenId(), marketplaceId.getSerialNumber());
	}

	public static String fromTimestamp(Timestamp timestamp) {
		return timestamp.getSeconds() + "." + timestamp.getNanos();
	}

}
