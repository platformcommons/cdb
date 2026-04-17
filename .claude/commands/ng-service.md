# /ng-service — Generate Angular HTTP Service

Generate a typed Angular HTTP service for the CDB platform that connects to a Spring Boot REST API via the gateway.

## Usage
```
/ng-service <ServiceName> [--api <apiBasePath>] [--model <ModelName>]
```

## Arguments: $ARGUMENTS

## Instructions

Parse `$ARGUMENTS` to extract:
- `ServiceName` — PascalCase name (e.g., `Provider`, generates `ProviderService`)
- `--api` — REST base path (e.g., `/api/v1/providers`); if omitted, infer from ServiceName
- `--model` — Model interface name; if omitted, same as ServiceName

### Step 1: Discover context
1. Find the target `ui/src/app/` directory in the relevant service module
2. Read `src/environments/environment.ts` to understand `apiUrl` shape
3. Check if an existing service pattern exists to follow exactly

### Step 2: Generate model interfaces

`<service-name>.model.ts`:
```typescript
export interface <Model>Dto {
  id: string;
  // add fields from the Spring Boot entity
  createdAt: string;
  createdBy: string;
}

export interface Create<Model>Request {
  // required fields only, no id/audit fields
}

export interface Update<Model>Request {
  // all updatable fields
}

export interface <Model>Page {
  content: <Model>Dto[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
```

### Step 3: Generate the service

`<service-name>.service.ts`:
```typescript
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { <Model>Dto, Create<Model>Request, Update<Model>Request, <Model>Page } from './<service-name>.model';

@Injectable({ providedIn: 'root' })
export class <ServiceName>Service {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}<apiBasePath>`;

  findAll(page = 0, size = 20, sort = 'createdAt,desc'): Observable<<Model>Page> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', sort);
    return this.http.get<<Model>Page>(this.baseUrl, { params });
  }

  findById(id: string): Observable<<Model>Dto> {
    return this.http.get<<Model>Dto>(`${this.baseUrl}/${id}`);
  }

  create(request: Create<Model>Request): Observable<<Model>Dto> {
    return this.http.post<<Model>Dto>(this.baseUrl, request);
  }

  update(id: string, request: Update<Model>Request): Observable<<Model>Dto> {
    return this.http.put<<Model>Dto>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
```

### Step 4: Check app.config.ts
Verify `provideHttpClient(withInterceptors([authInterceptor]))` is configured. If not, add it.

### Step 5: Report
- List created files
- State what fields should be added to the model based on the Spring Boot entity (if readable)
- Note what `environment.apiUrl` is set to in the discovered environment file
