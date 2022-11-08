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

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.client.bind.AnnotatedClientArgumentRequestBinder;
import io.micronaut.http.client.bind.ClientRequestUriContext;
import jakarta.inject.Singleton;

@Singleton
public class TimestampFilterArgumentBinder implements AnnotatedClientArgumentRequestBinder<TimestampFilter> {

	@NonNull
	@Override
	public Class<TimestampFilter> getAnnotationType() {
		return TimestampFilter.class;
	}

	@Override
	public void bind(@NonNull ArgumentConversionContext<Object> context, @NonNull ClientRequestUriContext uriContext,
			@NonNull Object value, @NonNull MutableHttpRequest<?> request) {

		String parameterName = context.getAnnotationMetadata().stringValue(TimestampFilter.class)
				.filter(StringUtils::isNotEmpty).orElse(context.getArgument().getName());

		if (value instanceof String) {
			uriContext.addQueryParameter(context.getArgument().getName(), parameterName + value);
		}
	}

}