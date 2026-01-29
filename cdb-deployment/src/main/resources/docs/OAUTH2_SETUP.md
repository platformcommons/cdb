# OAuth2 with PKCE Setup Guide

This guide explains how to set up OAuth2 with PKCE protocol for CDB platform authentication.

## Features Implemented

- ✅ OAuth2 Authorization Code flow with PKCE
- ✅ Customizable login screen with CDB branding
- ✅ Customizable consent screen
- ✅ Forgot password functionality
- ✅ Database-based OAuth2 client management
- ✅ "Login with CDB" button integration in API Registry UI

## Setup Instructions

### 1. Database Setup

Run the following SQL to create the OAuth2 client:

```sql
-- Run this in your MySQL database
source 000_initial_setup.sql;
```

### 2. Start Auth Registry

```bash
cd cdb-auth-registry
mvn spring-boot:run
```

The auth server will be available at: http://localhost:8083

### 3. Start API Registry

```bash
cd cdb-api-registry
mvn spring-boot:run
```

The API registry will be available at: http://localhost:8082

### 4. Test OAuth2 Flow

1. Visit http://localhost:8082
2. Click "Login with CDB" button in the header
3. You'll be redirected to the CDB login page
4. Enter credentials and authorize
5. You'll be redirected back to the API registry

## OAuth2 Endpoints

### Authorization Server (Auth Registry - Port 8083)

- **Authorization Endpoint**: `/oauth2/authorize`
- **Token Endpoint**: `/oauth2/token`
- **Login Page**: `/oauth2/login`
- **Consent Page**: `/oauth2/consent`
- **Forgot Password**: `/oauth2/forgot-password`
- **JWKS**: `/.well-known/jwks.json`

### Client Application (API Registry - Port 8082)

- **Callback URL**: `/auth/callback`
- **Client ID**: `cdb_api_registry`

## Customization

### Login Page Styling

Edit `/templates/oauth2/login.html` to customize:
- Colors and branding
- Logo and client information display
- Form styling

### Consent Page

Edit `/templates/oauth2/consent.html` to customize:
- Permission descriptions
- Client information display
- Button styling

### Client Configuration

Use the OAuth2 Client Management API:

```bash
# Create new client
POST /api/v1/oauth2/clients
{
  "clientName": "My App",
  "description": "My application description",
  "redirectUris": ["http://localhost:3000/callback"],
  "scopes": ["read", "write"],
  "requireConsent": true,
  "logoUrl": "https://example.com/logo.png"
}

# List clients
GET /api/v1/oauth2/clients

# Update client
PUT /api/v1/oauth2/clients/{id}
```

## Security Features

- **PKCE**: Code challenge/verifier for public clients
- **State Parameter**: CSRF protection
- **Short-lived Codes**: 10-minute authorization code expiry
- **Secure Storage**: Encrypted client secrets
- **JWT Tokens**: RS256 signed access tokens

## Environment Variables

### Auth Registry (.env)
```
CDB_AUTH_REGISTRY_PORT=8083
CDB_DB_URL=jdbc:mysql://localhost:3306/cdb_auth_registry_db
```

### API Registry UI (.env)
```
VITE_CDB_AUTH_URL=http://localhost:8083
```

## Troubleshooting

### Common Issues

1. **Invalid redirect URI**: Ensure the redirect URI is registered for the client
2. **PKCE verification failed**: Check code_verifier is properly stored and sent
3. **State mismatch**: Ensure state parameter is preserved across requests
4. **Token expired**: Authorization codes expire in 10 minutes

### Debug Mode

Enable debug logging in `application.yml`:

```yaml
logging:
  level:
    com.platformcommons.cdb.auth.registry: DEBUG
    org.springframework.security: DEBUG
```

## Production Deployment

1. **Use HTTPS**: All OAuth2 flows must use HTTPS in production
2. **Secure Secrets**: Use environment variables for client secrets
3. **Database Security**: Encrypt sensitive data at rest
4. **Rate Limiting**: Implement rate limiting on auth endpoints
5. **Monitoring**: Monitor failed authentication attempts

## API Documentation

Full API documentation is available at:
- Auth Registry: http://localhost:8083/swagger-ui.html
- API Registry: http://localhost:8082/swagger-ui.html