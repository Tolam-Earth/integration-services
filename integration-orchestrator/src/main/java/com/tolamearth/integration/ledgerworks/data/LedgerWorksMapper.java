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

package com.tolamearth.integration.ledgerworks.data;

import com.tolamearth.integration.core.assets.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class LedgerWorksMapper {

	private LedgerWorksMapper() {
	}

	public static List<Token> fromLedgerWorksMintedTransaction(Transaction transaction, List<String> TOKEN_IDS) {
		return transaction.nftTransfers().stream().filter(t -> TOKEN_IDS.contains(t.tokenId())).map(t -> {
			TokenTransaction tx = TokenTransaction.builder()
					.id(new TokenTransactionId(new NftId(t.tokenId(), String.valueOf(t.serialNumber())),
							transaction.transactionId()))
					.eventType(EventType.MINTED)
					.transactionTime(Objects.requireNonNullElse(transaction.consensusTimestamp(),
							TokenTransaction.transactionTimeDefault()))
					.owner(Objects.requireNonNullElse(t.receiverAccountId(), TokenTransaction.ownerDefault())).build();

			return Token.builder().nftId(new NftId(t.tokenId(), String.valueOf(t.serialNumber())))
					.memo(transaction.memoBase64()).build().addTransaction(tx);
		}).collect(Collectors.toList());
	}

	public static Mono<Token> mergeNftDetails(Token token, Map nftDetails) {
		TokenDetails parsedDetails = DetailsParser.parse(nftDetails);
		token.setVintageYear(parsedDetails.vintageYear());
		token.setProjectCategory(parsedDetails.projectCategory());
		token.setProjectType(parsedDetails.projectType());
		token.setCountry(parsedDetails.country());
		token.setFirstSubdivision(parsedDetails.firstSubdivision());
		token.setDeviceId(parsedDetails.deviceId());
		token.setGuardianId(parsedDetails.guardianId());
		return Mono.just(token);
	}

	private static class DetailsParser {

		private static final String TOKEN_ID = "tokenId";

		private static final String ATTRIBUTES = "attributes";

		private static final String SERIAL_NUMBER = "serialNumber";

		private static final String TITLE = "title";

		private static final String VALUE = "value";

		private static final String VINTAGE = "VINTAGE";

		private static final String PROJECT_CATEGORY = "PROJECT CATEGORY";

		private static final String PROJECT_CATEGORY_RENEWABLE = "RENEWABLE ENERGY";

		private static final String PROJECT_CATEGORY_EFFICIENCY = "ENERGY EFFICIENCY";

		private static final String PROJECT_CATEGORY_RENEWABLE_VALUE = "RENEW_ENERGY";

		private static final String PROJECT_CATEGORY_EFFICIENCY_VALUE = "COMM_ENRGY_EFF";

		private static final String PROJECT_TYPE = "PROJECT TYPE";

		private static final String PROJECT_TYPE_WIND = "GRID CONNECTED WIND";

		private static final String PROJECT_TYPE_EMM_RED = "IMPROVED COOKSTOVE";

		private static final String PROJECT_TYPE_WIND_VALUE = "WIND";

		private static final String PROJECT_TYPE_EMM_RED_VALUE = "EMM_RED";

		private static final String COUNTRY = "PROJECT COUNTRY";

		private static final String COUNTRY_INDIA = "INDIA";

		private static final String COUNTRY_KENYA = "KENYA";

		private static final String COUNTRY_INDIA_VALUE = "IND";

		private static final String COUNTRY_KENYA_VALUE = "KEN";

		private static final String FIRST_SUBDIVISION = "STATE/PROVINCE";

		private static final String PROVINCE_INDIA = "GUJARAT";

		private static final String PROVINCE_INDIA_VALUE = "GJ";

		private static final String PROVINCE_KENYA_VALUE = "01";

		static TokenDetails parse(Map nftDetails) {
			final TokenDetails.TokenDetailsBuilder builder = TokenDetails.builder();

			if (!nftDetails.containsKey(TOKEN_ID) || !nftDetails.containsKey(SERIAL_NUMBER)
					|| !nftDetails.containsKey(ATTRIBUTES)) {
				throw new IllegalStateException("NFT Details response is invalid.");
			}

			builder.tokenId(String.valueOf(nftDetails.get(TOKEN_ID)));
			builder.serialNumber(Long.valueOf(nftDetails.get(SERIAL_NUMBER).toString()));

			List<Map> attributes = (List<Map>) nftDetails.get(ATTRIBUTES);

			attributes.stream().forEach(attr -> {
				String title = attr.get(TITLE).toString().toUpperCase();
				switch (title) {
					case VINTAGE -> builder.vintageYear(Long.valueOf(attr.get(VALUE).toString()));
					case PROJECT_CATEGORY -> {
						String value = attr.get(VALUE).toString().toUpperCase();
						String projectCategory = switch (value) {
							case PROJECT_CATEGORY_RENEWABLE -> PROJECT_CATEGORY_RENEWABLE_VALUE;
							case PROJECT_CATEGORY_EFFICIENCY -> PROJECT_CATEGORY_EFFICIENCY_VALUE;
							default -> throw new IllegalStateException("Unexpected project category.");
						};
						builder.projectCategory(projectCategory);
					}
					case PROJECT_TYPE -> {
						String value = attr.get(VALUE).toString().toUpperCase();
						String projectType = switch (value) {
							case PROJECT_TYPE_WIND -> PROJECT_TYPE_WIND_VALUE;
							case PROJECT_TYPE_EMM_RED -> PROJECT_TYPE_EMM_RED_VALUE;
							default -> throw new IllegalStateException("Unexpected project type.");
						};
						builder.projectType(projectType);
					}
					case COUNTRY -> {
						String value = attr.get(VALUE).toString().toUpperCase();
						String countryCode = switch (value) {
							case COUNTRY_INDIA -> COUNTRY_INDIA_VALUE;
							case COUNTRY_KENYA -> COUNTRY_KENYA_VALUE;
							default -> throw new IllegalStateException("Unexpected country.");
						};
						builder.country(countryCode);
					}
					case FIRST_SUBDIVISION -> {
						String value = attr.get(VALUE).toString().toUpperCase();
						String provinceCode = switch (value) {
							case PROVINCE_INDIA -> PROVINCE_INDIA_VALUE;
							default -> PROVINCE_KENYA_VALUE;
						};
						builder.firstSubdivision(provinceCode);
					}
				}
			});
			builder.deviceId(UUID.randomUUID().toString());
			builder.guardianId(UUID.randomUUID().toString());
			return builder.build();
		}

	}

}
