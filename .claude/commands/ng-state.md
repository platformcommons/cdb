# /ng-state — Generate NgRx Signals Store

Generate a NgRx Signals Store for shared state management in a CDB Angular feature.

## Usage
```
/ng-state <StoreName> [--feature <featurePath>] [--scope root|route] [--entity <EntityName>]
```

## Arguments: $ARGUMENTS

## Instructions

Parse `$ARGUMENTS` to extract:
- `StoreName` — PascalCase name (e.g., `Provider`, generates `ProviderStore`)
- `--feature` — feature directory path under `src/app/`
- `--scope` — `root` (global singleton) or `route` (provided in route component only)
- `--entity` — if set, use `withEntities()` for normalized entity collection management

### Step 1: Decide scope
- `root`: store has `{ providedIn: 'root' }` — use for global concerns (user session, config)
- `route`: store is provided in a component's `providers: [StoreName]` — use for feature-local state

### Step 2: Generate store (without entities)
```typescript
import { signalStore, withState, withComputed, withMethods, patchState } from '@ngrx/signals';
import { computed, inject } from '@angular/core';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { tapResponse } from '@ngrx/operators';
import { pipe, switchMap, tap } from 'rxjs';
import { <StoreName>Service } from './<store-name>.service';
import { <StoreName>Dto, Create<StoreName>Request } from './<store-name>.model';

type <StoreName>State = {
  items: <StoreName>Dto[];
  selectedId: string | null;
  isLoading: boolean;
  isSaving: boolean;
  error: string | null;
};

const initialState: <StoreName>State = {
  items: [],
  selectedId: null,
  isLoading: false,
  isSaving: false,
  error: null,
};

export const <StoreName>Store = signalStore(
  // Use { providedIn: 'root' } for global scope:
  // withState(initialState) — no providedIn
  withState(initialState),

  withComputed(({ items, selectedId }) => ({
    selected<StoreName>: computed(() =>
      items().find(item => item.id === selectedId()) ?? null
    ),
    total<StoreName>s: computed(() => items().length),
    hasError: computed(() => !!error()),
  })),

  withMethods((store, service = inject(<StoreName>Service)) => ({
    loadAll: rxMethod<void>(
      pipe(
        tap(() => patchState(store, { isLoading: true, error: null })),
        switchMap(() =>
          service.findAll().pipe(
            tapResponse({
              next: page => patchState(store, { items: page.content, isLoading: false }),
              error: (err: Error) => patchState(store, { error: err.message, isLoading: false }),
            })
          )
        )
      )
    ),

    select(id: string): void {
      patchState(store, { selectedId: id });
    },

    clearSelection(): void {
      patchState(store, { selectedId: null });
    },

    create: rxMethod<Create<StoreName>Request>(
      pipe(
        tap(() => patchState(store, { isSaving: true, error: null })),
        switchMap(request =>
          service.create(request).pipe(
            tapResponse({
              next: created => patchState(store, {
                items: [...store.items(), created],
                isSaving: false,
              }),
              error: (err: Error) => patchState(store, { error: err.message, isSaving: false }),
            })
          )
        )
      )
    ),

    remove: rxMethod<string>(
      pipe(
        switchMap(id =>
          service.delete(id).pipe(
            tapResponse({
              next: () => patchState(store, {
                items: store.items().filter(i => i.id !== id),
              }),
              error: (err: Error) => patchState(store, { error: err.message }),
            })
          )
        )
      )
    ),
  }))
);
```

### Step 3: Generate store with entities (when --entity is set)
```typescript
import { withEntities, setAllEntities, addEntity, removeEntity } from '@ngrx/signals/entities';

export const <StoreName>Store = signalStore(
  withEntities<<StoreName>Dto>(),
  withMethods((store, service = inject(<StoreName>Service)) => ({
    loadAll: rxMethod<void>(
      pipe(
        switchMap(() => service.findAll().pipe(
          tapResponse({
            next: page => patchState(store, setAllEntities(page.content)),
            error: () => {}
          })
        ))
      )
    ),
    add: rxMethod<Create<StoreName>Request>(
      pipe(
        switchMap(req => service.create(req).pipe(
          tapResponse({
            next: entity => patchState(store, addEntity(entity)),
            error: () => {}
          })
        ))
      )
    ),
  }))
);
```

### Step 4: Wire into component (route scope)
For route-scoped stores, add to the shell component:
```typescript
@Component({
  // ...
  providers: [<StoreName>Store]  // scoped to this route subtree
})
```

### Step 5: Report
- File created, store scope, how to inject in components
- Signal selectors available on the store instance
