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

import com.tolamearth.integration.armm.ArmmMessageChannels;
import com.tolamearth.integration.core.assets.Token;
import com.tolamearth.integration.core.assets.TokenRepository;
import com.tolamearth.integration.marketplace.MarketplaceEvent;
import com.tolamearth.integration.marketplace.events.EventService;
import com.tolamearth.integration.marketplace.events.MarketplaceEventService;
import com.tolamearth.integration.marketplace.events.MarketplaceProtobufMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

public class TransactionPublisherServiceTest {

	TransactionPublisherService publisherService;

	EventService subscriberService;

	TokenRepository tokenRepository;

	ArmmMessageChannels messageChannels;

	private Flux<MarketplaceEvent> testFlux;

	@BeforeEach
	void initMocks() {
		this.tokenRepository = tokenRepository();
		this.subscriberService = subscriberService();
		this.messageChannels = messageChannels();
	}

	@Test
	void testNoTransactions() {
		MarketplaceEvent listedEvent = MarketplaceEvent.newBuilder().build();

		testFlux = Flux.just(listedEvent);
		when(this.subscriberService.getMarketplaceEvents()).thenReturn(testFlux);
		this.startPublisher();

		StepVerifier.create(this.publisherService.getTransactionStream()).verifyComplete();
	}

	@Test
	void testSingleListedEvent() {
		MarketplaceEvent.Transaction tx = MarketplaceEvent.Transaction.newBuilder().setTransactionId("1")
				.setNftId(MarketplaceEvent.NftId.newBuilder().setTokenId("1.2.3").setSerialNumber("1"))
				.setListPrice(500).setPurchasePrice(0).build();
		MarketplaceEvent listedEvent = MarketplaceEvent.newBuilder().setEventType(MarketplaceEvent.EventType.LISTED)
				.addTransactions(tx).build();

		Token expected = MarketplaceProtobufMapper.fromMarketplaceEvent(listedEvent).get(0);

		testFlux = Flux.just(listedEvent);
		when(this.subscriberService.getMarketplaceEvents()).thenReturn(testFlux);
		when(this.tokenRepository.saveOrUpdate(expected)).thenReturn(Mono.just(expected));
		when(this.messageChannels.sendNftDetails(any())).thenReturn(Mono.just("messageId1"));

		this.startPublisher();

		StepVerifier.create(this.publisherService.getTransactionStream()).expectNext(expected).verifyComplete();

		verify(this.tokenRepository).saveOrUpdate(expected);
		verify(this.messageChannels).sendNftDetails(notNull());
		verifyNoMoreInteractions(this.tokenRepository);
		verifyNoMoreInteractions(this.messageChannels);
	}

	@Test
	void testSinglePurchasedEvent() {
		MarketplaceEvent.Transaction tx = MarketplaceEvent.Transaction.newBuilder().setTransactionId("1")
				.setNftId(MarketplaceEvent.NftId.newBuilder().setTokenId("1.2.3").setSerialNumber("1"))
				.setListPrice(500).setPurchasePrice(500).build();
		MarketplaceEvent purchasedEvent = MarketplaceEvent.newBuilder()
				.setEventType(MarketplaceEvent.EventType.PURCHASED).addTransactions(tx).build();

		Token expected = MarketplaceProtobufMapper.fromMarketplaceEvent(purchasedEvent).get(0);

		testFlux = Flux.just(purchasedEvent);
		when(this.subscriberService.getMarketplaceEvents()).thenReturn(testFlux);
		when(this.tokenRepository.saveOrUpdate(expected)).thenReturn(Mono.just(expected));
		when(this.messageChannels.sendNftDetails(any())).thenReturn(Mono.just("messageId1"));

		this.startPublisher();

		StepVerifier.create(this.publisherService.getTransactionStream()).expectNext(expected).verifyComplete();

		verify(this.tokenRepository).saveOrUpdate(expected);
		verify(this.messageChannels).sendNftDetails(notNull());
		verifyNoMoreInteractions(this.tokenRepository);
		verifyNoMoreInteractions(this.messageChannels);
	}

	@Test
	void testMultipleEvents() {
		MarketplaceEvent.Transaction tx1 = MarketplaceEvent.Transaction.newBuilder().setTransactionId("1")
				.setNftId(MarketplaceEvent.NftId.newBuilder().setTokenId("1.2.3").setSerialNumber("1"))
				.setListPrice(500).setPurchasePrice(0).build();
		MarketplaceEvent listedEvent = MarketplaceEvent.newBuilder().setEventType(MarketplaceEvent.EventType.LISTED)
				.addTransactions(tx1).build();
		MarketplaceEvent.Transaction tx2 = MarketplaceEvent.Transaction.newBuilder().setTransactionId("1")
				.setNftId(MarketplaceEvent.NftId.newBuilder().setTokenId("1.2.3").setSerialNumber("1"))
				.setListPrice(500).setPurchasePrice(500).build();
		MarketplaceEvent purchasedEvent = MarketplaceEvent.newBuilder()
				.setEventType(MarketplaceEvent.EventType.PURCHASED).addTransactions(tx2).build();

		Token expectedListed = MarketplaceProtobufMapper.fromMarketplaceEvent(listedEvent).get(0);
		Token expectedPurchased = MarketplaceProtobufMapper.fromMarketplaceEvent(purchasedEvent).get(0);

		testFlux = Flux.just(listedEvent, purchasedEvent);
		when(this.subscriberService.getMarketplaceEvents()).thenReturn(testFlux);
		when(this.tokenRepository.saveOrUpdate(expectedListed)).thenReturn(Mono.just(expectedListed));
		when(this.tokenRepository.saveOrUpdate(expectedPurchased)).thenReturn(Mono.just(expectedPurchased));
		when(this.messageChannels.sendNftDetails(any())).thenReturn(Mono.just("messageId1"));

		this.startPublisher();

		StepVerifier.create(this.publisherService.getTransactionStream()).expectNext(expectedListed)
				.expectNext(expectedPurchased).verifyComplete();

		verify(this.tokenRepository).saveOrUpdate(expectedListed);
		verify(this.tokenRepository).saveOrUpdate(expectedPurchased);
		verify(this.messageChannels, times(2)).sendNftDetails(notNull());
		verifyNoMoreInteractions(this.tokenRepository);
		verifyNoMoreInteractions(this.messageChannels);
	}

	@Test
	void testMixedValidAndDBError() {
		MarketplaceEvent.Transaction errorTransaction = MarketplaceEvent.Transaction.newBuilder().build();
		MarketplaceEvent errorEvent = MarketplaceEvent.newBuilder().addTransactions(errorTransaction).build();
		MarketplaceEvent.Transaction tx1 = MarketplaceEvent.Transaction.newBuilder().setTransactionId("1")
				.setNftId(MarketplaceEvent.NftId.newBuilder().setTokenId("1.2.3").setSerialNumber("1"))
				.setListPrice(500).setPurchasePrice(0).build();
		MarketplaceEvent listedEvent = MarketplaceEvent.newBuilder().setEventType(MarketplaceEvent.EventType.LISTED)
				.addTransactions(tx1).build();
		MarketplaceEvent.Transaction tx2 = MarketplaceEvent.Transaction.newBuilder().setTransactionId("1")
				.setNftId(MarketplaceEvent.NftId.newBuilder().setTokenId("1.2.3").setSerialNumber("1"))
				.setListPrice(500).setPurchasePrice(500).build();
		MarketplaceEvent purchasedEvent = MarketplaceEvent.newBuilder()
				.setEventType(MarketplaceEvent.EventType.PURCHASED).addTransactions(tx2).build();

		Token expectedListed = MarketplaceProtobufMapper.fromMarketplaceEvent(listedEvent).get(0);
		Token expectedError = MarketplaceProtobufMapper.fromMarketplaceEvent(errorEvent).get(0);
		Token expectedPurchased = MarketplaceProtobufMapper.fromMarketplaceEvent(purchasedEvent).get(0);

		testFlux = Flux.just(listedEvent, errorEvent, purchasedEvent);
		when(this.subscriberService.getMarketplaceEvents()).thenReturn(testFlux);
		when(this.tokenRepository.saveOrUpdate(expectedListed)).thenReturn(Mono.just(expectedListed));
		when(this.tokenRepository.saveOrUpdate(expectedError)).thenThrow(new RuntimeException("DB Error"));
		when(this.tokenRepository.saveOrUpdate(expectedPurchased)).thenReturn(Mono.just(expectedPurchased));

		when(this.messageChannels.sendNftDetails(any())).thenReturn(Mono.just("messageId1"));

		this.startPublisher();

		StepVerifier.create(this.publisherService.getTransactionStream()).expectNext(expectedListed)
				.expectNext(expectedPurchased).verifyComplete();

		verify(this.tokenRepository).saveOrUpdate(expectedListed);
		verify(this.tokenRepository).saveOrUpdate(expectedError);
		verify(this.tokenRepository).saveOrUpdate(expectedPurchased);
		verify(this.messageChannels, times(2)).sendNftDetails(notNull());
		verifyNoMoreInteractions(this.tokenRepository);
		verifyNoMoreInteractions(this.messageChannels);
	}

	@Test
	void testMixedValidAndInvalid() {
		MarketplaceEvent errorEvent = MarketplaceEvent.newBuilder().build();
		MarketplaceEvent.Transaction tx1 = MarketplaceEvent.Transaction.newBuilder().setTransactionId("1")
				.setNftId(MarketplaceEvent.NftId.newBuilder().setTokenId("1.2.3").setSerialNumber("1"))
				.setListPrice(500).setPurchasePrice(0).build();
		MarketplaceEvent listedEvent = MarketplaceEvent.newBuilder().setEventType(MarketplaceEvent.EventType.LISTED)
				.addTransactions(tx1).build();
		MarketplaceEvent.Transaction tx2 = MarketplaceEvent.Transaction.newBuilder().setTransactionId("1")
				.setNftId(MarketplaceEvent.NftId.newBuilder().setTokenId("1.2.3").setSerialNumber("1"))
				.setListPrice(500).setPurchasePrice(500).build();
		MarketplaceEvent purchasedEvent = MarketplaceEvent.newBuilder()
				.setEventType(MarketplaceEvent.EventType.PURCHASED).addTransactions(tx2).build();

		Token expectedListed = MarketplaceProtobufMapper.fromMarketplaceEvent(listedEvent).get(0);
		Token expectedPurchased = MarketplaceProtobufMapper.fromMarketplaceEvent(purchasedEvent).get(0);

		testFlux = Flux.just(listedEvent, errorEvent, purchasedEvent);
		when(this.subscriberService.getMarketplaceEvents()).thenReturn(testFlux);
		when(this.tokenRepository.saveOrUpdate(expectedListed)).thenReturn(Mono.just(expectedListed));
		when(this.tokenRepository.saveOrUpdate(expectedPurchased)).thenReturn(Mono.just(expectedPurchased));

		when(this.messageChannels.sendNftDetails(any())).thenReturn(Mono.just("messageId1"));

		this.startPublisher();

		StepVerifier.create(this.publisherService.getTransactionStream()).expectNext(expectedListed)
				.expectNext(expectedPurchased).verifyComplete();

		verify(this.tokenRepository).saveOrUpdate(expectedListed);
		verify(this.tokenRepository).saveOrUpdate(expectedPurchased);
		verify(this.messageChannels, times(2)).sendNftDetails(notNull());
		verifyNoMoreInteractions(this.tokenRepository);
		verifyNoMoreInteractions(this.messageChannels);
	}

	@Test
	void testMixedValidAndDBInvalidReturn() {
		MarketplaceEvent.Transaction errorTransaction = MarketplaceEvent.Transaction.newBuilder().build();
		MarketplaceEvent errorEvent = MarketplaceEvent.newBuilder().addTransactions(errorTransaction).build();
		MarketplaceEvent.Transaction tx1 = MarketplaceEvent.Transaction.newBuilder().setTransactionId("1")
				.setNftId(MarketplaceEvent.NftId.newBuilder().setTokenId("1.2.3").setSerialNumber("1"))
				.setListPrice(500).setPurchasePrice(0).build();
		MarketplaceEvent listedEvent = MarketplaceEvent.newBuilder().setEventType(MarketplaceEvent.EventType.LISTED)
				.addTransactions(tx1).build();
		MarketplaceEvent.Transaction tx2 = MarketplaceEvent.Transaction.newBuilder().setTransactionId("1")
				.setNftId(MarketplaceEvent.NftId.newBuilder().setTokenId("1.2.3").setSerialNumber("1"))
				.setListPrice(500).setPurchasePrice(500).build();
		MarketplaceEvent purchasedEvent = MarketplaceEvent.newBuilder()
				.setEventType(MarketplaceEvent.EventType.PURCHASED).addTransactions(tx2).build();

		Token expectedListed = MarketplaceProtobufMapper.fromMarketplaceEvent(listedEvent).get(0);
		Token expectedError = MarketplaceProtobufMapper.fromMarketplaceEvent(errorEvent).get(0);
		Token expectedPurchased = MarketplaceProtobufMapper.fromMarketplaceEvent(purchasedEvent).get(0);

		testFlux = Flux.just(listedEvent, errorEvent, purchasedEvent);
		when(this.subscriberService.getMarketplaceEvents()).thenReturn(testFlux);
		when(this.tokenRepository.saveOrUpdate(expectedListed)).thenReturn(Mono.just(expectedListed));
		Token badDBResult = new Token();
		when(this.tokenRepository.saveOrUpdate(expectedError)).thenReturn(Mono.just(badDBResult));
		when(this.tokenRepository.saveOrUpdate(expectedPurchased)).thenReturn(Mono.just(expectedPurchased));

		when(this.messageChannels.sendNftDetails(any())).thenReturn(Mono.just("messageId1"));

		this.startPublisher();

		StepVerifier.create(this.publisherService.getTransactionStream()).expectNext(expectedListed)
				.expectNext(expectedPurchased).verifyComplete();

		verify(this.tokenRepository).saveOrUpdate(expectedListed);
		verify(this.tokenRepository).saveOrUpdate(expectedError);
		verify(this.tokenRepository).saveOrUpdate(expectedPurchased);
		verify(this.messageChannels, times(2)).sendNftDetails(notNull());
		verifyNoMoreInteractions(this.tokenRepository);
		verifyNoMoreInteractions(this.messageChannels);
	}

	private TokenRepository tokenRepository() {
		return mock(TokenRepository.class);
	}

	private EventService subscriberService() {
		return mock(MarketplaceEventService.class);
	}

	private ArmmMessageChannels messageChannels() {
		return mock(ArmmMessageChannels.class);
	}

	private void startPublisher() {
		this.publisherService = new TransactionPublisherService(this.subscriberService, this.tokenRepository,
				this.messageChannels);
		this.publisherService.initTransactionStream();
	}

}
