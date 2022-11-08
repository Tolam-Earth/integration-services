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

package com.tolamearth.integration.api.purchase;

import com.tolamearth.integration.api.ApiError;
import com.tolamearth.integration.api.ApiNftId;
import com.tolamearth.integration.marketplace.buyer.BuyerService;
import com.tolamearth.integration.api.ErrorCode;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.uri.UriBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import io.micronaut.http.annotation.Error;
import java.net.URI;

@Slf4j
@RequiredArgsConstructor
@Controller("${api.integration-api-version}")
public class OffsetPurchaseController {

	static final String BUYER = "/offsets/buyer";

	@Value("${api.integration-api-version}")
	private String integrationApiVersion;

	private final BuyerService buyerService;

	@Post(BUYER)
	Mono<HttpResponse<OffsetPurchaseResponse>> purchaseOffset(@Body @NonNull OffsetPurchaseRequest request) {

		log.info("Received offset purchase request: {}", request);

		ApiNftId nftId = request.getAsset().getNftId();

		URI locationHeader = UriBuilder.of(integrationApiVersion).path("/offsets")
				.path(nftId.getTokenId() + "," + nftId.getSerialNumber()).path("transactions").build();

		buyerService.makeAsyncPurchase(request);

		return Mono.just(HttpResponse.accepted(locationHeader).body(new OffsetPurchaseResponse("PENDING")));
	}

	@Error(status = HttpStatus.BAD_REQUEST, global = true)
	public HttpResponse<ApiError> badRequestHandler(HttpRequest request) {
		return HttpResponse.badRequest(ErrorCode.HTTP_STATUS_400_ERROR_1003);
	}

}
