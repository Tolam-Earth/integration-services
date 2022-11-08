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

import com.google.protobuf.InvalidProtocolBufferException;
import com.tolamearth.integration.marketplace.MarketplaceEvent;
import io.micronaut.gcp.pubsub.annotation.PubSubListener;
import io.micronaut.gcp.pubsub.annotation.Subscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@PubSubListener
@RequiredArgsConstructor
public class MarketplaceEventListener {

	private final EventService marketplaceService;

	@Subscription("pub_nft_marketplace_state_subscription")
	public void onMarketplaceEvent(byte[] message) throws InvalidProtocolBufferException {
		MarketplaceEvent event = MarketplaceEvent.parseFrom(message);
		log.info("Received Marketplace Message {}", event);
		marketplaceService.emitMarketplaceEvent(event);
	}

}
