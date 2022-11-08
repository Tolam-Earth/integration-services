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

package com.tolamearth.integration.api.history;

import com.tolamearth.integration.api.ApiError;
import com.tolamearth.integration.api.ApiNftId;
import com.tolamearth.integration.api.ErrorCode;
import com.tolamearth.integration.core.assets.*;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@Controller("${api.integration-api-version}")
public class OffsetHistoryController {

	private TokenRepository repository;

	@Get("/offsets/{token_id},{serial_number}/transactions")
	Mono<HttpResponse> getOffsetTransactionHistory(@PathVariable String token_id, @PathVariable String serial_number) {

		log.info("Processing offset transaction history request. TokenId: {}, SerialNum: {}", token_id, serial_number);

		return getOffsetHistory(token_id, serial_number).map(history -> {
			if (history.isPresent()) {
				return HttpResponse.ok(history.get());
			}
			return HttpResponse.notFound();
		});
	}

	private Mono<Optional<OffsetHistoryResponse>> getOffsetHistory(String tokenId, String serialNum) {
		return repository.findById(new NftId(tokenId, serialNum)).map(this::createOffsetHistoryResponse)
				.switchIfEmpty(Mono.just(Optional.empty()));
	}

	private Optional<OffsetHistoryResponse> createOffsetHistoryResponse(Token token) {
		return Optional.of(OffsetHistoryResponse.builder()
				.nftId(new ApiNftId(token.getNftId().getTokenId(), token.getNftId().getSerialNumber()))
				.transactions(buildHistoryTransactions(token.getTransactions())).build());
	}

	private List<HistoryTransaction> buildHistoryTransactions(List<TokenTransaction> transactions) {
		return transactions.stream()
				// .filter(transaction ->
				// !transaction.getEventType().equals(EventType.MINTED))
				.map(this::mapHistoryTransaction).collect(Collectors.toList());
	}

	private HistoryTransaction mapHistoryTransaction(TokenTransaction transaction) {
		HistoryTransaction historyTransaction = HistoryTransaction.builder()
				.transactionId(transaction.getId().getTransactionId()).transactionTime(transaction.getTransactionTime())
				.msgType(transaction.getEventType().name()).owner(transaction.getOwner()).build();

		if (transaction.getEventType().equals(EventType.LISTED)) {
			historyTransaction.setPrice(transaction.getListPrice());
		}
		else if (transaction.getEventType().equals(EventType.PURCHASED)) {
			historyTransaction.setPrice(transaction.getPurchasePrice());
		}

		return historyTransaction;
	}

	@Error(status = HttpStatus.NOT_FOUND, global = true)
	public HttpResponse<ApiError> badRequestHandler(HttpRequest request) {
		return HttpResponse.badRequest(ErrorCode.HTTP_STATUS_400_ERROR_1003);
	}

}
