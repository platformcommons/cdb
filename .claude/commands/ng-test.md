# /ng-test — Generate Angular Tests

Generate comprehensive unit and component tests for an Angular file in the CDB platform.

## Usage
```
/ng-test <targetFilePath> [--type unit|component|e2e|all]
```

## Arguments: $ARGUMENTS

## Instructions

Parse `$ARGUMENTS` to extract:
- `targetFilePath` — path to the Angular file to test (service, component, store, pipe)
- `--type` — test type: `unit` (logic only), `component` (DOM + interactions), `e2e` (Cypress), `all`

### Step 1: Read the target file
Read `targetFilePath` completely to understand:
- What the class/function does
- All public methods and their signatures
- Dependencies it injects
- Signals and computed values
- Template structure (for component tests)

### Step 2: Generate tests based on file type

#### For Services
```typescript
describe('<ServiceName>Service', () => {
  let service: <ServiceName>Service;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        <ServiceName>Service,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(<ServiceName>Service);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  describe('findAll', () => {
    it('sends GET to correct URL with pagination params', () => {
      service.findAll(0, 10).subscribe();
      const req = httpTesting.expectOne(r => r.url.includes('/api/v1/<resource>'));
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('10');
      req.flush({ content: [], totalElements: 0 });
    });
  });

  describe('create', () => {
    it('sends POST with request body', () => {
      const request = { name: 'Test' };
      service.create(request).subscribe();
      const req = httpTesting.expectOne('/api/v1/<resource>');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush({ id: '1', name: 'Test' });
    });
  });

  describe('delete', () => {
    it('sends DELETE to resource URL', () => {
      service.delete('abc-123').subscribe();
      const req = httpTesting.expectOne('/api/v1/<resource>/abc-123');
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });
});
```

#### For NgRx Signals Stores
```typescript
describe('<StoreName>Store', () => {
  let store: InstanceType<typeof <StoreName>Store>;
  let serviceSpy: jest.Mocked<<StoreName>Service>;

  beforeEach(() => {
    serviceSpy = {
      findAll: jest.fn().mockReturnValue(of({ content: [], totalElements: 0 })),
      create: jest.fn(),
      delete: jest.fn().mockReturnValue(of(void 0)),
    } as any;

    TestBed.configureTestingModule({
      providers: [
        <StoreName>Store,
        { provide: <StoreName>Service, useValue: serviceSpy }
      ]
    });
    store = TestBed.inject(<StoreName>Store);
  });

  it('initial state is empty and not loading', () => {
    expect(store.items()).toEqual([]);
    expect(store.isLoading()).toBe(false);
    expect(store.error()).toBeNull();
  });

  it('loadAll sets items on success', fakeAsync(() => {
    const items = [{ id: '1', name: 'Test' }];
    serviceSpy.findAll.mockReturnValue(of({ content: items, totalElements: 1 }));
    store.loadAll();
    tick();
    expect(store.items()).toEqual(items);
    expect(store.isLoading()).toBe(false);
  }));

  it('loadAll sets error on failure', fakeAsync(() => {
    serviceSpy.findAll.mockReturnValue(throwError(() => new Error('Network error')));
    store.loadAll();
    tick();
    expect(store.error()).toBe('Network error');
    expect(store.isLoading()).toBe(false);
  }));
});
```

#### For Components
```typescript
describe('<ComponentName>Component', () => {
  let fixture: ComponentFixture<<ComponentName>Component>;
  let component: <ComponentName>Component;

  const mockStore = {
    items: signal<any[]>([]),
    isLoading: signal(false),
    error: signal<string | null>(null),
    loadAll: jest.fn(),
    select: jest.fn(),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [<ComponentName>Component],
      providers: [{ provide: <StoreName>Store, useValue: mockStore }]
    }).compileComponents();

    fixture = TestBed.createComponent(<ComponentName>Component);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('calls loadAll on init', () => {
    expect(mockStore.loadAll).toHaveBeenCalled();
  });

  it('shows loading skeleton when isLoading is true', () => {
    mockStore.isLoading.set(true);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="loading"]')).toBeTruthy();
  });

  it('shows empty state when no items', () => {
    mockStore.items.set([]);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="empty-state"]')).toBeTruthy();
  });

  it('renders items when data is available', () => {
    mockStore.items.set([{ id: '1', name: 'Test Item' }]);
    fixture.detectChanges();
    const rows = fixture.nativeElement.querySelectorAll('[data-testid="item-row"]');
    expect(rows).toHaveLength(1);
  });
});
```

#### For E2E (Cypress)
```typescript
describe('<Feature> Management', () => {
  beforeEach(() => {
    cy.intercept('GET', '/api/v1/<resource>*', {
      fixture: '<resource>-list.json'
    }).as('get<Resource>s');
    cy.visit('/<route>');
    cy.wait('@get<Resource>s');
  });

  it('displays items in the list', () => {
    cy.get('[data-testid="item-row"]').should('have.length.greaterThan', 0);
  });

  it('navigates to detail on row click', () => {
    cy.get('[data-testid="item-row"]').first().click();
    cy.url().should('include', '/<route>/');
  });

  it('creates a new item', () => {
    cy.intercept('POST', '/api/v1/<resource>', { statusCode: 201, body: { id: 'new-id', name: 'New Item' } }).as('create');
    cy.get('[data-testid="add-button"]').click();
    cy.get('[data-testid="name-input"]').type('New Item');
    cy.get('[data-testid="save-button"]').click();
    cy.wait('@create');
    cy.get('[data-testid="success-notification"]').should('be.visible');
  });

  it('shows error on network failure', () => {
    cy.intercept('POST', '/api/v1/<resource>', { statusCode: 500 }).as('createFail');
    cy.get('[data-testid="add-button"]').click();
    cy.get('[data-testid="name-input"]').type('Fail Item');
    cy.get('[data-testid="save-button"]').click();
    cy.wait('@createFail');
    cy.get('[data-testid="error-notification"]').should('be.visible');
  });
});
```

### Step 3: Add missing data-testid attributes
If the component template lacks `data-testid` attributes needed for tests, add them to the component's HTML file.

### Step 4: Create Cypress fixture file
Create `cypress/fixtures/<resource>-list.json` with sample data matching the model interface.

### Step 5: Report
List all generated test files, total test cases added, and any data-testid attributes added to the component template.
