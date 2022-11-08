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
import com.tolamearth.integration.marketplace.events.EventService;
import com.tolamearth.integration.marketplace.events.MarketplaceProtobufMapper;
import com.tolamearth.integration.armm.ArmmEvent;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Slf4j
@Singleton
public class TransactionPublisherService {

	private final EventService marketplaceService;

	private final TokenRepository tokenRepository;

	private final ArmmMessageChannels messageChannels;

	private Flux<Token> transactionStream;

	public TransactionPublisherService(EventService marketplaceService, TokenRepository tokenRepository,
			ArmmMessageChannels messageChannels) {
		this.marketplaceService = marketplaceService;
		this.tokenRepository = tokenRepository;
		this.messageChannels = messageChannels;
	}

	@PostConstruct
	public void initTransactionStream() {

		if (!Objects.isNull(this.transactionStream)) {
			throw new IllegalStateException("transactionStream already initialized");
		}

		this.transactionStream = this.marketplaceService.getMarketplaceEvents().concatMap(marketplaceEvent -> {
			log.info("New Marketplace {} Event received", marketplaceEvent.getEventType());
			return Flux.fromIterable(MarketplaceProtobufMapper.fromMarketplaceEvent(marketplaceEvent));
		}).concatMap(token -> {
			log.info("Saving new transactions from marketplace event for token {}", token);
			return this.tokenRepository.saveOrUpdate(token);
		}).flatMap(this::publishToken)
				.onErrorContinue((ex, token) -> log.error("Error processing Marketplace event {}", token, ex));
	}

	public Flux<Token> getTransactionStream() {
		return this.transactionStream;
	}

	private Mono<Token> publishToken(Token token) {
		log.info("Publishing new transactions to ARMM for token {}", token);
		ArmmEvent event = ArmmProtobufMapper.fromMarketplaceToken(token);
		return this.messageChannels.sendNftDetails(event.toByteArray()).thenReturn(token);
	}

}
