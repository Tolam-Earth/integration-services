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

package com.tolamearth.integration.ledgerworks.discovery;

import com.tolamearth.integration.core.EventEmitter;
import com.tolamearth.integration.ledgerworks.http.LedgerWorksClient;
import com.tolamearth.integration.ledgerworks.data.Transaction;
import com.tolamearth.integration.ledgerworks.data.TransactionsResponse;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Singleton
public class PollingTokenDiscoveryService implements TokenDiscoveryService {

	private final LedgerWorksClient ledgerWorksClient;

	private final List<String> TOKEN_IDS;

	private String lastTrackedTimestamp;

	private final List<String> treasuryAccountIds;

	private final Flux<Transaction> mintedTokens;

	private final EventEmitter<Transaction> emitter;

	public PollingTokenDiscoveryService(LedgerWorksClient ledgerWorksClient,
			@Value("${token-discovery.token-ids}") List<String> tokenIds) {

		this.ledgerWorksClient = ledgerWorksClient;
		this.TOKEN_IDS = tokenIds;
		this.treasuryAccountIds = new ArrayList<>();
		this.lastTrackedTimestamp = null;
		this.emitter = new EventEmitter<>();
		this.mintedTokens = Flux.create(emitter::registerSink);
	}

	public void discoverMintedTokens() {
		log.info("Discovering minted tokens...");

		if (treasuryAccountIds.isEmpty()) {
			getTreasuryAccountIds();
		}

		treasuryAccountIds.forEach(treasuryAccountId -> {
			log.info("Last tracked timestamp: {}", lastTrackedTimestamp);

			ledgerWorksClient.getTransactions(treasuryAccountId, "TOKENMINT", "asc", lastTrackedTimestamp)
					.flatMapIterable(TransactionsResponse::transactions)
					.flatMapSequential(
							initialTransaction -> ledgerWorksClient.getTransaction(initialTransaction.transactionId()))
					.flatMapIterable(TransactionsResponse::transactions)
					.doOnNext(initialTransaction -> lastTrackedTimestamp = initialTransaction.consensusTimestamp())
					.subscribe(finalTransaction -> {
						emitter.emit(finalTransaction);
						log.info("Adding token mint transaction: {}", finalTransaction);
					}, throwable -> log.error("Error processing latest minted token transactions. Exception: ",
							throwable));
		});
	}

	@Override
	public Flux<Transaction> getMintedTransactions() {
		return mintedTokens;
	}

	private void getTreasuryAccountIds() {
		TOKEN_IDS.forEach(tokenId -> {
			ledgerWorksClient.getToken(tokenId).subscribe(token -> treasuryAccountIds.add(token.treasuryAccountId()),
					throwable -> log.error("Error fetching treasury account Ids from ledger works API. Exception: ",
							throwable));
		});
	}

}
