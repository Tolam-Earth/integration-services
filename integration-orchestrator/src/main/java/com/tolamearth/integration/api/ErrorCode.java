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

package com.tolamearth.integration.api;

public class ErrorCode {

	public static ApiError HTTP_STATUS_400_ERROR_1001 = ApiError.builder().code("1001")
			.message("Missing required field").build();

	public static ApiError HTTP_STATUS_400_ERROR_1003 = ApiError.builder().code("1003").message("Invalid data").build();

	public static ApiError HTTP_STATUS_404_ERROR_1004 = ApiError.builder().code("1004").message("Unknown resource")
			.build();

	public static ApiError HTTP_STATUS_415_ERROR_1002 = ApiError.builder().code("1002").message("Invalid data format")
			.build();

}
