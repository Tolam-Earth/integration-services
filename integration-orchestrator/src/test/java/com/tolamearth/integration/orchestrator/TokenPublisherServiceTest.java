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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tolamearth.integration.armm.ArmmMessageChannels;
import com.tolamearth.integration.core.assets.NftId;
import com.tolamearth.integration.core.assets.Token;
import com.tolamearth.integration.core.assets.TokenRepository;
import com.tolamearth.integration.ledgerworks.data.LedgerWorksMapper;
import com.tolamearth.integration.ledgerworks.data.NftTransfer;
import com.tolamearth.integration.ledgerworks.data.Transaction;
import com.tolamearth.integration.ledgerworks.discovery.PollingTokenDiscoveryService;
import com.tolamearth.integration.ledgerworks.discovery.TokenDiscoveryService;
import com.tolamearth.integration.ledgerworks.http.EsgClient;
import io.micronaut.core.io.scan.DefaultClassPathResourceLoader;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TokenPublisherServiceTest {

	Logger log = LoggerFactory.getLogger(TokenPublisherServiceTest.class);

	TokenPublisherService publisherService;

	TokenDiscoveryService discoveryService;

	TokenRepository tokenRepository;

	EsgClient esgClient;

	ArmmMessageChannels messageChannels;

	private Flux<Transaction> testFlux;

	private Map testNftDetails;

	List<String> TOKEN_IDS = List.of("0.1.2");

	@BeforeAll
	void initNftDetails() {
		DefaultClassPathResourceLoader loader = new DefaultClassPathResourceLoader(this.getClass().getClassLoader());
		Optional<InputStream> testResource = loader.getResourceAsStream("classpath:EsgOffsetApiResponse.json");
		Assertions.assertTrue(testResource.isPresent());
		try (InputStream testData = testResource.get()) {
			String testResponse = new String(testData.readAllBytes(), StandardCharsets.UTF_8);
			ObjectMapper mapper = new ObjectMapper();
			this.testNftDetails = mapper.reader().readValue(testResponse, Map.class);
			Assertions.assertNotNull(this.testNftDetails);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@BeforeEach
	void initMocks() {
		this.tokenRepository = tokenRepository();
		this.discoveryService = tokenDiscoveryService();
		this.messageChannels = armmMessageChannels();
		this.esgClient = esgClient();
	}

	private void startPublisher() {
		this.publisherService = new TokenPublisherService(this.discoveryService, this.esgClient, this.tokenRepository,
				this.messageChannels, TOKEN_IDS);
		this.publisherService.initMintedTokenStream();
	}

	@Test
	void testFreshToken() {
		NftTransfer transfer1 = NftTransfer.builder().tokenId("0.1.2").serialNumber(3).build();
		Transaction transaction = Transaction.builder().transactionId("1.2.3-4-5").nftTransfers(List.of(transfer1))
				.build();

		NftId nftId = new NftId("0.1.2", "3");
		Token token = LedgerWorksMapper.fromLedgerWorksMintedTransaction(transaction, TOKEN_IDS).get(0);
		setTestData(token);
		Token storedToken = LedgerWorksMapper.fromLedgerWorksMintedTransaction(transaction, TOKEN_IDS).get(0);
		setTestData(storedToken);
		when(tokenRepository.findById(nftId)).thenReturn(Mono.empty());
		when(esgClient.getNftDetails(nftId.getTokenId(), Long.valueOf(nftId.getSerialNumber())))
				.thenReturn(Mono.just(this.testNftDetails));
		when(tokenRepository.save(token)).thenReturn(Mono.just(storedToken));
		when(messageChannels.sendNftDetails(any())).thenReturn(Mono.just("messageId1"));

		testFlux = Flux.just(transaction);
		when(discoveryService.getMintedTransactions()).thenReturn(testFlux);

		this.startPublisher();

		StepVerifier.create(this.publisherService.getMintedTokenStream()).expectNext(token).verifyComplete();

		verify(tokenRepository).findById(nftId);
		verify(tokenRepository).save(token);
		verify(messageChannels).sendNftDetails(notNull());
		verifyNoMoreInteractions(tokenRepository);
		verifyNoMoreInteractions(messageChannels);
	}

	@Test
	void testExistingToken() {
		NftTransfer transfer1 = NftTransfer.builder().tokenId("0.1.2").serialNumber(4).build();
		Transaction transaction = Transaction.builder().nftTransfers(List.of(transfer1)).build();

		NftId nftId = new NftId("0.1.2", "4");
		Token storedToken = LedgerWorksMapper.fromLedgerWorksMintedTransaction(transaction, TOKEN_IDS).get(0);
		when(esgClient.getNftDetails(nftId.getTokenId(), Long.valueOf(nftId.getSerialNumber())))
				.thenReturn(Mono.just(this.testNftDetails));
		when(tokenRepository.findById(nftId)).thenReturn(Mono.just(storedToken));

		testFlux = Flux.just(transaction);
		when(discoveryService.getMintedTransactions()).thenReturn(testFlux);

		this.startPublisher();

		StepVerifier.create(this.publisherService.getMintedTokenStream()).verifyComplete();

		Assertions.assertEquals(1, publisherService.getRepeatedTokenCount());
		verify(tokenRepository).findById(nftId);
		verifyNoMoreInteractions(tokenRepository);
		verifyNoInteractions(messageChannels);
	}

	@Test
	void testMultipleTokens() {
		NftTransfer transfer1 = NftTransfer.builder().tokenId("0.1.2").serialNumber(3).build();
		NftTransfer transfer2 = NftTransfer.builder().tokenId("0.1.2").serialNumber(4).build();
		NftTransfer transfer3 = NftTransfer.builder().tokenId("0.1.2").serialNumber(5).build();

		Transaction transaction = Transaction.builder().transactionId("1.2.3-4-5")
				.nftTransfers(List.of(transfer1, transfer2, transfer3)).build();

		List<Token> tokens = LedgerWorksMapper.fromLedgerWorksMintedTransaction(transaction, TOKEN_IDS);
		List<Token> expectedTokens = LedgerWorksMapper.fromLedgerWorksMintedTransaction(transaction, TOKEN_IDS);

		NftId nftId1 = new NftId("0.1.2", "3");
		NftId nftId2 = new NftId("0.1.2", "4");
		NftId nftId3 = new NftId("0.1.2", "5");
		Token token1 = tokens.get(0);
		setTestData(token1);
		Token token2 = tokens.get(1);
		setTestData(token2);
		for (Token expected : expectedTokens) {
			setTestData(expected);
		}

		when(esgClient.getNftDetails(nftId1.getTokenId(), Long.valueOf(nftId1.getSerialNumber())))
				.thenReturn(Mono.just(this.testNftDetails));
		when(tokenRepository.findById(nftId1)).thenReturn(Mono.empty());
		when(tokenRepository.save(token1)).thenReturn(Mono.just(expectedTokens.get(0)));

		when(esgClient.getNftDetails(nftId2.getTokenId(), Long.valueOf(nftId2.getSerialNumber())))
				.thenReturn(Mono.just(this.testNftDetails));
		when(tokenRepository.findById(nftId2)).thenReturn(Mono.empty());
		when(tokenRepository.save(token2)).thenReturn(Mono.just(expectedTokens.get(1)));

		when(esgClient.getNftDetails(nftId3.getTokenId(), Long.valueOf(nftId3.getSerialNumber())))
				.thenReturn(Mono.just(this.testNftDetails));
		when(tokenRepository.findById(nftId3)).thenReturn(Mono.just(expectedTokens.get(2)));

		when(messageChannels.sendNftDetails(any())).thenReturn(Mono.just("messageId1"));

		testFlux = Flux.just(transaction);
		when(discoveryService.getMintedTransactions()).thenReturn(testFlux);

		this.startPublisher();

		StepVerifier.create(this.publisherService.getMintedTokenStream()).expectNext(token1, token2).verifyComplete();

		verify(tokenRepository).findById(nftId1);
		verify(tokenRepository).save(token1);
		verify(tokenRepository).findById(nftId2);
		verify(tokenRepository).save(token2);
		verify(tokenRepository).findById(nftId3);
		verify(messageChannels, times(2)).sendNftDetails(any());
		verifyNoMoreInteractions(tokenRepository);
		verifyNoMoreInteractions(messageChannels);
	}

	@Test
	void testMixedValidAndInvalidTokens() {
		NftTransfer transfer1 = NftTransfer.builder().tokenId("0.1.2").serialNumber(3).build();
		NftTransfer transfer2 = NftTransfer.builder().tokenId("0.1.2").serialNumber(4).build();
		NftTransfer transfer3 = NftTransfer.builder().tokenId("0.1.2").serialNumber(5).build();

		Transaction transaction1 = Transaction.builder().transactionId("1.2.3-4-5")
				.nftTransfers(List.of(transfer1, transfer2)).build();

		Transaction invalidTransaction = Transaction.builder().build();

		Transaction transaction2 = Transaction.builder().transactionId("1.2.3-6-7").nftTransfers(List.of(transfer3))
				.build();

		List<Token> tokens = LedgerWorksMapper.fromLedgerWorksMintedTransaction(transaction1, TOKEN_IDS);
		tokens.addAll(LedgerWorksMapper.fromLedgerWorksMintedTransaction(transaction2, TOKEN_IDS));
		List<Token> expectedTokens = LedgerWorksMapper.fromLedgerWorksMintedTransaction(transaction1, TOKEN_IDS);
		expectedTokens.addAll(LedgerWorksMapper.fromLedgerWorksMintedTransaction(transaction2, TOKEN_IDS));

		NftId nftId1 = new NftId("0.1.2", "3");
		NftId nftId2 = new NftId("0.1.2", "4");
		NftId nftId3 = new NftId("0.1.2", "5");
		Token token1 = tokens.get(0);
		setTestData(token1);
		Token token2 = tokens.get(1);
		setTestData(token2);
		Token token3 = tokens.get(2);
		setTestData(token3);
		for (Token expected : expectedTokens) {
			setTestData(expected);
		}

		when(esgClient.getNftDetails(nftId1.getTokenId(), Long.valueOf(nftId1.getSerialNumber())))
				.thenReturn(Mono.just(this.testNftDetails));
		when(tokenRepository.findById(nftId1)).thenReturn(Mono.empty());
		when(tokenRepository.save(token1)).thenReturn(Mono.just(expectedTokens.get(0)));

		when(esgClient.getNftDetails(nftId2.getTokenId(), Long.valueOf(nftId2.getSerialNumber())))
				.thenReturn(Mono.just(this.testNftDetails));
		when(tokenRepository.findById(nftId2)).thenReturn(Mono.empty());
		when(tokenRepository.save(token2)).thenReturn(Mono.just(expectedTokens.get(1)));

		when(esgClient.getNftDetails(nftId3.getTokenId(), Long.valueOf(nftId3.getSerialNumber())))
				.thenReturn(Mono.just(this.testNftDetails));
		when(tokenRepository.findById(nftId3)).thenReturn(Mono.empty());
		when(tokenRepository.save(token3)).thenReturn(Mono.just(expectedTokens.get(2)));

		when(messageChannels.sendNftDetails(any())).thenReturn(Mono.just("messageId1"));

		testFlux = Flux.just(transaction1, invalidTransaction, transaction2);
		when(discoveryService.getMintedTransactions()).thenReturn(testFlux);

		this.startPublisher();

		StepVerifier.create(this.publisherService.getMintedTokenStream()).expectNext(token1, token2, token3)
				.verifyComplete();

		verify(tokenRepository).findById(nftId1);
		verify(tokenRepository).save(token1);
		verify(tokenRepository).findById(nftId2);
		verify(tokenRepository).save(token2);
		verify(tokenRepository).findById(nftId3);
		verify(tokenRepository).save(token3);
		verify(messageChannels, times(3)).sendNftDetails(any());
		verifyNoMoreInteractions(tokenRepository);
		verifyNoMoreInteractions(messageChannels);
	}

	@Test
	void testMixedValidAndDBError() {
		NftTransfer transfer1 = NftTransfer.builder().tokenId("0.1.2").serialNumber(3).build();
		NftTransfer transfer2 = NftTransfer.builder().tokenId("0.1.2").serialNumber(4).build();
		NftTransfer transfer3 = NftTransfer.builder().tokenId("0.1.2").serialNumber(5).build();

		Transaction transaction1 = Transaction.builder().transactionId("1.2.3-4-5")
				.nftTransfers(List.of(transfer1, transfer2, transfer3)).build();

		List<Token> tokens = LedgerWorksMapper.fromLedgerWorksMintedTransaction(transaction1, TOKEN_IDS);
		List<Token> expectedTokens = LedgerWorksMapper.fromLedgerWorksMintedTransaction(transaction1, TOKEN_IDS);

		NftId nftId1 = new NftId("0.1.2", "3");
		NftId nftId2 = new NftId("0.1.2", "4");
		NftId nftId3 = new NftId("0.1.2", "5");
		Token token1 = tokens.get(0);
		setTestData(token1);
		Token token2 = tokens.get(1);
		setTestData(token2);
		Token token3 = tokens.get(2);
		setTestData(token3);
		for (Token expected : expectedTokens) {
			setTestData(expected);
		}

		when(esgClient.getNftDetails(nftId1.getTokenId(), Long.valueOf(nftId1.getSerialNumber())))
				.thenReturn(Mono.just(this.testNftDetails));
		when(tokenRepository.findById(nftId1)).thenThrow(new RuntimeException("DB Error finding token."));

		when(esgClient.getNftDetails(nftId2.getTokenId(), Long.valueOf(nftId2.getSerialNumber())))
				.thenReturn(Mono.just(this.testNftDetails));
		when(tokenRepository.findById(nftId2)).thenReturn(Mono.empty());
		when(tokenRepository.save(token2)).thenThrow(new RuntimeException("DB Error saving token."));

		when(esgClient.getNftDetails(nftId3.getTokenId(), Long.valueOf(nftId3.getSerialNumber())))
				.thenReturn(Mono.just(this.testNftDetails));
		when(tokenRepository.findById(nftId3)).thenReturn(Mono.empty());
		when(tokenRepository.save(token3)).thenReturn(Mono.just(expectedTokens.get(2)));

		when(messageChannels.sendNftDetails(any())).thenReturn(Mono.just("messageId1"));

		testFlux = Flux.just(transaction1);
		when(discoveryService.getMintedTransactions()).thenReturn(testFlux);

		this.startPublisher();

		StepVerifier.create(this.publisherService.getMintedTokenStream()).expectNext(token3).verifyComplete();

		verify(tokenRepository).findById(nftId1);
		verify(tokenRepository).findById(nftId2);
		verify(tokenRepository).save(token2);
		verify(tokenRepository).findById(nftId3);
		verify(tokenRepository).save(token3);
		verify(messageChannels, times(1)).sendNftDetails(any());
		verifyNoMoreInteractions(tokenRepository);
		verifyNoMoreInteractions(messageChannels);
	}

	@Test
	void testMixedValidAndServiceError() {
		NftTransfer transfer1 = NftTransfer.builder().tokenId("0.1.2").serialNumber(3).build();
		NftTransfer transfer2 = NftTransfer.builder().tokenId("0.1.2").serialNumber(4).build();
		NftTransfer transfer3 = NftTransfer.builder().tokenId("0.1.2").serialNumber(5).build();

		Transaction transaction1 = Transaction.builder().transactionId("1.2.3-4-5")
				.nftTransfers(List.of(transfer1, transfer2, transfer3)).build();

		List<Token> tokens = LedgerWorksMapper.fromLedgerWorksMintedTransaction(transaction1, TOKEN_IDS);
		List<Token> expectedTokens = LedgerWorksMapper.fromLedgerWorksMintedTransaction(transaction1, TOKEN_IDS);

		NftId nftId1 = new NftId("0.1.2", "3");
		NftId nftId2 = new NftId("0.1.2", "4");
		NftId nftId3 = new NftId("0.1.2", "5");
		Token token1 = tokens.get(0);
		setTestData(token1);
		Token token2 = tokens.get(1);
		setTestData(token2);
		Token token3 = tokens.get(2);
		setTestData(token3);
		for (Token expected : expectedTokens) {
			setTestData(expected);
		}

		when(esgClient.getNftDetails(nftId1.getTokenId(), Long.valueOf(nftId1.getSerialNumber())))
				.thenReturn(Mono.just(this.testNftDetails));
		when(tokenRepository.findById(nftId1)).thenReturn(Mono.empty());
		when(tokenRepository.save(token1)).thenReturn(Mono.just(expectedTokens.get(0)));

		when(esgClient.getNftDetails(nftId2.getTokenId(), Long.valueOf(nftId2.getSerialNumber())))
				.thenReturn(Mono.error(new RuntimeException("Error calling ESG Details service.")));

		when(esgClient.getNftDetails(nftId3.getTokenId(), Long.valueOf(nftId3.getSerialNumber())))
				.thenReturn(Mono.just(this.testNftDetails));
		when(tokenRepository.findById(nftId3)).thenReturn(Mono.empty());
		when(tokenRepository.save(token3)).thenReturn(Mono.just(expectedTokens.get(2)));

		when(messageChannels.sendNftDetails(any())).thenReturn(Mono.just("messageId1"));

		testFlux = Flux.just(transaction1);
		when(discoveryService.getMintedTransactions()).thenReturn(testFlux);

		this.startPublisher();

		StepVerifier.create(this.publisherService.getMintedTokenStream()).expectNext(token1, token3).verifyComplete();

		verify(tokenRepository).findById(nftId1);
		verify(tokenRepository).save(token1);
		verify(tokenRepository).findById(nftId3);
		verify(tokenRepository).save(token3);
		verify(messageChannels, times(2)).sendNftDetails(any());
		verifyNoMoreInteractions(tokenRepository);
		verifyNoMoreInteractions(messageChannels);
	}

	private void setTestData(Token token) {
		// Set the NFT Details to match the ESG Api Mock Response
		LedgerWorksMapper.mergeNftDetails(token, this.testNftDetails).thenReturn(token);
	}

	TokenDiscoveryService tokenDiscoveryService() {
		return mock(PollingTokenDiscoveryService.class);
	}

	TokenRepository tokenRepository() {
		return mock(TokenRepository.class);
	}

	ArmmMessageChannels armmMessageChannels() {
		return mock(ArmmMessageChannels.class);
	}

	EsgClient esgClient() {
		return mock(EsgClient.class);
	}

}
