# Tolam Earth Integration Service

The Integration Service is a primarily event-driven microservice system that 
serves as a coordinating bridge between the Exchange Marketplace, the ARMM, 
and external services such as [Hedera](https://hedera.com/) and 
[Ledger Works](https://www.lworks.io/).

The service provides capabilities such as:
- Discovery of newly minted Offsets, retrieval of their full ESG details, and 
  storage and publishing of that data for the ARMM.
- Ingestion, storage, and downstream publishing to the ARMM of Offset listing 
  and purchasing events for Offsets from the Marketplace.
- A History service API that allows consumers to retrieve the full event 
  record for a given Offset.
- A Purchase service API that gives callers the ability purchase and Offset 
  that has been listed in the Marketplace.

## Building and Running The Service

The Integration Service is written in Java using 
[Mirconaut](https://micronaut.io) and is composed of two subprojects:

- **message-schemas** - This contains the Protocol Buffer schemas defined for
  exchanging messages via GCP PubSub with the Marketplace and ARMM. 
  The protobuf definitions are compiled into Java code as part of the project
  build.

- **integration-orchestrator** - The core Integration Service functionality 
  is contained here. This is a Micronaut application which can be built and 
  run locally, or as a Docker image.

### Prerequisites

The code is written for a Java 17 runtime, which must be installed on your 
system. We suggest using [SDKMan](https://sdkman.io/) to manage your local 
Java installations.

Building and running the service locally requires a local 
[Docker](https://www.docker.com/) installation. Docker must be up and running
as it is used to provide stand-ins for external Postgres and PubSub services 
both in executing the project's test suite as part of the build, and in order 
to run the service on a local machine.

The project uses [Gradle](https://gradle.org/) for its build system, but 
includes the self-contained Gradle wrapper, so a local system install of 
Gradle is not needed.

This project utilizes the [spring-javaformat](https://github.com/spring-io/spring-javaformat) 
plugin to ensure formatting consistency across the codebase. Details can be 
found in the provided link above for initializing the plugin within your 
preferred IDE.

### Building

To format the codebase, execute

`./gradlew format`

To build the service, execute

`./gradlew build`

on the command line from the project root folder.

To build the Docker image, execute

`./gradlew dockerBuild`

on the command line from the project root folder.

### Running

To run the service locally, execute

`./gradlew run`

on the command line from the project root folder.

## Assumptions and Next Steps

The system in its current form is intended as a Proof of Concept, and certain
assumptions have been made that are unlikely to remain as the system evolves
into a full production-ready cloud service.

- **Dependency-Free Deployment** - While the system has been designed with 
  the intention of the eventual execution environment being the GCP, it is 
  currently self-contained and does not rely on the production GCP services. 
  See, for example, the use of the GCP PubSub emulator as opposed to the full 
  service.

- **Pre-configured Offset Token IDs** - The service is able to discover newly 
  minted Offsets by polling the Ledger Works mirror node API, but the Token 
  IDs for those offsets must be pre-configured at startup time.

- **Placeholder Data** - There is some data that is specified in the message 
  schemas for which the service is currently mapping only certain assumed 
  values and in some cases generating random values for instances where the 
  desired information is not currently supplied. See the Data Mapping outline
  below.

Some of the next steps that will be taken to evolve the code to a fully 
production-ready system include:

- **Cloud Deployment** - The code will be refactored to run in the GCP Cloud 
  Run serverless execution environment, and will rely upon GCP services such 
  as PubSub and Postgres.

- **Push-Based Offset Discovery** - The Offset discovery service will move
  away from polling to using a push-based service provided by Ledger Works.

- **Robust Data Mapping** - Complete mapping of all possible permutations of
  the ESG data for Offset tokens.
  
## Application Configuration

### Required Environment Variables

#### Ledger Works API
- **LEDGER_WORKS_API_KEY** - The Ledger Works API key can be obtained by going to 
  https://www.lworks.io/ and create an account, then under 
  https://app.lworks.io/api-access-tokens you can create an access token for 
  either TestNet or MainNet.
- **HEDERA_NETWORK** - Hedera has two networks: testnet and mainnet. The network
  used must correspond to the Ledger Works API key.
- 
#### Hedera API
To obtain a Hedera operator ID and private key see the 
[Hedera Getting Started Documentation](https://docs.hedera.com/guides/getting-started/introduction).
- **ADMIN_OPERATOR_ID** - The admin Hedera account ID.
- **ADMIN_PRIVATE_KEY** - The admin Hedera private key.
- **BUYER_OPERATOR_ID** - The buyer Hedera account ID.
- **BUYER_PRIVATE_KEY** - The buyer Hedera private key.

#### General Application
- **HEM_OFFSETS_CONTRACT_ID** - The ID for the HEM smart contract on the Hedera Network.
- **TOKEN_DISCOVERY_POLL_INTERVAL** - How often, in seconds, the service will poll the Ledger Works 
  API for newly minted NFT tokens.
- **TOKEN_DISCOVERY_TOKEN_IDS** - A list of NFT token Ids that the service will use
  to poll for newly minted token offsets.
- **API_HEM_MARKETPLACE_CLIENT_URL** - The URL for the HEM Marketplace Client.

## Retrieving Newly Minted NFT Details From Ledger Works

### Overview
The Integration-Orchestrator service will poll the Ledger Works API, on a 
configurable interval, in order to retrieve all necessary details for newly
minted NFT tokens. The details for each token will then be stored on the 
service's database as a `Token.java` object, and then published on the
`nft_details` event topic to be consumed by the ARMM service.

### Polling Interval
The polling interval is handled by the `TokenDiscoveryScheduler.java` class,
and is configurable through the `token-discovery.poll-interval` variable
in the `application.yml` file, or by setting the `TOKEN_DISCOVERY_POLL_INTERVAL`
environment variable.

### Ledger Works API
The service uses a combination of endpoints within the `LedgerWorksClient.java` and 
`EsgClient.java` classes to aggregate all necessary details for a newly minted NFT.

The Ledger Works API key and URLs for each client are configurable using the following 
environment variables.

- `LEDGER_WORKS_API_KEY`
- `HEDERA_NETWORK`

To obtain a Ledger Works API key and URL go to https://www.lworks.io/ and create an account,
then under https://app.lworks.io/api-access-tokens you can create an access token
for either TestNet or MainNet.

### Publishing to ARMM
The `TokenPublisherService.java` will convert the details of each newly minted NFT into
an `ArmmEvent.java` (a Google Protocol Buffer within the `armm.proto` file), and then 
publish it as a serialized byte array on the `nft_details` topic.

## Recieving Transaction Events From Marketplace
The Integration-Orchestrator service will receive both LISTED and PURCHASED events from
the Marketplace through a subscription on the `pub_nft_marketplace_state_subscription`
topic. It will then update the existing NFT details on the service's database with the
latest transaction details. Finally, the updated NFT details will be published to the 
ARMM on the `nft_details` topic.

### Marketplace Subscription
The `MarketplaceEventListener.java` subscribes to the `pub_nft_marketplace_state_subscription`
topic, and will receive a serialized byte array of `MarketplaceEvent.class`, which is a 
Google Protocol Buffer within the `marketplace.proto` file.

### Publishing to ARMM
The `TokenPublisherService.java` will convert the updated details of each NFT into
an `ArmmEvent.java` (a Google Protocol Buffer within the `armm.proto` file), and then 
publish it as a serialized byte array on the `nft_details` topic.

## Integration-Orchestrator API Endpoints
The Integration-Orchestrator service currently exposes two API endpoints. One for getting NFT offset 
transaction history, and another for purchasing an NFT offset. 

The API version is configurable through 
the `api.integration-api-version` variable, and is currently set to `/integration/v1.`

### History API

`GET /offsets/{token_id},{serial_number}/transactions`
    
For more information see [Retrieve Offset History API](https://github.com/objectcomputing/hem-architecture/blob/main/integration/information/api/integration-api.md#retrieve-offset-history)

### Purchase API

`POST /offsets/buyer`

For more information see [Purchasing Offsets API](https://github.com/objectcomputing/hem-architecture/blob/main/integration/information/api/integration-api.md#purchasing-offsets)

Testing note: Before an offset can be purchased it must first be listed. This would have to be done
through either the Marketplace UI, or through the `OffsetListerTest.java` test class.

## Ledger Works Data Mapping

NFT details for newly minted tokens are obtained from a combination of Ledgerworks APIs, which are listed below. The required fields are then mapped to Integration-Orchestrator POJOs and stored on the service's database.
- https://docs.hedera.com/guides/docs/mirror-node-api/rest-api#transaction-by-transaction-id
- https://docs.hedera.com/guides/docs/mirror-node-api/rest-api#nft-info
- https://testnet.esg.api.lworks.io/api/v1/docs/index.html
- https://mainnet.esg.api.lworks.io/api/v1/docs/index.html

| Integration-Orchestrator - Token                   | Notes                                                | Legerworks Fields                                                                                                                             | Notes                                      |
| -------------------------------------------------- | ---------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------ |
| nftId.tokenId                                      |                                                      | transaction.nft_transfers.token_id                                                                                                            |                                            |
| nftId.serialNumber                                 |                                                      | transaction.nft_transfers.serial_number                                                                                                       |                                            |
| memo                                               |                                                      | transaction.memo_base64                                                                                                                       |                                            |
| projectCategory                                    |                                                      | If attribute is RENEWABLE ENERGY then set value as RENEW_ENERGY else if attribute is ENERGY EFFICIENCY then set value as COMM_ENRGY_EFF       |                                            |
| projectType                                        |                                                      | If attribute is GRID CONNECTED WIND then set value as WIND else if attribute is IMPROVED COOKSTOVE then set value as EMM_RED                  |                                            |
| quality                                            |                                                      | N/A                                                                                                                                           |                                            |
| vintageYear                                        |                                                      | VINTAGE                                                                                                                                       |                                            |
| country                                            |                                                      | if attribute is INDIA then set value as GJ                                                                                                    | default value is "01"                      |
| deviceId                                           |                                                      | Randomly generated UUID                                                                                                                       |                                            |
| guardianId                                         |                                                      | Randomly generated UUID                                                                                                                       |                                            |
| firstSubdivision                                   |                                                      | if attribute is GUJARAT then set value as GJ                                                                                                  | default value is "01"                      |
| transactions                                       | A list of TokenTransaction objects. See table below. |                                                                                                                                               |                                            |

| Integration-Orchestrator - TokenTransaction        | Notes                                                | Legerworks Fields                                                                                                                             | Notes                                      |
| -------------------------------------------------- | ---------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------ |
| id.nftId.tokenId                                   |                                                      | transaction.nft_transfers.token_id                                                                                                            |                                            |
| id.nftId.serialNumber                              |                                                      | transaction.nft_transfers.serial_number                                                                                                       |                                            |
| eventType                                          |                                                      | EventType.MINTED                                                                                                                              |                                            |
| transactionTime                                    |                                                      | transaction.consensus_timestamp                                                                                                               | If null a default string of "0.0" is used. |
| owner                                              |                                                      | transaction.nft_transfers.receiver_account_id                                                                                                 | If null a default empty string is used.    |
| listPrice                                          |                                                      | N/A                                                                                                                                           |                                            |
| purchasePrice                                      |                                                      | N/A                                                                                                                                           |                                            |
| token                                              | A Token object. See table above.                     |                                                                                                                                               |


## License

Copyright 2022 Tolam Earth

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at:

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
