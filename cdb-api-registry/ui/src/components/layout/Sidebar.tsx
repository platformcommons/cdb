import { NavLink } from 'react-router-dom'
import { useState } from 'react'

function Section({ title, children, defaultOpen = false }: { title: string; children: React.ReactNode; defaultOpen?: boolean }) {
  const [open, setOpen] = useState(defaultOpen)

  return (
    <div>
      <button
        type="button"
        onClick={() => setOpen(v => !v)}
        className="w-full px-3 pt-4 pb-2 text-xs font-semibold text-gray-600 uppercase tracking-wider flex items-center justify-between hover:text-gray-900 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand/40"
        aria-expanded={open}
      >
        <span>{title}</span>
        <svg
          className={`h-4 w-4 transition-transform ${open ? 'rotate-180' : ''}`}
          viewBox="0 0 20 20"
          fill="currentColor"
          aria-hidden="true"
        >
          <path fillRule="evenodd" d="M5.23 7.21a.75.75 0 011.06.02L10 11.17l3.71-3.94a.75.75 0 111.08 1.04l-4.25 4.5a.75.75 0 01-1.08 0l-4.25-4.5a.75.75 0 01.02-1.06z" clipRule="evenodd" />
        </svg>
      </button>
      <div className={`ml-3 pl-3 border-l border-gray-200 transition-all ${open ? 'mt-1 space-y-1' : 'h-0 overflow-hidden'}`}>
        {open && children}
      </div>
    </div>
  )
}

function Item({ to, children }: { to: string, children: string }) {
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        `block rounded-md px-2 py-2 text-sm transition-colors ${isActive ? 'bg-brand/10 text-brand' : 'text-gray-700 hover:bg-gray-100 hover:text-gray-900'}`
      }
    >
      {children}
    </NavLink>
  )
}

export default function Sidebar({ open = true }: { open?: boolean }) {
  return (
    <aside className={`${open ? 'hidden md:block w-64' : 'hidden'} shrink-0 border-r bg-white`} aria-hidden={!open}>
      <div className="sticky top-0 max-h-[calc(100vh-57px)] md:max-h-[calc(100vh-57px)] overflow-auto">
        <nav className="py-3">
          <Section title="API Registry" defaultOpen>
            <Item to="/">All</Item>
            <Item to="/categories">Domains</Item>
          </Section>

            <Section title="Provider Registry">
                <Item to="/providers">Providers</Item>
            </Section>

          <Section title="Master Data Registry">
            <Item to="/master-data">Overview</Item>
          </Section>

          <Section title="Developer Docs">
            <Item to="/docs/how-to-use">How to use</Item>
            <Item to="/docs/libraries">Libraries</Item>
            <Item to="/docs/forum">Forum</Item>
          </Section>

          <Section title="About Us">
            <Item to="/about/about-cdb">About CDB</Item>
            <Item to="/about/partners">Partners</Item>
            <Item to="/about/contact">Contact Us</Item>
          </Section>

          <Section title="Policy">
            <Item to="/policy/license">License</Item>
            <Item to="/policy/data-governance">Data Governance</Item>
            <Item to="/policy/privacy">Privacy Policy</Item>
          </Section>
        </nav>
      </div>
    </aside>
  )
}
