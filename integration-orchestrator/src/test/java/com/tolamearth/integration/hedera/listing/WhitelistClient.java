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

package com.tolamearth.integration.hedera.listing;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.validation.Validated;
import reactor.core.publisher.Mono;

import java.util.List;

@Client("${api.hem-marketplace-client-url}" + "${api.hem-marketplace-api-version}")
interface WhitelistClient {

	@Post("/offsets/list")
	Mono<ListingResponse> listOffset(ListingRequest request);

	record PricedNft(@JsonProperty("token_id") String tokenId, @JsonProperty("serial_number") Long serialNumber,
			@JsonProperty("price") Long price) {
	}

	record ListingRequest(@NonNull @Validated @JsonProperty("txn_id") String transactionId,
			@NonNull @JsonProperty("account_id") String accountId,
			@NonNull @JsonProperty("nfts") List<PricedNft> pricedNfts) {
	}

	record ListingResponse(@JsonProperty ListingRequest request) {
	}

}