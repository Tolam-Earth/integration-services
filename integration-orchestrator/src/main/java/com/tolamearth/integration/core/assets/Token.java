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

import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Token {

	@EmbeddedId
	private NftId nftId;

	private String memo;

	private String projectCategory;

	private String projectType;

	private long quality;

	private long vintageYear;

	private String country;

	@EqualsAndHashCode.Exclude
	private String deviceId;

	@EqualsAndHashCode.Exclude
	private String guardianId;

	private String firstSubdivision;

	@OneToMany(mappedBy = "token", cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.EAGER)
	private List<TokenTransaction> transactions;

	public Token addTransaction(TokenTransaction transaction) {
		if (this.transactions == null) {
			this.transactions = new ArrayList<>();
		}
		transaction.setToken(this);
		this.transactions.add(transaction);
		return this;
	}

}
