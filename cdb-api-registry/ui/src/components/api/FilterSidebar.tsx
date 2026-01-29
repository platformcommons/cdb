import { ApiStatus } from '@types/api'
import { fetchAvailableTags, fetchAvailableDomains, fetchAvailableOwners } from '@services/apiService'
import MultiSelectDropdown from '@components/common/MultiSelectDropdown'

export interface FilterState {
  query?: string
  tags: string[]
  domains: string[]
  owners: string[]
}

export default function FilterSidebar({
  filters,
  onChange
}: {
  filters: FilterState
  onChange: (f: FilterState) => void
}) {

  return (
    <aside className="space-y-6">

      <MultiSelectDropdown
        fetchOptions={fetchAvailableOwners}
        selected={filters.owners}
        onChange={(owners) => onChange({ ...filters, owners })}
        placeholder="Select owners"
        label="Owners"
      />

      <MultiSelectDropdown
        fetchOptions={fetchAvailableDomains}
        selected={filters.domains}
        onChange={(domains) => onChange({ ...filters, domains })}
        placeholder="Select domains"
        label="Domains"
      />

      <MultiSelectDropdown
        fetchOptions={fetchAvailableTags}
        selected={filters.tags}
        onChange={(tags) => onChange({ ...filters, tags })}
        placeholder="Select tags"
        label="Tags"
      />
    </aside>
  )
}
