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

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.io.scan.DefaultClassPathResourceLoader;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@MicronautTest(startApplication = false)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EsgClientTest implements TestPropertyProvider {

	@Inject
	EsgClient client;

	EmbeddedServer esgApi;

	@Override
	public Map<String, String> getProperties() {
		return Collections.singletonMap("esg-url", esgUrl());
	}

	@Test
	void testResource() {
		Map result = client.getNftDetails("0.0.48243577", 3031).block();
		Assertions.assertNotNull(result);
		Assertions.assertEquals(3, result.size());
	}

	@AfterAll
	public void teardown() {
		esgApi.close();
	}

	private String esgUrl() {
		if (esgApi == null) {
			esgApi = ApplicationContext.run(EmbeddedServer.class,
					Collections.singletonMap("test.name", "EsgClientTestEsgApi"), "test");
		}
		return "http://localhost:" + esgApi.getPort();
	}

	@Requires(property = "test.name", value = "EsgClientTestEsgApi")
	@Controller
	public static class TokensController {

		private final String testResponse;

		public TokensController() {
			DefaultClassPathResourceLoader loader = new DefaultClassPathResourceLoader(
					this.getClass().getClassLoader());
			Optional<InputStream> testResource = loader.getResourceAsStream("classpath:EsgOffsetApiResponse.json");
			Assertions.assertTrue(testResource.isPresent());
			try (InputStream testData = testResource.get()) {
				testResponse = new String(testData.readAllBytes(), StandardCharsets.UTF_8);
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		@Get("/tokens/{tokenId}/nfts/{serialNumber}")
		public String getDetails(String tokenId, long serialNumber) {
			Assertions.assertEquals("0.0.48243577", tokenId);
			Assertions.assertEquals(3031, serialNumber);
			return testResponse;
		}

	}

}
