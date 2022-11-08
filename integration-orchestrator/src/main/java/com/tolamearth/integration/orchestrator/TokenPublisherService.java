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
import com.tolamearth.integration.armm.ArmmProtobufMapper;
import com.tolamearth.integration.core.assets.Token;
import com.tolamearth.integration.core.assets.TokenRepository;
import com.tolamearth.integration.ledgerworks.data.LedgerWorksMapper;
import com.tolamearth.integration.ledgerworks.discovery.TokenDiscoveryService;
import com.tolamearth.integration.ledgerworks.http.EsgClient;
import com.tolamearth.integration.armm.ArmmEvent;
import io.micronaut.context.annotation.Value;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Singleton
public class TokenPublisherService {

	private static final Logger log = LoggerFactory.getLogger(TokenPublisherService.class);

	private final TokenDiscoveryService tokenDiscoveryService;

	private final EsgClient esgClient;

	private final TokenRepository tokenRepository;

	private final ArmmMessageChannels messageChannels;

	private Flux<Token> mintedTokenStream;

	private long repeatedTokenCount = 0;

	private List<String> TOKEN_IDS;

	public TokenPublisherService(TokenDiscoveryService tokenDiscoveryService, EsgClient esgClient,
			TokenRepository tokenRepository, ArmmMessageChannels messageChannels,
			@Value("${token-discovery.token-ids}") List<String> TOKEN_IDS) {
		this.tokenDiscoveryService = tokenDiscoveryService;
		this.esgClient = esgClient;
		this.tokenRepository = tokenRepository;
		this.messageChannels = messageChannels;
		this.TOKEN_IDS = TOKEN_IDS;
	}

	@PostConstruct
	public void initMintedTokenStream() {
		if (!Objects.isNull(this.mintedTokenStream)) {
			throw new IllegalStateException("mintedTokenStream already initialized");
		}

		this.mintedTokenStream = this.tokenDiscoveryService.getMintedTransactions()
				.concatMapIterable(
						transaction -> LedgerWorksMapper.fromLedgerWorksMintedTransaction(transaction, TOKEN_IDS))
				.flatMapSequential(this::retrieveNftDetails)
				.map(result -> result
						.orElseThrow(() -> new IllegalStateException("Failed to retrieve and map NFT Details")))
				.concatMap(this::processToken).flatMapSequential(token -> {
					log.info("Publishing new MINTED token {} to ARMM", token);
					return publishToken(token);
				}).doOnNext(token -> log.info("Processing complete for new MINTED token {}", token))
				.onErrorContinue((ex, token) -> log.error("Error processing MINTED token {}", token, ex));
	}

	@Transactional
	Mono<Token> processToken(Token token) {
		return Mono.just(token).filterWhen(this::isTokenUnprocessed).flatMap(t -> saveToken(t));
	}

	private Mono<Optional<Token>> retrieveNftDetails(Token token) {
		log.info("Retrieving NFT Details for token {}", token);
		return this.esgClient
				.getNftDetails(token.getNftId().getTokenId(), Long.valueOf(token.getNftId().getSerialNumber()))
				.flatMap(details -> LedgerWorksMapper.mergeNftDetails(token, details)).thenReturn(Optional.of(token))
				.onErrorResume(throwable -> {
					log.error("Error retrieving and mapping NFT Details", throwable);
					return Mono.just(Optional.empty());
				});
	}

	private Mono<Token> saveToken(Token token) {
		log.info("Storing new MINTED token {} received from TokenDiscoveryService", token);
		return tokenRepository.save(token);
	}

	public Flux<Token> getMintedTokenStream() {
		return this.mintedTokenStream;
	}

	public Long getRepeatedTokenCount() {
		return this.repeatedTokenCount;
	}

	private Mono<Token> publishToken(Token token) {
		ArmmEvent event = ArmmProtobufMapper.fromMintedToken(token);
		Mono<String> message = this.messageChannels.sendNftDetails(event.toByteArray());
		return message.thenReturn(token);
	}

	private Mono<Boolean> isTokenUnprocessed(Token token) {
		return tokenRepository.findById(token.getNftId()).map(result -> {
			log.info("Token {} received from TokenDiscoveryService already processed.", token);
			repeatedTokenCount = repeatedTokenCount < Long.MAX_VALUE ? repeatedTokenCount + 1 : Long.MAX_VALUE;
			return Boolean.FALSE;
		}).switchIfEmpty(Mono.just(Boolean.TRUE));
	}

}
