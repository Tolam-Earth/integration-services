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

package com.tolamearth.integration.marketplace;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.StartupEvent;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Bean
@Requires(env = "local")
public class MockMessagePublisher {

	private final MarketplaceEvent listedEvent;

	private final MarketplaceEvent purchasedEvent;

	@Inject
	private final MarketplaceMessageChannels messageChannels;

	public MockMessagePublisher(MarketplaceMessageChannels messageChannels) {
		this.messageChannels = messageChannels;
		MarketplaceEvent.Transaction tx1 = MarketplaceEvent.Transaction.newBuilder().setTransactionId("1")
				.setNftId(MarketplaceEvent.NftId.newBuilder().setTokenId("1.2.3").setSerialNumber("1"))
				.setListPrice(500).setPurchasePrice(0).build();
		this.listedEvent = MarketplaceEvent.newBuilder().setEventType(MarketplaceEvent.EventType.LISTED)
				.addTransactions(tx1).build();
		MarketplaceEvent.Transaction tx2 = MarketplaceEvent.Transaction.newBuilder().setTransactionId("2")
				.setNftId(MarketplaceEvent.NftId.newBuilder().setTokenId("1.2.3").setSerialNumber("1"))
				.setListPrice(500).setPurchasePrice(500).build();
		this.purchasedEvent = MarketplaceEvent.newBuilder().setEventType(MarketplaceEvent.EventType.PURCHASED)
				.addTransactions(tx2).build();
	}

	// Uncomment the below annotation just to verify that MarketplaceEventListener is
	// working
	// @EventListener
	public void onStartupEvent(StartupEvent event) {
		log.info("PUBLISHING MOCK MARKETPLACE MESSAGES");
		log.info("SENDING LISTED EVENT");
		messageChannels.sendMarketplaceMessage(this.listedEvent.toByteArray())
				.subscribe(s -> log.info("SUCCESSFULLY SENT LISTED EVENT {}", s));
		log.info("SENDING PURCHASED EVENT");
		messageChannels.sendMarketplaceMessage(this.purchasedEvent.toByteArray())
				.subscribe(s -> log.info("SUCCESSFULLY SENT PURCHASED EVENT {}", s));
	}

}
