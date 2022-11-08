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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

public record Transaction(String bytes, @JsonProperty("charged_tx_fee") int chargedTxFee,
		@JsonProperty("consensus_timestamp") String consensusTimestamp, @JsonProperty("entity_id") String entityId,
		@JsonProperty("max_fee") int maxFee, @JsonProperty("memo_base64") String memoBase64, String name, String node,
		int nonce, @JsonProperty("parent_consensus_timestamp") String parentConsensusTimestamp, String result,
		boolean scheduled, @JsonProperty("transaction_hash") String transactionHash,
		@JsonProperty("transaction_id") String transactionId,
		@JsonProperty("nft_transfers") List<NftTransfer> nftTransfers,
		@JsonProperty("token_transfers") List<TokenTransfer> tokenTransfers, List<Transfer> transfers,
		@JsonProperty("valid_duration_seconds") int validDurationSeconds,
		@JsonProperty("valid_start_timestamp") String validStartTimestamp) {

	@Builder
	public Transaction {
	}
}
