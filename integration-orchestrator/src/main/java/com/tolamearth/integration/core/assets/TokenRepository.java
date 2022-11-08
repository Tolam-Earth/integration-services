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

package com.tolamearth.integration.core.assets;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.reactive.ReactorCrudRepository;
import reactor.core.publisher.Mono;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

@Repository
public abstract class TokenRepository implements ReactorCrudRepository<Token, NftId> {

	private final EntityManager entityManager;

	protected TokenRepository(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	@Transactional
	public Mono<Token> saveOrUpdate(Token token) {
		return findById(token.getNftId()).flatMap(storedToken -> {
			storedToken.getTransactions().addAll(token.getTransactions());
			return this.update(token);
		}).switchIfEmpty(this.save(token));
	}

}
