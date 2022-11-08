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

package com.tolamearth.integration.marketplace.events;

import com.google.protobuf.Timestamp;
import com.tolamearth.integration.marketplace.MarketplaceEvent;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.mock;

@Slf4j
@MicronautTest(rebuildContext = true)
public class MarketplaceEventServiceTest {

	@Inject
	MarketplaceEventService marketplaceService;

	@Inject
	MarketplaceEventListener marketplaceEventListener;

	@MockBean(MarketplaceEventListener.class)
	MarketplaceEventListener marketplaceEventListener() {
		return mock(MarketplaceEventListener.class);
	}

	@Test
	void test_emitMarketplaceEvent() {

		MarketplaceEvent testEvent = MarketplaceEvent.newBuilder().setEventType(MarketplaceEvent.EventType.LISTED)
				.addAllTransactions(List.of(
						MarketplaceEvent.Transaction.newBuilder()
								.setNftId(MarketplaceEvent.NftId.newBuilder().setTokenId("listed-token-id-1")
										.setSerialNumber("listed-serial-number-1").build())
								.setOwner("owner-1").setTransactionId("transaction-id-1").setListPrice(100)
								.setPurchasePrice(110).setTransactionTime(Timestamp.getDefaultInstance()).build(),
						MarketplaceEvent.Transaction.newBuilder()
								.setNftId(MarketplaceEvent.NftId.newBuilder().setTokenId("listed-token-id-2")
										.setSerialNumber("listed-serial-number-2").build())
								.setOwner("owner-2").setTransactionId("transaction-id-2").setListPrice(200)
								.setPurchasePrice(210).setTransactionTime(Timestamp.getDefaultInstance()).build()))
				.build();

		StepVerifier.create(marketplaceService.getMarketplaceEvents())
				.then(() -> marketplaceService.emitMarketplaceEvent(testEvent)).expectNext(testEvent).thenCancel()
				.verify();
	}

}
