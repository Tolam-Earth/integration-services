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
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static com.tolamearth.integration.marketplace.MarketplaceEvent.EventType.LISTED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@MicronautTest
public class MarketplaceEventListenerTest {

	@Inject
	MarketplaceEventListener listener;

	EventService service;

	@MockBean(MarketplaceEventService.class)
	EventService marketplaceService() {
		this.service = mock(EventService.class);
		return this.service;
	}

	@Test
	void testBasicMessage() throws InvalidProtocolBufferException {
		listener.onMarketplaceEvent(MarketplaceEvent.newBuilder().setEventType(LISTED).build().toByteArray());
		verify(service, times(1)).emitMarketplaceEvent(any());
	}

}
