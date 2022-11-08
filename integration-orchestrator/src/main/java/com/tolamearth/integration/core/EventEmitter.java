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

package com.tolamearth.integration.core;

import io.micronaut.core.annotation.Introspected;
import lombok.Data;
import lombok.NoArgsConstructor;
import reactor.core.publisher.FluxSink;

import java.util.Objects;

@Data
@Introspected
@NoArgsConstructor
public class EventEmitter<T> {

	private FluxSink<T> sink;

	public void emit(T token) {
		if (Objects.nonNull(this.sink)) {
			this.sink.next(token);
		}
	}

	public void registerSink(FluxSink<T> sink) {
		this.sink = sink;
	}

}