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

package com.tolamearth.integration.marketplace.buyer;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * Purchase Nfts through the marketplace (calls whitelist_purchase contract function)
 */
@Client("${api.hem-marketplace-client-url}" + "${api.hem-marketplace-api-version}")
public interface PurchaseOffsetClient {

	@Post("/offsets/purchase")
	Mono<PurchaseResponse> purchaseNfts(@Body PurchaseRequest request);

	record PurchaseRequest(@NonNull @JsonProperty("txn_id") String transactionId,
			@NonNull @JsonProperty("account_id") String accountId, @NonNull @JsonProperty("nfts") List<Nft> nfts) {

	}

	record Nft(@JsonProperty("token_id") String tokenId, @JsonProperty("serial_number") Long serialNumber) {

	}

	record PurchaseResponse(PurchaseRequest request) {

	}

}
