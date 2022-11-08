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

package com.tolamearth.integration.orchestrator;

import com.tolamearth.integration.core.assets.Token;
import com.tolamearth.integration.core.assets.TokenRepository;
import com.tolamearth.integration.marketplace.events.EventService;
import com.tolamearth.integration.marketplace.events.MarketplaceEventListener;
import com.tolamearth.integration.marketplace.events.MarketplaceEventService;
import com.tolamearth.integration.marketplace.events.MarketplaceProtobufMapper;
import com.tolamearth.integration.marketplace.MarketplaceEvent;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
@MicronautTest
public class TransactionPublisherServiceDBFunctionalTest {

	@Inject
	TransactionPublisherService publisherService;

	@Inject
	TokenRepository tokenRepository;

	private Flux<MarketplaceEvent> testFlux;

	private MarketplaceEvent listedEvent;

	private MarketplaceEvent purchasedEvent;

	@Test
	void testPublisherService() {
		Token listedToken = MarketplaceProtobufMapper.fromMarketplaceEvent(listedEvent).get(0);
		Token purchasedToken = MarketplaceProtobufMapper.fromMarketplaceEvent(purchasedEvent).get(0);
		Token[] tokens = { listedToken, purchasedToken };

		StepVerifier.create(publisherService.getTransactionStream()).expectNext(tokens).verifyComplete();

		Token storedToken1 = tokenRepository.findById(listedToken.getNftId()).block();
		Assertions.assertEquals(2, storedToken1.getTransactions().size());

		Long tokenCount = tokenRepository.findAll().count().block();
		Assertions.assertEquals(1, tokenCount);
	}

	@MockBean(MarketplaceEventService.class)
	public EventService getMockMarketplaceService() {
		MarketplaceEvent.Transaction tx1 = MarketplaceEvent.Transaction.newBuilder().setTransactionId("1")
				.setNftId(MarketplaceEvent.NftId.newBuilder().setTokenId("1.2.3").setSerialNumber("1"))
				.setListPrice(500).setPurchasePrice(0).build();
		listedEvent = MarketplaceEvent.newBuilder().setEventType(MarketplaceEvent.EventType.LISTED).addTransactions(tx1)
				.build();
		MarketplaceEvent.Transaction tx2 = MarketplaceEvent.Transaction.newBuilder().setTransactionId("2")
				.setNftId(MarketplaceEvent.NftId.newBuilder().setTokenId("1.2.3").setSerialNumber("1"))
				.setListPrice(500).setPurchasePrice(500).build();
		purchasedEvent = MarketplaceEvent.newBuilder().setEventType(MarketplaceEvent.EventType.PURCHASED)
				.addTransactions(tx2).build();

		EventService service = mock(EventService.class);
		this.testFlux = Flux.just(listedEvent, purchasedEvent);
		when(service.getMarketplaceEvents()).thenReturn(testFlux);
		return service;
	}

	@MockBean(MarketplaceEventListener.class)
	public MarketplaceEventListener getMockListener() {
		return mock(MarketplaceEventListener.class);
	}

}
