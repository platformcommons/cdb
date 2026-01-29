# Commons Digital Backbone (CDB) SDK Technical Documentation

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Security Model](#security-model)
4. [API Specifications](#api-specifications)
5. [SDK Implementation Guide](#sdk-implementation-guide)
6. [Integration Flow](#integration-flow)
7. [Code Examples](#code-examples)
8. [Error Handling](#error-handling)
9. [Best Practices](#best-practices)

---

## Overview

The Commons Digital Backbone (CDB) is a secure interoperability platform that enables service providers to integrate with each other's APIs through standardized SDKs. The platform ensures:

- **Authentication**: Verify the identity of API consumers
- **Authorization**: Ensure requests come from registered providers
- **Confidentiality**: Encrypt sensitive data end-to-end
- **Integrity**: Detect any tampering of data in transit
- **Non-repudiation**: Cryptographically prove the origin of requests

### Key Components

1. **CDB Gateway**: Central registry that manages provider information
2. **Client SDK**: Used by API consumers (Provider A)
3. **Server SDK**: Used by API providers (Provider B)
4. **Cryptographic Keys**: RSA key pairs for each provider

---

## Architecture

### System Architecture Diagram

```
┌─────────────┐                    ┌─────────────┐
│ Provider A  │                    │ Provider B  │
│  (Client)   │                    │  (Server)   │
│             │                    │             │
│ ┌─────────┐ │                    │ ┌─────────┐ │
│ │ Client  │ │                    │ │ Server  │ │
│ │  SDK    │ │                    │ │  SDK    │ │
│ └────┬────┘ │                    │ └────▲────┘ │
└──────┼──────┘                    └──────┼──────┘
       │                                  │
       │  1. Get Provider B Info          │
       ├──────────────┐                   │
       │              ▼                   │
       │         ┌─────────────┐          │
       │         │     CDB     │          │
       │         │   Gateway   │          │
       │         └─────────────┘          │
       │                                  │
       │  2. Encrypted & Signed Request   │
       └──────────────────────────────────┤
                                          │
                                    3. Decrypt &
                                       Verify
```

### Data Flow

1. **Registration Phase**: Providers register with CDB Gateway
   - Generate RSA-4096 key pair
   - Submit public key, base URL, and metadata
   - Receive unique provider code

2. **Discovery Phase**: Provider A discovers Provider B
   - Query CDB Gateway for Provider B's information
   - Retrieve base URL and public key

3. **Communication Phase**: Secure API call
   - Provider A signs and encrypts request
   - Provider B decrypts and verifies request
   - Provider B processes and responds

---

## Security Model

### Cryptographic Algorithms

#### Asymmetric Encryption
- **Algorithm**: RSA with OAEP padding
- **Key Size**: 4096 bits (minimum 2048 bits)
- **Padding**: OAEP with SHA-256
- **Purpose**: Encrypt symmetric keys and verify signatures

#### Symmetric Encryption
- **Algorithm**: AES-GCM
- **Key Size**: 256 bits
- **Mode**: Galois/Counter Mode (GCM)
- **IV Size**: 96 bits (12 bytes)
- **Tag Size**: 128 bits (16 bytes)
- **Purpose**: Encrypt payload data

#### Digital Signatures
- **Algorithm**: RSA-PSS with SHA-256
- **Hash Function**: SHA-256
- **Salt Length**: Maximum (matches hash output)
- **Purpose**: Verify message authenticity and integrity

### Hybrid Encryption Approach

The SDK uses hybrid encryption for optimal security and performance:

1. **Payload Encryption**: AES-256-GCM (fast for large data)
2. **Key Encryption**: RSA-4096-OAEP (secure key exchange)
3. **Digital Signature**: RSA-PSS (authenticity verification)

**Why Hybrid?**
- RSA is too slow for large payloads
- AES requires secure key exchange
- Combining both provides security and performance

---

## API Specifications

### CDB Gateway REST API

#### Get Provider Information

**Endpoint**: `GET /api/providers/{providerCode}`

**Request Headers**:
```
Content-Type: application/json
```

**Response** (200 OK):
```json
{
  "providerCode": "PROVIDER_B",
  "baseUrl": "https://api.providerb.com",
  "publicKey": "MIICIjANBgkqhkiG9w0BAQ...",
  "name": "Provider B Services",
  "status": "active",
  "registeredAt": "2025-01-15T10:30:00Z"
}
```

**Error Responses**:
- `404 Not Found`: Provider not found
- `500 Internal Server Error`: Server error

---

### Secure Request Format

#### Request Headers

```
Content-Type: application/json
X-ENCRYPTED: true|false
X-SIGNATURE: <base64-encoded-signature>
X-PROVIDER-CODE: <sender-provider-code>
```

#### Unencrypted Request Body

```json
{
  "encrypted": false,
  "payload": "{\"action\":\"getData\",\"params\":{\"id\":123}}",
  "signature": "SGVsbG8gV29ybGQ...",
  "providerCode": "PROVIDER_A"
}
```

#### Encrypted Request Body

```json
{
  "encrypted": true,
  "encryptedData": "a3JhYmFzZSBiYXNlNjQ...",
  "encryptedKey": "ZW5jcnlwdGVkIGFl...",
  "iv": "cmFuZG9tIGl2...",
  "signature": "c2lnbmF0dXJlIGRh...",
  "providerCode": "PROVIDER_A"
}
```

---

## SDK Implementation Guide

This section provides detailed specifications for implementing CDB SDKs in any programming language.

### Client SDK Requirements

#### 1. Initialization

**Required Environment Variables**:
```
CDB_GATEWAY_URL=https://gateway.cdb.platform
CLIENT_PROVIDER_CODE=PROVIDER_A
CLIENT_PRIVATE_KEY=<base64-encoded-private-key-der>
```

**Initialization Steps**:
1. Load environment variables
2. Validate all required variables are present
3. Parse and load private key (DER format, Base64 encoded)
4. Initialize HTTP client
5. Initialize provider information cache

#### 2. Get Provider Information

**Method Signature**: `getProviderInfo(providerCode: string): ProviderInfo`

**Implementation Steps**:
1. Check cache for provider information
2. If not cached, make HTTP GET request to CDB Gateway
3. Parse response JSON
4. Extract `baseUrl` and `publicKey`
5. Store in cache
6. Return ProviderInfo object

**Caching Strategy**:
- Use in-memory cache (HashMap/Dictionary)
- No expiration (assumes provider info is stable)
- Cache key: provider code

#### 3. Sign Payload

**Method Signature**: `signPayload(payload: string): string`

**Implementation Steps**:
1. Convert payload string to bytes (UTF-8 encoding)
2. Create signature instance with SHA-256
3. Initialize with private key
4. Update with payload bytes
5. Generate signature
6. Encode signature as Base64
7. Return Base64 string

**Algorithm Details**:
```
Algorithm: RSA-PSS
Hash: SHA-256
MGF: MGF1 with SHA-256
Salt Length: Maximum (same as hash output)
```

#### 4. Encrypt Payload

**Method Signature**: `encryptPayload(payload: string, publicKey: string): EncryptedPayload`

**Implementation Steps**:

**Step 4.1: Generate AES Key and IV**
```
aesKey = generateRandomBytes(32)  // 256-bit key
iv = generateRandomBytes(12)      // 96-bit IV for GCM
```

**Step 4.2: Encrypt Payload with AES-GCM**
```
cipher = AES-GCM(key=aesKey, iv=iv)
ciphertext = cipher.encrypt(payload.toBytes('utf-8'))
tag = cipher.getTag()  // 16 bytes authentication tag
encryptedData = ciphertext + tag
```

**Step 4.3: Encrypt AES Key with RSA**
```
rsaCipher = RSA-OAEP(hash=SHA-256, mgf=MGF1-SHA-256)
encryptedKey = rsaCipher.encrypt(aesKey, publicKey)
```

**Step 4.4: Encode and Return**
```
return EncryptedPayload(
    encryptedData = base64Encode(encryptedData),
    encryptedKey = base64Encode(encryptedKey),
    iv = base64Encode(iv)
)
```

#### 5. Create Secure Request

**Method Signature**: `createSecureRequest(payload: string, targetProviderCode: string, encrypt: boolean): SecureRequest`

**Implementation Steps**:
1. Get provider information for target
2. Sign the payload
3. If encrypt is true:
   - Encrypt payload with provider's public key
   - Create SecureRequest with encrypted data
4. If encrypt is false:
   - Create SecureRequest with plain payload
5. Add signature and provider code
6. Return SecureRequest object

---

### Server SDK Requirements

#### 1. Initialization

**Required Environment Variables**:
```
CDB_GATEWAY_URL=https://gateway.cdb.platform
SERVER_PROVIDER_CODE=PROVIDER_B
SERVER_PRIVATE_KEY=<base64-encoded-private-key-der>
```

**Initialization Steps**:
1. Load environment variables
2. Validate all required variables are present
3. Parse and load private key
4. Initialize HTTP client
5. Initialize public key cache

#### 2. Parse Request Headers

**Method Signature**: `parseHeaders(headers: Map<string, string>): RequestHeaders`

**Implementation Steps**:
1. Extract `X-ENCRYPTED` header (convert to boolean)
2. Extract `X-SIGNATURE` header
3. Extract `X-PROVIDER-CODE` header
4. Validate required headers are present
5. Return RequestHeaders object

#### 3. Decrypt Payload

**Method Signature**: `decryptPayload(encryptedData: string, encryptedKey: string, iv: string): string`

**Implementation Steps**:

**Step 3.1: Decrypt AES Key with RSA**
```
encryptedKeyBytes = base64Decode(encryptedKey)
rsaCipher = RSA-OAEP(hash=SHA-256, mgf=MGF1-SHA-256)
aesKey = rsaCipher.decrypt(encryptedKeyBytes, serverPrivateKey)
```

**Step 3.2: Prepare Encrypted Data**
```
encryptedDataBytes = base64Decode(encryptedData)
ciphertext = encryptedDataBytes[0:-16]  // All except last 16 bytes
tag = encryptedDataBytes[-16:]          // Last 16 bytes
ivBytes = base64Decode(iv)
```

**Step 3.3: Decrypt with AES-GCM**
```
cipher = AES-GCM(key=aesKey, iv=ivBytes, tag=tag)
decryptedBytes = cipher.decrypt(ciphertext)
payload = decryptedBytes.toString('utf-8')
```

**Step 3.4: Return Decrypted Payload**
```
return payload
```

#### 4. Verify Signature

**Method Signature**: `verifySignature(payload: string, signature: string, senderPublicKey: PublicKey): boolean`

**Implementation Steps**:
1. Decode signature from Base64
2. Convert payload to bytes (UTF-8)
3. Create signature verifier with SHA-256
4. Initialize with sender's public key
5. Update with payload bytes
6. Verify signature
7. Return true if valid, false otherwise
8. Catch any exceptions and return false

**Algorithm Details**:
```
Algorithm: RSA-PSS
Hash: SHA-256
MGF: MGF1 with SHA-256
Salt Length: Maximum
```

#### 5. Get Provider Public Key

**Method Signature**: `getProviderPublicKey(providerCode: string): PublicKey`

**Implementation Steps**:
1. Check cache for public key
2. If not cached, make HTTP GET request to CDB Gateway
3. Parse response JSON
4. Extract and decode `publicKey`
5. Load public key from DER format
6. Store in cache
7. Return PublicKey object

#### 6. Process Request

**Method Signature**: `processRequest(request: SecureRequest): ProcessedRequest`

**Implementation Steps**:
1. Check if request is encrypted
2. If encrypted:
   - Decrypt payload using server private key
3. If not encrypted:
   - Use payload directly
4. Get sender's public key from CDB Gateway
5. Verify signature of payload
6. Return ProcessedRequest with:
   - Decrypted payload
   - Signature validation status
   - Sender provider code

---

## Integration Flow

### Complete Integration Flow Diagram

```
Provider A (Client)                CDB Gateway              Provider B (Server)
      |                                  |                           |
      |  1. Initialize Client SDK        |                           |
      |  - Load private key              |                           |
      |  - Load configuration            |                           |
      |                                  |                           |
      |  2. Get Provider B Info          |                           |
      |--------------------------------->|                           |
      |  GET /api/providers/PROVIDER_B   |                           |
      |                                  |                           |
      |  3. Provider B Information       |                           |
      |<---------------------------------|                           |
      |  {baseUrl, publicKey}            |                           |
      |                                  |                           |
      |  4. Prepare Request Payload      |                           |
      |  payload = {"action": "getData"} |                           |
      |                                  |                           |
      |  5. Sign Payload                 |                           |
      |  signature = RSA-PSS-Sign(       |                           |
      |    payload, clientPrivateKey)    |                           |
      |                                  |                           |
      |  6. Encrypt Payload              |                           |
      |  a. Generate AES-256 key         |                           |
      |  b. Encrypt payload with AES-GCM |                           |
      |  c. Encrypt AES key with         |                           |
      |     Provider B's RSA public key  |                           |
      |                                  |                           |
      |  7. Send Encrypted Request       |                           |
      |---------------------------------------------------------------->|
      |  POST /api/endpoint              |                           |
      |  Headers:                        |                           |
      |    X-ENCRYPTED: true             |                           |
      |    X-SIGNATURE: <sig>            |                           |
      |    X-PROVIDER-CODE: PROVIDER_A   |                           |
      |  Body:                           |                           |
      |    {encryptedData, encryptedKey, |                           |
      |     iv, signature, providerCode} |                           |
      |                                  |                           |
      |                                  |    8. Initialize Server SDK|
      |                                  |    - Load private key     |
      |                                  |                           |
      |                                  |    9. Check X-ENCRYPTED   |
      |                                  |    header = true          |
      |                                  |                           |
      |                                  |    10. Decrypt AES Key    |
      |                                  |    with Server Private Key|
      |                                  |                           |
      |                                  |    11. Decrypt Payload    |
      |                                  |    with AES-GCM           |
      |                                  |                           |
      |                                  |    12. Get Provider A Info|
      |                                  |<--------------------------|
      |                                  |  GET /api/providers/      |
      |                                  |      PROVIDER_A           |
      |                                  |-------------------------->|
      |                                  |  {publicKey}              |
      |                                  |                           |
      |                                  |    13. Verify Signature   |
      |                                  |    RSA-PSS-Verify(        |
      |                                  |      payload, signature,  |
      |                                  |      providerAPublicKey)  |
      |                                  |                           |
      |                                  |    14. Process Request    |
      |                                  |    if signature valid     |
      |                                  |                           |
      |  15. Response                    |                           |
      |<----------------------------------------------------------------|
      |  {status: "success", data: ...}  |                           |
```

### Step-by-Step Integration Process

#### Provider A (Client) Steps:

1. **Initialize SDK**
   ```
   client = new CDBClient()
   ```

2. **Discover Provider B**
   ```
   providerInfo = client.getProviderInfo("PROVIDER_B")
   baseUrl = providerInfo.baseUrl
   publicKey = providerInfo.publicKey
   ```

3. **Prepare Payload**
   ```
   payload = JSON.stringify({
     action: "getData",
     params: { id: 123 }
   })
   ```

4. **Create Secure Request**
   ```
   secureRequest = client.createSecureRequest(
     payload,
     "PROVIDER_B",
     encrypt = true
   )
   ```

5. **Send HTTP Request**
   ```
   POST {baseUrl}/api/endpoint
   Headers:
     Content-Type: application/json
     X-ENCRYPTED: true
     X-SIGNATURE: {secureRequest.signature}
     X-PROVIDER-CODE: {secureRequest.providerCode}
   Body:
     {secureRequest.toJSON()}
   ```

#### Provider B (Server) Steps:

1. **Initialize SDK**
   ```
   server = new CDBServer()
   ```

2. **Parse Incoming Request**
   ```
   headers = server.parseHeaders(httpRequest.headers)
   body = parseJSON(httpRequest.body)
   ```

3. **Create SecureRequest Object**
   ```
   secureRequest = new SecureRequest(
     encrypted: body.encrypted,
     encryptedData: body.encryptedData,
     encryptedKey: body.encryptedKey,
     iv: body.iv,
     signature: body.signature,
     providerCode: body.providerCode
   )
   ```

4. **Process Request**
   ```
   processed = server.processRequest(secureRequest)
   ```

5. **Validate Signature**
   ```
   if (!processed.signatureValid) {
     return HTTP 401 Unauthorized
   }
   ```

6. **Process Business Logic**
   ```
   payload = parseJSON(processed.payload)
   result = handleBusinessLogic(payload)
   return HTTP 200 OK with result
   ```

---

## Code Examples

### Example 1: Client SDK Usage (Pseudocode)

```javascript
// Initialize client
const client = new CDBClient();

// Get provider information
const providerB = client.getProviderInfo("PROVIDER_B");
console.log(`Provider B URL: ${providerB.baseUrl}`);

// Prepare request
const payload = JSON.stringify({
  action: "createOrder",
  params: {
    customerId: "CUST-123",
    items: [
      { productId: "PROD-456", quantity: 2 }
    ]
  }
});

// Create secure request with encryption
const secureRequest = client.createSecureRequest(
  payload,
  "PROVIDER_B",
  true  // encrypt = true
);

// Send HTTP request
const response = await fetch(`${providerB.baseUrl}/api/orders`, {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'X-ENCRYPTED': secureRequest.encrypted ? 'true' : 'false',
    'X-SIGNATURE': secureRequest.signature,
    'X-PROVIDER-CODE': secureRequest.providerCode
  },
  body: JSON.stringify(secureRequest.toDict())
});

const result = await response.json();
console.log('Order created:', result);
```

### Example 2: Server SDK Usage (Pseudocode)

```javascript
// Initialize server
const server = new CDBServer();

// Express.js example
app.post('/api/orders', async (req, res) => {
  try {
    // Parse headers
    const headers = server.parseHeaders(req.headers);
    
    // Parse body
    const body = req.body;
    
    // Create secure request object
    const secureRequest = new SecureRequest({
      encrypted: body.encrypted,
      encryptedData: body.encryptedData,
      encryptedKey: body.encryptedKey,
      iv: body.iv,
      signature: body.signature,
      providerCode: body.providerCode,
      payload: body.payload
    });
    
    // Process request
    const processed = server.processRequest(secureRequest);
    
    // Validate signature
    if (!processed.signatureValid) {
      return res.status(401).json({
        error: 'Invalid signature',
        code: 'SIGNATURE_INVALID'
      });
    }
    
    // Parse decrypted payload
    const payload = JSON.parse(processed.payload);
    
    // Business logic
    const order = await createOrder(payload.params);
    
    // Return response
    res.json({
      status: 'success',
      orderId: order.id,
      sender: processed.senderProviderCode
    });
    
  } catch (error) {
    console.error('Processing error:', error);
    res.status(500).json({
      error: error.message,
      code: 'PROCESSING_ERROR'
    });
  }
});
```

### Example 3: Key Generation (Pseudocode)

```python
# Generate RSA key pair for provider registration
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.primitives import serialization
import base64

# Generate private key
private_key = rsa.generate_private_key(
    public_exponent=65537,
    key_size=4096
)

# Generate public key
public_key = private_key.public_key()

# Serialize private key (DER format)
private_key_der = private_key.private_bytes(
    encoding=serialization.Encoding.DER,
    format=serialization.PrivateFormat.PKCS8,
    encryption_algorithm=serialization.NoEncryption()
)

# Serialize public key (DER format)
public_key_der = public_key.public_bytes(
    encoding=serialization.Encoding.DER,
    format=serialization.PublicFormat.SubjectPublicKeyInfo
)

# Encode as Base64 for storage
private_key_b64 = base64.b64encode(private_key_der).decode('utf-8')
public_key_b64 = base64.b64encode(public_key_der).decode('utf-8')

print(f"Private Key (Base64): {private_key_b64}")
print(f"Public Key (Base64): {public_key_b64}")

# Store private key in .env file (keep secure!)
# Store public key in CDB Gateway during registration
```

---

## Error Handling

### Error Codes and Handling

#### Client SDK Errors

| Error Code | Description | Handling Strategy |
|------------|-------------|-------------------|
| `PROVIDER_NOT_FOUND` | Target provider not registered in CDB | Check provider code, notify user |
| `INVALID_PUBLIC_KEY` | Cannot parse provider's public key | Contact CDB support |
| `ENCRYPTION_FAILED` | Payload encryption failed | Retry with different data, check key format |
| `SIGNING_FAILED` | Signature generation failed | Check private key, retry |
| `NETWORK_ERROR` | Cannot reach CDB Gateway | Retry with exponential backoff |
| `INVALID_RESPONSE` | Malformed response from CDB Gateway | Log and alert operations |

#### Server SDK Errors

| Error Code | Description | Handling Strategy |
|------------|-------------|-------------------|
| `DECRYPTION_FAILED` | Cannot decrypt payload | Return 400 Bad Request |
| `SIGNATURE_INVALID` | Signature verification failed | Return 401 Unauthorized, log attempt |
| `SENDER_NOT_FOUND` | Sender provider not in CDB | Return 403 Forbidden |
| `MALFORMED_REQUEST` | Missing required fields | Return 400 Bad Request |
| `KEY_FETCH_FAILED` | Cannot get sender's public key | Return 500, retry internally |

### Error Response Format

```json
{
  "error": "Human-readable error message",
  "code": "ERROR_CODE",
  "timestamp": "2025-09-30T10:30:00Z",
  "requestId": "req-123-456-789",
  "details": {
    "field": "additionalInfo"
  }
}
```

### Retry Strategy

#### Client-Side Retries
```
For network errors:
  Retry with exponential backoff
  Max retries: 3
  Base delay: 1 second
  Backoff multiplier: 2
  Max delay: 10 seconds

For 5xx errors:
  Retry up to 2 times
  
For 4xx errors:
  Do not retry (client error)
```

#### Server-Side Error Logging
```
All signature validation failures:
  - Log sender provider code
  - Log timestamp
  - Log payload hash (for debugging)
  - Alert security team if threshold exceeded

All decryption failures:
  - Log error type
  - Check key rotation status
  - Alert operations
```

---

## Best Practices

### Security Best Practices

#### 1. Key Management

**DO:**
- Generate keys with minimum 2048 bits (4096 recommended)
- Store private keys in secure environment variables or key vaults
- Use hardware security modules (HSM) for production
- Rotate keys annually or after suspected compromise
- Use different keys for different environments (dev/staging/prod)

**DON'T:**
- Hard-code private keys in source code
- Commit private keys to version control
- Share private keys between environments
- Reuse keys across different providers
- Store keys in plaintext files

#### 2. Request Validation

**Always Verify:**
- Signature before processing any request
- Provider code matches expected sender
- Timestamp to prevent replay attacks (implement in business logic)
- Request size limits to prevent DoS

**Implementation:**
```javascript
// Add timestamp to payload
const payload = {
  timestamp: Date.now(),
  data: { /* your data */ }
};

// Server-side validation
const requestAge = Date.now() - payload.timestamp;
if (requestAge > 300000) { // 5 minutes
  throw new Error('Request expired');
}
```

#### 3. Encryption Decisions

**When to Encrypt:**
- Personal Identifiable Information (PII)
- Financial data
- Health records
- Authentication credentials
- Proprietary business data

**When Plain Signing is Sufficient:**
- Public data
- Non-sensitive metadata
- High-throughput scenarios where performance matters
- Internal network communications (with network-level encryption)

#### 4. Logging and Monitoring

**Log These Events:**
- All signature verification failures
- Decryption errors
- Provider authentication attempts
- Key rotation events
- Unusual request patterns

**DO NOT Log:**
- Private keys
- Decrypted payloads containing PII
- Full request bodies (use hash instead)
- Encryption keys or IVs

#### 5. Performance Optimization

**Caching:**
```javascript
// Cache provider information
const providerCache = new Map();
const CACHE_TTL = 3600000; // 1 hour

function getProviderInfo(code) {
  const cached = providerCache.get(code);
  if (cached && Date.now() - cached.timestamp < CACHE_TTL) {
    return cached.data;
  }
  
  const data = fetchFromGateway(code);
  providerCache.set(code, {
    data: data,
    timestamp: Date.now()
  });
  
  return data;
}
```

**Connection Pooling:**
- Reuse HTTP connections to CDB Gateway
- Use connection pools for database queries
- Implement circuit breakers for external calls

**Async Processing:**
- Process signature verification asynchronously
- Use message queues for high-volume scenarios
- Implement batch processing for multiple requests

---

## Advanced Topics

### 1. Webhook Integration

For asynchronous notifications, Provider B can send webhooks to Provider A:

```javascript
// Provider B sending webhook
const client = new CDBClient();
const webhookPayload = JSON.stringify({
  event: 'order.completed',
  orderId: 'ORD-123',
  timestamp: Date.now()
});

const secureRequest = client.createSecureRequest(
  webhookPayload,
  "PROVIDER_A",
  true
);

await fetch(providerA.webhookUrl, {
  method: 'POST',
  headers: {
    'X-ENCRYPTED': 'true',
    'X-SIGNATURE': secureRequest.signature,
    'X-PROVIDER-CODE': 'PROVIDER_B'
  },
  body: JSON.stringify(secureRequest.toDict())
});
```

### 2. Request Idempotency

Implement idempotency to handle duplicate requests:

```javascript
// Client: Add idempotency key
const payload = {
  idempotencyKey: generateUUID(),
  data: { /* request data */ }
};

// Server: Check idempotency
const idempotencyCache = new Map();

function processIdempotentRequest(request) {
  const key = request.payload.idempotencyKey;
  
  if (idempotencyCache.has(key)) {
    return idempotencyCache.get(key); // Return cached response
  }
  
  const result = processRequest(request);
  idempotencyCache.set(key, result);
  
  return result;
}
```

### 3. Rate Limiting

Implement rate limiting to prevent abuse:

```javascript
// Server-side rate limiting
const rateLimits = new Map();

function checkRateLimit(providerCode) {
  const now = Date.now();
  const limit = rateLimits.get(providerCode) || {
    count: 0,
    resetTime: now + 60000 // 1 minute window
  };
  
  if (now > limit.resetTime) {
    limit.count = 0;
    limit.resetTime = now + 60000;
  }
  
  if (limit.count >= 100) { // 100 requests per minute
    throw new Error('Rate limit exceeded');
  }
  
  limit.count++;
  rateLimits.set(providerCode, limit);
}
```

### 4. Multi-Region Support

Handle multiple CDB Gateway regions:

```javascript
class CDBClient {
  constructor() {
    this.regions = [
      'https://gateway-us.cdb.platform',
      'https://gateway-eu.cdb.platform',
      'https://gateway-asia.cdb.platform'
    ];
    this.currentRegion = this.selectOptimalRegion();
  }
  
  selectOptimalRegion() {
    // Implement latency-based selection
    // or use geo-location
    return this.regions[0];
  }
  
  async getProviderInfoWithFailover(code) {
    for (const region of this.regions) {
      try {
        return await this.fetchFromRegion(region, code);
      } catch (error) {
        console.warn(`Region ${region} failed, trying next`);
      }
    }
    throw new Error('All regions failed');
  }
}
```

---

## Testing Guide

### Unit Testing

#### Client SDK Tests

```javascript
describe('CDBClient', () => {
  test('should sign payload correctly', () => {
    const client = new CDBClient();
    const payload = '{"test": "data"}';
    const signature = client.signPayload(payload);
    
    expect(signature).toBeDefined();
    expect(signature.length).toBeGreaterThan(0);
  });
  
  test('should encrypt and decrypt payload', () => {
    const client = new CDBClient();
    const server = new CDBServer();
    
    const payload = '{"sensitive": "data"}';
    const encrypted = client.encryptPayload(
      payload,
      server.getPublicKey()
    );
    
    const decrypted = server.decryptPayload(
      encrypted.encryptedData,
      encrypted.encryptedKey,
      encrypted.iv
    );
    
    expect(decrypted).toEqual(payload);
  });
  
  test('should cache provider information', async () => {
    const client = new CDBClient();
    
    const info1 = await client.getProviderInfo('TEST_PROVIDER');
    const info2 = await client.getProviderInfo('TEST_PROVIDER');
    
    expect(info1).toBe(info2); // Same object reference
  });
});
```

#### Server SDK Tests

```javascript
describe('CDBServer', () => {
  test('should verify valid signature', () => {
    const client = new CDBClient();
    const server = new CDBServer();
    
    const payload = '{"test": "data"}';
    const signature = client.signPayload(payload);
    
    const isValid = server.verifySignature(
      payload,
      signature,
      client.getPublicKey()
    );
    
    expect(isValid).toBe(true);
  });
  
  test('should reject invalid signature', () => {
    const server = new CDBServer();
    
    const payload = '{"test": "data"}';
    const fakeSignature = 'invalid-signature';
    
    const isValid = server.verifySignature(
      payload,
      fakeSignature,
      somePublicKey
    );
    
    expect(isValid).toBe(false);
  });
  
  test('should process encrypted request', async () => {
    const client = new CDBClient();
    const server = new CDBServer();
    
    const payload = '{"action": "test"}';
    const secureRequest = client.createSecureRequest(
      payload,
      'TEST_PROVIDER',
      true
    );
    
    const processed = await server.processRequest(secureRequest);
    
    expect(processed.payload).toEqual(payload);
    expect(processed.signatureValid).toBe(true);
  });
});
```

### Integration Testing

```javascript
describe('End-to-End Integration', () => {
  test('should complete full request cycle', async () => {
    // Setup
    const providerA = new CDBClient({
      code: 'PROVIDER_A',
      privateKey: providerAPrivateKey
    });
    
    const providerB = new CDBServer({
      code: 'PROVIDER_B',
      privateKey: providerBPrivateKey
    });
    
    // Provider A creates request
    const payload = JSON.stringify({
      action: 'getData',
      params: { id: 123 }
    });
    
    const secureRequest = providerA.createSecureRequest(
      payload,
      'PROVIDER_B',
      true
    );
    
    // Provider B receives and processes
    const processed = await providerB.processRequest(secureRequest);
    
    // Assertions
    expect(processed.signatureValid).toBe(true);
    expect(processed.payload).toEqual(payload);
    expect(processed.senderProviderCode).toEqual('PROVIDER_A');
  });
});
```

---

## SDK Implementation Checklist

Use this checklist when implementing a CDB SDK in a new language:

### Client SDK Checklist

- [ ] Environment configuration loading
- [ ] Private key loading and parsing (DER format)
- [ ] HTTP client initialization
- [ ] Provider information caching
- [ ] `getProviderInfo()` method with HTTP GET to Gateway
- [ ] `signPayload()` method with RSA-PSS signing
- [ ] `encryptPayload()` method with hybrid encryption
  - [ ] AES-256-GCM symmetric encryption
  - [ ] RSA-OAEP key encryption
  - [ ] Proper IV generation
  - [ ] Authentication tag handling
- [ ] `createSecureRequest()` method
- [ ] Error handling for all operations
- [ ] Unit tests for all methods
- [ ] Integration tests
- [ ] Documentation with code examples

### Server SDK Checklist

- [ ] Environment configuration loading
- [ ] Private key loading and parsing
- [ ] HTTP client initialization
- [ ] Public key caching
- [ ] `parseHeaders()` method
- [ ] `getProviderPublicKey()` method with HTTP GET
- [ ] `decryptPayload()` method
  - [ ] RSA-OAEP key decryption
  - [ ] AES-256-GCM data decryption
  - [ ] Authentication tag verification
- [ ] `verifySignature()` method with RSA-PSS
- [ ] `processRequest()` method
- [ ] Error handling and logging
- [ ] Unit tests for all methods
- [ ] Integration tests
- [ ] Documentation with code examples

---

## Troubleshooting

### Common Issues and Solutions

#### Issue 1: Signature Verification Fails

**Symptoms:**
- Server returns 401 Unauthorized
- `signatureValid` is false

**Possible Causes:**
1. Payload was modified after signing
2. Wrong private/public key pair
3. Different encoding used (e.g., UTF-16 vs UTF-8)
4. Whitespace differences in JSON

**Solutions:**
```javascript
// Ensure consistent JSON serialization
const payload = JSON.stringify(data, null, 0); // No whitespace

// Verify key pair
console.log('Public key hash:', hashPublicKey(publicKey));

// Log payload being signed
console.log('Signing payload:', payload);

// Check encoding
const payloadBytes = Buffer.from(payload, 'utf-8');
```

#### Issue 2: Decryption Fails

**Symptoms:**
- Error: "Decryption failed" or "Invalid ciphertext"
- Exception during `decryptPayload()`

**Possible Causes:**
1. Wrong private key
2. Corrupted encrypted data
3. Incorrect Base64 encoding/decoding
4. Authentication tag mismatch

**Solutions:**
```javascript
// Verify encrypted data integrity
console.log('Encrypted data length:', encryptedData.length);
console.log('Encrypted key length:', encryptedKey.length);

// Check Base64 encoding
try {
  const decoded = base64Decode(encryptedData);
  console.log('Decoded successfully');
} catch (error) {
  console.error('Base64 decoding failed:', error);
}

// Ensure proper tag handling
const ciphertext = encryptedBytes.slice(0, -16);
const tag = encryptedBytes.slice(-16);
```

#### Issue 3: Provider Not Found

**Symptoms:**
- 404 error from CDB Gateway
- "Provider not found" message

**Possible Causes:**
1. Incorrect provider code
2. Provider not registered
3. Provider deactivated
4. Wrong CDB Gateway URL

**Solutions:**
```javascript
// Verify provider code
console.log('Looking for provider:', providerCode);

// Check Gateway URL
console.log('CDB Gateway:', cdbGatewayUrl);

// Test Gateway connectivity
const response = await fetch(cdbGatewayUrl + '/health');
console.log('Gateway status:', response.status);
```

#### Issue 4: Performance Issues

**Symptoms:**
- Slow request processing
- High CPU usage
- Memory leaks

**Solutions:**
```javascript
// Implement caching
const cache = new LRUCache({ max: 100, ttl: 3600000 });

// Use connection pooling
const agent = new https.Agent({
  keepAlive: true,
  maxSockets: 50
});

// Profile encryption operations
console.time('encryption');
const encrypted = encryptPayload(payload, publicKey);
console.timeEnd('encryption');

// Monitor memory usage
console.log('Memory:', process.memoryUsage());
```

---

## Appendix

### A. Environment Variable Template

`.env` file template for Client SDK:
```bash
# CDB Gateway Configuration
CDB_GATEWAY_URL=https://gateway.cdb.platform

# Provider Configuration
CLIENT_PROVIDER_CODE=PROVIDER_A

# Security Keys (Base64 encoded DER format)
CLIENT_PRIVATE_KEY=MIIJKAIBAAKCAgEA...

# Optional: Performance tuning
REQUEST_TIMEOUT=30000
MAX_RETRIES=3
CACHE_TTL=3600000
```

`.env` file template for Server SDK:
```bash
# CDB Gateway Configuration
CDB_GATEWAY_URL=https://gateway.cdb.platform

# Provider Configuration
SERVER_PROVIDER_CODE=PROVIDER_B

# Security Keys (Base64 encoded DER format)
SERVER_PRIVATE_KEY=MIIJKQIBAAKCAgEA...

# Optional: Security settings
MAX_REQUEST_SIZE=1048576
SIGNATURE_TIMEOUT=300000
ENABLE_RATE_LIMITING=true
```

### B. Cryptographic Standards Reference

- **RSA Key Size**: NIST SP 800-57 recommends 2048-bit minimum
- **AES Key Size**: 256-bit for high security (FIPS 140-2)
- **Hash Algorithm**: SHA-256 (FIPS 180-4)
- **RSA Padding**: OAEP with SHA-256 (PKCS#1 v2.2)
- **Signature Scheme**: PSS with SHA-256 (PKCS#1 v2.1)
- **AES Mode**: GCM for authenticated encryption (NIST SP 800-38D)

### C. Glossary

- **CDB**: Commons Digital Backbone
- **DER**: Distinguished Encoding Rules (binary key format)
- **GCM**: Galois/Counter Mode (authenticated encryption)
- **IV**: Initialization Vector
- **OAEP**: Optimal Asymmetric Encryption Padding
- **PII**: Personal Identifiable Information
- **PSS**: Probabilistic Signature Scheme
- **RSA**: Rivest-Shamir-Adleman (asymmetric encryption)
- **SDK**: Software Development Kit

### D. Support and Resources

- **CDB Gateway API Documentation**: https://docs.cdb.platform/api
- **Security Guidelines**: https://docs.cdb.platform/security
- **Provider Registration**: https://portal.cdb.platform/register
- **Support Email**: support@cdb.platform
- **Developer Forum**: https://forum.cdb.platform

---

## Conclusion

This technical documentation provides a complete specification for implementing CDB SDKs in any programming language. The security model ensures confidentiality, integrity, authentication, and non-repudiation for all inter-provider communications.

Key takeaways:
1. Use hybrid encryption (RSA + AES) for optimal security and performance
2. Always verify signatures before processing requests
3. Implement proper error handling and logging
4. Cache provider information to improve performance
5. Follow security best practices for key management

For additional assistance or clarification, please contact the CDB platform support team.