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

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Singleton
@Requires(notEnv = "test")
public class EventSubscriptionListeners {

	private final List<Disposable> subscriptions = new ArrayList<>();

	private final TokenPublisherService tokenPublisherService;

	private final TransactionPublisherService transactionPublisherService;

	public EventSubscriptionListeners(TokenPublisherService publisherService,
			TransactionPublisherService transactionPublisherService) {
		this.tokenPublisherService = publisherService;
		this.transactionPublisherService = transactionPublisherService;
	}

	@EventListener
	public void onStartupEvent(StartupEvent event) {
		this.subscriptions.add(tokenPublisherService.getMintedTokenStream().subscribe());
		log.info("Subscribed to minted token stream...");
		this.subscriptions.add(transactionPublisherService.getTransactionStream().subscribe());
		log.info("Subscribed to Marketplace event stream...");
	}

}
