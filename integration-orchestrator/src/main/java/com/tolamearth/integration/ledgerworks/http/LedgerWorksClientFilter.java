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

package com.tolamearth.integration.ledgerworks.http;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.ClientFilterChain;
import io.micronaut.http.filter.HttpClientFilter;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;

import static io.micronaut.http.HttpHeaders.AUTHORIZATION;

@Filter("/api/v1/**")
@RequiredArgsConstructor
public class LedgerWorksClientFilter implements HttpClientFilter {

	@Value("${ledger-works-api-key}")
	private String LEDGER_WORKS_API_KEY;

	@Override
	public Publisher<? extends HttpResponse<?>> doFilter(MutableHttpRequest<?> request, ClientFilterChain chain) {
		return chain.proceed(request.header(AUTHORIZATION, LEDGER_WORKS_API_KEY));
	}

}