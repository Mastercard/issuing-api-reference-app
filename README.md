# Mastercard Processing APMEA Issuing APIs - Reference Application

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Mastercard_issuing-api-reference-app&metric=alert_status)](https://sonarcloud.io/dashboard?id=Mastercard_issuing-api-reference-app)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=Mastercard_issuing-api-reference-app&metric=coverage)](https://sonarcloud.io/dashboard?id=Mastercard_issuing-api-reference-app)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=Mastercard_issuing-api-reference-app&metric=vulnerabilities)](https://sonarcloud.io/dashboard?id=Mastercard_issuing-api-reference-app)
[![](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/Mastercard/issuing-api-reference-app/blob/master/LICENSE)

## Table of Contents
- [Overview](#overview)
- [Pre-requisites](#prerequisites)
  * [Mastercard Account](#mcdeveloper)
  * [Certificates/Keys](#certificates)
  * [Softwares/Libraries](#softwares)
- [Usage](#getting-started)
  * [Step 1 - Clone git project](#clone)
  * [Step 2 (Optional) - Copy latest API Specs](#specs)
  * [Step 3 - Certificates Configuration](#certificates-config)
	  * [3.1 Download & install certificates](#install-certificates)
	  * [3.2 mTLS Certificate configuration](#mtls-certificates-config)
	  * [3.3 PIN block Encryption Certificate configuration](#pin-encryption-config)
	  * [3.4 Data Encryption Certificate configuration](#data-encryption-config)
		* [3.4.1 Option 1 (Recommended) - Different AES key for request & response](#two-key-encryption-config)
		* [3.4.2 Option 2 - Common AES key for request & response](#one-key-encryption-config)
  * [Step 4 (Optional) - Generating The API Client Sources & Review](#codegen)
  * [Step 5 - Build the project](#build)
  * [Step 6 - Execute the Use-Cases](#execute-the-use-cases)
- [Execute the Use-Cases](#execute-the-use-cases)
- [Use-Cases](#use-cases)
- [API Reference](#api-reference)
  * [OpenAPI Specs](#open-api-specs)
  * [API Client Helper](#api-client-heler)
      * [API Client](#api-client)
      * [SSL Context for mTLS](#ssl-context)
      * [PIN block formation and encryption](#pin-block-encryption)
      * [Data Encryption and Decryption](#encryption-and-decryption)
      * [Request/Response Logging](#logging)
  * [Other References](#references)
- [Support](#support)
- [License](#license)

<hr/><br/>

## Overview  <a name="overview"></a>

Mastercard Processing APMEA Issuing Digital First provides card processing platform to deliver a secure and seamless customer experience for prepaid, debit, and credit products.

Mastercard Processing APMEA Issuing platform enables:
- instantly issue physical or digital primary, add-on, and supplementary prepaid cards from anywhere, anytime
- handling card profile activities
- manage and control all card activities seamlessly
- safe and secure solution to handle all PIN-related activities for cards
- processing all card transaction activities

This is a reference application to demonstrate how Mastercard Processing APMEA Issuing Digital First APIs can be consumed.
Issuing Digital First APIs support prepaid, debit, and credit cards processing.

This Reference application is a guide for using Issuing APIs for cards processing. Please visit Mastercard Developer portal for more details about the APIs: [Mastercard Developers](https://developer.mastercard.com/product/mi-issuing).

<hr/><br/>

## Pre-requisites  <a name="prerequisites"></a>

### 1. Mastercard Account <a name="mcdeveloper"></a>
- [Mastercard Developers portal account](https://developer.mastercard.com/dashboard)

### 2. Certificates/Keys <a name="certificates"></a>
- **mTLS Client Certificate**: Customers need to use the [Key Management Portal (KMP)](https://www.mastercardconnect.com/-/store-plus/item-details/A/ckmp) hosted on [Mastercard Connect](https://www.mastercardconnect.com/) to provision MTLS client certificates. Contact: [KMD team](mailto:key_management@mastercard.com) to onboard.

mTLS Certificate Approval - Create a new project on [Mastercard Developers](https://developer.mastercard.com/dashboard) and add ***MI Issuing*** APIs to it and upload the mTLS certificate and click continue and wait for on-boarding & approval.

- **RSA Public Key/Certificate for Data Encryption** - Customers need to request certificates/keys for PIN encryption and API request encryption using above process.

- **RSA Private Key/Certificate for Data Decryption** - Customers need to generate RSA key pair and share public key with Mastercard.

<u>Note</u>: **Sandbox APIs are instantly accessible** without following the above process to get certificates/keys from Mastercard Key Management team. Required certificates/keys are available on [Mastercard Developers Portal](https://developer.mastercard.com/product/mi-issuing) for Sandbox environment (simulates the production environment with dummy data).

### 3. Softwares/Libraries <a name="softwares"></a>
- [Java 8 or later](https://www.azul.com/downloads/?package=jdk#download-openjdk)
- [Apache Maven 3.3+](https://maven.apache.org/download.cgi)
- [OpenAPI Generator](https://openapi-generator.tech/docs/plugins/)
- A text Editor or any IDE (We will refer to IntelliJ IDEA for this guide) with [Lombok plugins](https://projectlombok.org/setup/overview)

Note: Set up the `JAVA_HOME` environment variable to match the location of your Java installation.

<hr/><br/>

## Usage <a name="getting-started"></a>

### Step 1 - Clone git project <a name="clone"></a>
Clone the project from GitHub - 
```
git clone https://github.com/Mastercard/issuing-api-reference-app.git
```

<BR/>

### Step 2 (Optional) - Copy latest API Specs <a name="specs"></a>

- Download the latest API specs from [Mastercard Developer portal](#api-reference), if required.
- Copy these specifications in reference application source code - `src\main\resources\yaml_specs`

<BR/>

### Step 3 - Certificates Configuration <a name="certificates-config"></a>

Let's assume that you wish to configure and test Mastercard Issuing API available in `sandbox` environment.

Note: You may create a environment specific folder like (```src\main\resources\mtf```) or (```src\main\resources\prod```) and do similar configurations and pass the JVM argument `env` like `-Denv=mtf` while executing the code so that all configurations can be picked from this environment specific folder.

#### **3.1 Download & install certificates<a name="install-certificates"></a>
Download & install the mTLS certificate** and **encryption certificate** and configure the paths & credentials in application.properties file in reference application source code (```src/main/resources/sandbox```).

#### **3.2 mTLS Certificate configuration**:<a name="mtls-certificates-config"></a>
Configurations in **src/main/resources/sandbox/application.properties** 

|Property Name| Description|
|--|--|
|mi.api.mtls.keystore.file| Path to mTLS certificate keystore (PKCS #12, including chain) .pfx file |
|mi.api.mtls.keystore.password| Password of the Keystore that contains the mTLS certificate referred above file. |

**Note**: Here, reference application makes use of keystore file for the purpose of demo. But it is recommended to store your private certificate/key in physical devices such as USB Tokens, Smart Cards, or Hardware Storage Module (HSM) and write your own code to extract the certificate from your device rather than fetching it from keystore.

#### **3.3 PIN block Encryption Certificate configuration**:<a name="pin-encryption-config"></a>
Configurations in **src/main/resources/sandbox/application.properties**
 
|Property Name| Description|
|--|--|
|mi.api.pin.encryption.tdea.public.key.file| Path to card PIN block (ISO Format 0) encryption certificate keystore (PKCS #8, excluding chain) file that contains Mastercard RSA Public key .cer file|


#### **3.4 Data Encryption Certificate configuration**:<a name="data-encryption-config"></a>

3.4.1 **Option 1 (Recommended)** - Different AES key for request & response - Mastercard & Client both sharing their Public Key <a name="two-key-encryption-config"></a>

Configurations in **src/main/resources/sandbox/application.properties**
 
|Property Name| Description|
|--|--|
|mi.api.encryption.public.key.file| Path to data encryption certificate keystore (PKCS #8, excluding chain) file that contains Mastercard RSA Public key .cer file|
|mi.api.encryption.public.key.fingerprint| The hex-encoded SHA-256 digest of Mastercard RSA public key referred above|
|mi.api.encryption.oaep.algorithm| OAEP (Optimal Asymmetric Encryption Padding) padding digest algorithm used together with RSA encryption. For example, SHA256, SHA512, NONE|
|mi.api.encryption.private.key.keystore.file| Path to data decryption certificate keystore (PKCS #12, excluding chain) file that contains your RSA Private key .cer file. Ideally it should be retrieved from hardware device |
|mi.api.encryption.private.key.keystore.password| Password of the Keystore that contains the data decryption Private Key referred above file. |

3.4.2 **Option 2** - Common AES key for request & response - only Mastercard shares their Public Key<a name="one-key-encryption-config"></a>

Configurations in **src/main/resources/sandbox/application.properties**

|Property Name| Description|
|--|--|
|mi.api.encryption.public.key.file| Path to data encryption certificate keystore (PKCS #8, excluding chain) file that contains Mastercard RSA Public key .cer file|
|mi.api.encryption.public.key.fingerprint| The hex-encoded SHA-256 digest of Mastercard RSA public key referred above|
|mi.api.encryption.oaep.algorithm| OAEP (Optimal Asymmetric Encryption Padding) padding digest algorithm used together with RSA encryption. For example, SHA256, SHA512, NONE|
 
 
**Note**: Please refer to the sample values provided in reference application properties file.

<BR/>

### Step 4 (Optional) - Generating The API Client Sources & Review <a name="codegen"></a>

Now that you have all the dependencies you need, you can generate the sources.
We have already integrated OpenAPI Generator Maven Plugin as well as OpenAPI Specs of all the API in the pom.xml which generates API client libraries using configured OpenAPI Specs. OpenAPI Generator provides generators and library templates for supporting multiple languages and frameworks.

If you don't wish to review the generated code or API client reference code then you may skip this step since [next step - Build](#build) covers the generation of client source as well.

To generate the code, use one of the following two methods:

#### Option 1 - Using ```Terminal```

Navigate to the root directory of the project within a terminal window and execute below command:
```
mvn clean compile
```

#### Option 2 - Using IDE
- Ensure that [Lombok plugins](https://projectlombok.org/setup/overview) is installed
- Method 1 - In IntelliJ IDEA, open the Maven window (View > Tool Windows > Maven). Click the icons ```Reimport All Maven Projects``` and ```Generate Sources and Update Folders for All Projects```
- Method 2 - In the same menu, navigate to the commands ({Project name} > Lifecycle), select ```clean``` and ```compile``` then click the icon ```Run Maven Build```.


<BR/>

### Step 5 - Build the project <a name="build"></a>

Once you have updated the certificates & properties, you are ready to build the application. You can do this by navigating to the project's base directory from the terminal and then by running the following command.
```
mvn clean install -Dmaven.test.skip=true
```

<BR/>

### Step 6 - Execute the Use-Cases <a name="execute-the-use-cases"></a>
When the project builds successfully, you can run the following command to start/run the Spring Boot application:
 
```
java -Denv=sandbox -jar target/issuing-api-reference-app-1.0.3.jar  
```

<B> Note: Checkout the API response in the `output_<env>` folder (relative path). </B>

- Add argument ```search-cards``` or ```get-card``` in above command to execute/test each API individually. For example, 
 * Run below command to execute/test the Search Cards ( ```/card-management/cards/searches```) API
```
java -Denv=sandbox -jar target/issuing-api-reference-app-1.0.3.jar search-cards
```

 * Run below command to execute/test multiple APIs like Search Cards ( ```/card-management/cards/searches```) API and Get Card (```/card-management/cards/{card_id}```) API
```
java -Denv=sandbox -jar target/issuing-api-reference-app-1.0.3.jar search-cards get-card
```

 * Run below command to find the complete list of use cases available in reference-app for testing
```
java -Denv=sandbox -jar target/issuing-api-reference-app-1.0.3.jar list
```

 * Run below command to execute/test all the use cases available in reference-app for testing
```
java -Denv=sandbox -jar target/issuing-api-reference-app-1.0.3.jar all
```
                                                                      
**NOTE:**   
    - For MTF & PROD environment, update request with valid details in JSON files under location ```/src/main/resources/<env>/sample_requests/``` in order to execute these apis successfully. 

<hr/><br/>

## Service Use-Cases Documentation <a name="use-cases"></a>
Latest Open API use-cases documentation can be found here: 
- [Card Issuance Use Cases](https://developer.mastercard.com/card-issuance/documentation/use-cases/)
- [Card Management Use Cases](https://developer.mastercard.com/card-management/documentation/use-cases/)
- [Card Controls Use Cases](https://developer.mastercard.com/card-controls/documentation/use-cases/)
- [Authorization Management Use Cases](https://developer.mastercard.com/authorization-management/documentation/use-cases/)
- Transaction Management Use Cases (coming soon...)

<hr/><br/>

## API Reference <a name="api-reference"></a>

### OpenAPI Specs <a name="open-api-specs"></a>
Latest Open API specifications can be found here: 
- [Card Issuance API Specs](https://developer.mastercard.com/card-issuance/documentation/api-reference/)
- [Card Management API Specs](https://developer.mastercard.com/card-management/documentation/api-reference/)
- [Card Controls API Specs](https://developer.mastercard.com/card-controls/documentation/api-reference/)
- [Authorization Management API Specs](https://developer.mastercard.com/authorization-management/documentation/api-reference/)
- Transaction Management API Specs (coming soon...)

To develop a client application that consumes a Issuing RESTful APIs with Spring Boot.

### API Client Helper <a name="api-client-heler"></a>
The `com.mastercard.developer.issuing.client.helper.ApiClientHelper` class assists you with all the prerequisites for calling Issuing APIs.
It covers all the below mentioned points:

<br/>
#### API Client <a name="api-client"></a>
It is recommended to create an instance of `ApiClient` per thread in a multithreaded environment to avoid any potential issues. You can use the `getApiClient` static method of `ApiClientHelper` class to create new instance of `ApiClient` and initialize all the required configurations.

#### SSL Context for mTLS <a name="ssl-context"></a>
The `ApiClientHelper` class also helps in setting up `SSLContext` based on the configurations provided in the `application.properties` file and set to `OkHttpClient.Builder` instance. `OkHttpClient.Builder` instances are cached for each service (unique URI).

#### PIN block formation and encryption <a name="pin-block-encryption"></a>
The `com.mastercard.developer.issuing.client.helper.PinBlockTDEAEncrypter` is a singleton class that you can use when to encrypt card PIN. It consists of PIN block formation and encryption process.

#### Data Encryption and Decryption <a name="encryption-and-decryption"></a>
The `com.mastercard.developer.interceptors` provides `OkHttpFieldLevelEncryptionInterceptor` and `OkHttpGetRequestEncryptionHeaderInterceptor` class that you can use when configuring your API client. This class will take care of encrypting payload before sending the request and decrypting payload after receiving the response.

#### Request/Response Logging <a name="logging"></a>
The `com.mastercard.developer.interceptors.OkHttpLoggingInterceptor` class provides you the request and response logging functionality for debugging purpose.
The request/response may contain sensitive data, hence this logging must be disabled by simply changing the `mi.client.debug.mode` property value to `false` in `application.properties` file.  

<br/>
### Other References <a name="references"></a>
- [OpenAPI Generator (Maven Plugin)](https://openapi-generator.tech/docs/plugins/)


<hr/><br/>

## Support <a name="support"></a>
If you would like further information, please send an email to apisupport@mastercard.com

<hr/><br/>

## License <a name="license"></a>
Copyright 2023 Mastercard
 
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 
       http://www.apache.org/licenses/LICENSE-2.0
 
Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
