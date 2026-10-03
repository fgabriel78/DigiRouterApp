# router-protocol Specification

## Purpose
Provides low-level encrypted communication, cryptographic session establishment, and TR-181 data model RPC primitives for interacting with the TP-Link XGB430v Pro (ESDIGI) router web API.

## Requirements

### Requirement: Unauthenticated Parameter Discovery
The system SHALL retrieve public cryptographic parameters and administrative lock status from the router before attempting authentication.

#### Scenario: Fetch GDPR public key parameters
- **GIVEN** a network connection to the router's base address
- **WHEN** the client queries `POST /cgi/getGDPRParm` with an empty text body
- **THEN** the system extracts the RSA modulus hex string (`nn`), public exponent hex string (`ee`), and sequence counter (`seq`).

#### Scenario: Query router busy status
- **GIVEN** a network connection to the router's base address
- **WHEN** the client queries `POST /cgi/getBusy` with an empty text body
- **THEN** the system determines whether an existing user session is active (`isLogined`) and whether the administration lock is engaged (`isBusy`).

### Requirement: Cryptographic Request and Response Encryption
The system SHALL encrypt all data model RPC requests using AES-128-CBC and sign payloads using raw RSA-512 without padding, conforming to the router's GDPR encryption scheme.

#### Scenario: Encrypt RPC request payload
- **GIVEN** an initialized `GdprCrypto` instance with RSA modulus, exponent, session credentials, and sequence number
- **WHEN** the client prepares a payload for transmission
- **THEN** the plain text is encrypted with AES-128-CBC using a 16-digit key and IV, encoded as Base64, and bundled with an RSA-512 encrypted signature containing the user credentials MD5 hash and sequence length.

#### Scenario: Decrypt RPC response payload
- **GIVEN** an encrypted Base64-encoded response from the `/cgi_gdpr?9` endpoint
- **WHEN** the client processes the response body
- **THEN** the payload is decrypted with AES-128-CBC using the active session key and IV into a UTF-8 JSON string, falling back to clear text if the router returned an unencrypted error.

### Requirement: Authentication and Session Token Management
The system SHALL authenticate administrative credentials against `/cgi/login` and maintain the session cookie and `TokenID` header across subsequent requests.

#### Scenario: Successful login with admin credentials
- **GIVEN** valid username and password credentials
- **WHEN** the client submits an encrypted login request to `/cgi/login`
- **THEN** the system verifies the return code is 0, saves session cookies in an internal cookie jar, requests the root document `/`, and extracts the `TokenID` for future requests.

#### Scenario: Failed login with invalid credentials
- **GIVEN** invalid username or password credentials
- **WHEN** the client submits an encrypted login request to `/cgi/login`
- **THEN** the system resets cryptographic state and throws a `RouterException` containing the router error code (e.g., 71233).

#### Scenario: Session termination on logout
- **GIVEN** an active authenticated session
- **WHEN** `logout()` is invoked
- **THEN** the client dispatches an encrypted call to `/cgi/logout`, clears session cookies, resets the token ID to "0", and invalidates the session state.

### Requirement: TR-181 Data Model Operations Execution
The system SHALL execute TR-181 object operations (`go`, `gl`, `gs`, `so`, `ao`, `do`, `op`, `cgi`) against the `/cgi_gdpr?9` endpoint using standard JSON envelopes.

#### Scenario: Read single object instance
- **GIVEN** an authenticated session and an object identifier (`oid`)
- **WHEN** `get(oid)` is called
- **THEN** the client dispatches an operation with `operation="go"`, `stack="0,0,0,0,0,0"`, and returns the extracted JSON object.

#### Scenario: Read multi-instance list
- **GIVEN** an authenticated session and an object identifier (`oid`)
- **WHEN** `getList(oid)` is called
- **THEN** the client dispatches an operation with `operation="gl"` and returns the JSON array or object containing all instances.

#### Scenario: Write modified object attributes
- **GIVEN** an authenticated session, an object identifier (`oid`), and a map of changed attributes
- **WHEN** `set(oid, attrs, stack)` is called
- **THEN** the client dispatches an operation with `operation="so"`, transmits only the changed key-value pairs, and validates that the router returned a success response.

#### Scenario: Add new object instance
- **GIVEN** an authenticated session, a multi-instance object identifier, initial attributes, and an optional parent stack (`pstack`)
- **WHEN** `add(oid, attrs, parentStack)` is called
- **THEN** the client dispatches an operation with `operation="ao"` and includes the parent stack reference.

#### Scenario: Delete object instance
- **GIVEN** an authenticated session, an object identifier, and an instance stack identifier
- **WHEN** `delete(oid, stack)` is called
- **THEN** the client dispatches an operation with `operation="do"` targeted at the specific instance stack.

#### Scenario: Execute router action trigger
- **GIVEN** an authenticated session and an action object identifier (e.g., `ACT_REBOOT`, `ACT_WIFI_RELOAD_MLO`)
- **WHEN** `operate(oid, attrs)` is called
- **THEN** the client dispatches an operation with `operation="op"`.

### Requirement: Transport Headers and Error Propagation
The system SHALL include browser-like HTTP headers (`User-Agent`, `Referer`, `TokenID`) to avoid HTTP 406 rejections and throw descriptive exceptions when the router returns an error.

#### Scenario: Browser emulation headers
- **GIVEN** any HTTP request constructed by `RouterClient`
- **WHEN** the request is sent over HTTP
- **THEN** the headers contain `User-Agent` mimicking Android Chrome, `Referer` pointing to `baseUrl/`, and the current `TokenID`.

#### Scenario: Router business error response
- **GIVEN** an RPC call that results in `success: false` or a non-zero `errorcode`
- **WHEN** the response JSON is parsed
- **THEN** a `RouterException` is thrown carrying the specific integer error code and context message.
