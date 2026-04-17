# /ng-optimize — Angular Performance Audit and Fix

Audit the CDB Angular UI for performance issues and apply fixes with measurable impact.

## Usage
```
/ng-optimize [--target <componentPath>] [--focus bundle|rendering|network|all]
```

## Arguments: $ARGUMENTS

## Instructions

Parse `$ARGUMENTS` to extract:
- `--target` — specific component or feature path to audit (default: whole `ui/src/app/`)
- `--focus` — area to focus on: `bundle` (chunk sizes), `rendering` (CD issues), `network` (HTTP), `all`

### Step 1: Audit Phase

Systematically check for these anti-patterns:

#### Rendering Performance
- [ ] Components missing `ChangeDetectionStrategy.OnPush`
- [ ] `| async` pipes replaced with manual subscriptions that don't unsubscribe
- [ ] `*ngFor` without `trackBy` on lists > 10 items
- [ ] `@HostListener` on scroll/resize without debounce
- [ ] Heavy computation in templates (method calls, complex expressions — use `computed()` or pipes)
- [ ] Signal-based state accessed without `computed()` causing extra re-evaluations

#### Bundle Size
- [ ] Eagerly loaded components that could be lazy-loaded
- [ ] Barrel imports (`import * from`) pulling in entire libraries
- [ ] Large icon libraries imported as full module instead of individual icons
- [ ] Third-party libraries not tree-shakeable
- [ ] Missing `defer {}` blocks for below-the-fold content

#### Network
- [ ] Missing HTTP caching headers usage on read-heavy services
- [ ] Multiple API calls that could be combined with `forkJoin`/`combineLatest`
- [ ] No debounce on search/filter inputs
- [ ] Images without lazy loading (`loading="lazy"`)
- [ ] Missing `shareReplay(1)` on observables subscribed multiple times

### Step 2: Apply Fixes

For each issue found, apply the fix inline:

**Missing OnPush:**
```typescript
// Before
@Component({ changeDetection: ChangeDetectionStrategy.Default })

// After
@Component({ changeDetection: ChangeDetectionStrategy.OnPush })
```

**Missing trackBy:**
```html
<!-- Before -->
<div *ngFor="let item of items">

<!-- After -->
<div *ngFor="let item of items; trackBy: trackById">
```
```typescript
trackById = (_: number, item: { id: string }) => item.id;
```

**Template method call → computed():**
```typescript
// Before (re-evaluates every CD cycle)
get filteredItems() { return this.items.filter(i => i.active); }

// After (only re-evaluates when items signal changes)
readonly filteredItems = computed(() => this.items().filter(i => i.active));
```

**Eager → Lazy route:**
```typescript
// Before
import { FeatureComponent } from './feature.component';
{ path: 'feature', component: FeatureComponent }

// After
{ path: 'feature', loadComponent: () => import('./feature.component').then(m => m.FeatureComponent) }
```

**Search debounce:**
```typescript
readonly searchControl = new FormControl('');
readonly filtered = toSignal(
  this.searchControl.valueChanges.pipe(
    debounceTime(300),
    distinctUntilChanged(),
    switchMap(term => this.service.search(term ?? ''))
  ),
  { initialValue: [] }
);
```

**Defer block for below-fold content:**
```html
@defer (on viewport) {
  <cdb-heavy-chart [data]="chartData()" />
} @placeholder {
  <div class="chart-skeleton"></div>
}
```

### Step 3: Report

Output a table:
| Issue | Location | Fix Applied | Impact |
|---|---|---|---|
| Missing OnPush | `feature-list.component.ts:12` | Applied | High |
| No trackBy | `feature-list.component.html:45` | Applied | Medium |

Followed by: bundle size recommendations (lazy load candidates) and network optimization suggestions.
