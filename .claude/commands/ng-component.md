# /ng-component — Generate Angular Standalone Component

Generate a production-ready Angular 17+ standalone component for the CDB platform.

## Usage
```
/ng-component <ComponentName> [--feature <featurePath>] [--type list|form|detail|shell] [--store]
```

## Arguments: $ARGUMENTS

## Instructions

Parse `$ARGUMENTS` to extract:
- `ComponentName` — PascalCase name of the component
- `--feature` — path within the `ui/src/app/` directory (default: infer from name)
- `--type` — component type: `list` (data table), `form` (create/edit), `detail` (read-only view), `shell` (route container)
- `--store` — if present, also generate a NgRx Signals Store

### Step 1: Discover existing structure
1. Find the target `ui/` directory — check `cdb-master-data-engine/ui`, `cdb-provider-registry/ui`, `cdb-api-registry/ui`
2. Read `src/app/app.routes.ts` to understand current routing
3. Read one existing component file to follow patterns already in place

### Step 2: Generate the component

**File structure to create:**
```
src/app/<feature>/
├── <feature-name>.routes.ts          (if new route needed)
├── <component-name>.component.ts
├── <component-name>.component.html
└── <component-name>.component.scss
```

If `--store` is passed, also create:
```
<component-name>.store.ts
```

**Component template to follow:**

`<component-name>.component.ts`:
```typescript
import { ChangeDetectionStrategy, Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'cdb-<selector-name>',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './<component-name>.component.html',
  styleUrl: './<component-name>.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class <ComponentName>Component implements OnInit {
  // inject services via inject()

  ngOnInit(): void { }
}
```

**Type-specific patterns:**

- **list**: Include `data-testid="<name>-row"` on each table row, loading skeleton, empty state, and pagination
- **form**: Use typed `FormGroup<T>`, include validation error messages, `data-testid` on inputs and buttons
- **detail**: Read-only display, async pipe for data loading, skeleton loader
- **shell**: Minimal layout wrapper with `<router-outlet>` and breadcrumb placeholder

**NgRx Signals Store (when --store):**
```typescript
export const <ComponentName>Store = signalStore(
  withState<{ items: <Model>[]; isLoading: boolean; error: string | null }>({
    items: [], isLoading: false, error: null
  }),
  withMethods((store, service = inject(<ComponentName>Service)) => ({
    loadAll: rxMethod<void>(pipe(
      tap(() => patchState(store, { isLoading: true })),
      switchMap(() => service.findAll().pipe(
        tapResponse({
          next: items => patchState(store, { items, isLoading: false }),
          error: (e: Error) => patchState(store, { error: e.message, isLoading: false })
        })
      ))
    ))
  }))
);
```

### Step 3: Update routes
Add the new component to the appropriate routes file using `loadComponent` (lazy).

### Step 4: Report
State what files were created/modified and what imports need to be added.

## Output Requirements
- Zero NgModules (standalone only)
- `ChangeDetectionStrategy.OnPush` always
- `inject()` not constructor injection
- `data-testid` attributes on all interactive elements
- No inline styles — use `.scss` file
