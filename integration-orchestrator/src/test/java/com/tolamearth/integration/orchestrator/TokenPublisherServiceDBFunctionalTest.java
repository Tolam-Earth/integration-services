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

import com.google.protobuf.InvalidProtocolBufferException;
import com.tolamearth.integration.armm.ArmmEvent;
import com.tolamearth.integration.core.assets.NftId;
import com.tolamearth.integration.core.assets.Token;
import com.tolamearth.integration.core.assets.TokenRepository;
import com.tolamearth.integration.ledgerworks.data.LedgerWorksMapper;
import com.tolamearth.integration.ledgerworks.data.NftTransfer;
import com.tolamearth.integration.ledgerworks.data.Transaction;
import com.tolamearth.integration.ledgerworks.discovery.PollingTokenDiscoveryService;
import com.tolamearth.integration.ledgerworks.discovery.TokenDiscoveryService;
import io.micronaut.context.annotation.Property;
import io.micronaut.gcp.pubsub.annotation.PubSubListener;
import io.micronaut.gcp.pubsub.annotation.Subscription;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Property(name = "token-discovery.token-ids", value = "0.0.48243577")
@Property(name = "esg-url", value = "https://testnet.esg.api.lworks.io/api/v1")
public class TokenPublisherServiceDBFunctionalTest {

	Logger log = LoggerFactory.getLogger(TokenPublisherServiceDBFunctionalTest.class);

	@Inject
	TokenPublisherService tokenPublisherService;

	@Inject
	TokenRepository tokenRepository;

	@Inject
	TestMessageListener listener;

	List<String> TOKEN_IDS = List.of("0.0.48243577");

	NftId nftId1 = new NftId("0.0.48243577", "3031");

	NftId nftId2 = new NftId("0.0.48243577", "3032");

	NftId nftId3 = new NftId("0.0.48243577", "3033");

	NftTransfer transfer1 = NftTransfer.builder().tokenId("0.0.48243577").serialNumber(3031).build();

	NftTransfer transfer2 = NftTransfer.builder().tokenId("0.0.48243577").serialNumber(3032).build();

	NftTransfer transfer3 = NftTransfer.builder().tokenId("0.0.48243577").serialNumber(3033).build();

	Transaction transaction1 = Transaction.builder().transactionId("0.1.2")
			.nftTransfers(List.of(transfer1, transfer2, transfer3)).build();

	private Flux<Transaction> testFlux;

	@Test
	void testPublisherService() {
		Token[] tokens = LedgerWorksMapper.fromLedgerWorksMintedTransaction(transaction1, TOKEN_IDS).stream()
				.toArray(Token[]::new);
		for (Token token : tokens) {
			setTestData(token);
		}

		StepVerifier.create(tokenPublisherService.getMintedTokenStream()).expectNext(tokens).verifyComplete();

		Token storedToken1 = tokenRepository.findById(nftId1).block();
		Assertions.assertEquals(tokens[0], storedToken1);
		Assertions.assertEquals(1, storedToken1.getTransactions().size());
		Token storedToken2 = tokenRepository.findById(nftId2).block();
		Assertions.assertNotNull(storedToken2);
		Token storedToken3 = tokenRepository.findById(nftId3).block();
		Assertions.assertNotNull(storedToken3);

		Long tokenCount = tokenRepository.findAll().count().block();
		Assertions.assertEquals(3, tokenCount);
		Assertions.assertEquals(3, tokenPublisherService.getRepeatedTokenCount());

		Assertions.assertEquals(3, listener.events.size());
	}

	@MockBean(PollingTokenDiscoveryService.class)
	public TokenDiscoveryService getMockDiscoveryService() {
		TokenDiscoveryService discoveryService = mock(PollingTokenDiscoveryService.class);
		this.testFlux = Flux.just(transaction1, transaction1);
		when(discoveryService.getMintedTransactions()).thenReturn(testFlux);
		return discoveryService;
	}

	private void setTestData(Token token) {
		// Set the NFT Details to match the ESG Api Mock Response
		token.setVintageYear(2021);
		token.setProjectCategory("RENEW_ENERGY");
		token.setProjectType("WIND");
		token.setCountry("IND");
		token.setFirstSubdivision("GJ");
	}

	@PubSubListener
	public static class TestMessageListener {

		Logger log = LoggerFactory.getLogger(TestMessageListener.class);

		List<ArmmEvent> events = new ArrayList<>();

		public TestMessageListener() {

		}

		@Subscription("nft_details_listener")
		public void onArmmEvent(byte[] data) throws InvalidProtocolBufferException {
			ArmmEvent event = ArmmEvent.parseFrom(data);
			log.info("Received ARMM Event: {}", event);
			events.add(event);
		}

	}

}
