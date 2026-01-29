import { Suspense, lazy } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import Layout from '@components/layout/Layout'
import HomePage from '@pages/HomePage'
import MarketplacePage from '@pages/MarketplacePage'
import NotFoundPage from '@pages/NotFoundPage'
import ErrorBoundary from '@components/common/ErrorBoundary'
import PlaceholderPage from '@pages/PlaceholderPage'
import HowToUsePage from '@pages/HowToUsePage'
import AuthCallbackPage from '@pages/AuthCallbackPage'
import ProviderRegistryPage from '@pages/ProviderRegistryPage'
import DomainDashboard from '@components/api/DomainDashboard'

const ApiDetailPage = lazy(() => import('@pages/ApiDetailPage'))

export default function App() {
  return (
    <Layout>
      <ErrorBoundary>
        <Suspense fallback={<div className="p-6">Loading...</div>}>
          <Routes>
            <Route path="/" element={<MarketplacePage />} />
            <Route path="/search" element={<MarketplacePage />} />
            <Route path="/auth/callback" element={<AuthCallbackPage />} />

            {/* API Registry subpages */}
            <Route path="/categories" element={<DomainDashboard />} />
            <Route path="/domains" element={<DomainDashboard />} />
            <Route path="/trending" element={<PlaceholderPage title="Trending" description="See what's popular across the ecosystem right now." />} />

              {/* Provider Data Registry */}
              <Route path="/providers" element={<ProviderRegistryPage />} />


              {/* Master Data Registry */}
            <Route path="/master-data" element={<PlaceholderPage title="Master Data Registry" description="Discover standardized master data sets and schemas." />} />

            {/* Developer Docs */}
            <Route path="/docs/how-to-use" element={<HowToUsePage />} />
            <Route path="/docs/libraries" element={<PlaceholderPage title="Libraries" description="Official SDKs and helper libraries for rapid development." />} />
            <Route path="/docs/forum" element={<PlaceholderPage title="Developer Forum" description="Join the community to ask questions and share knowledge." />} />

            {/* About */}
            <Route path="/about/about-cdb" element={<PlaceholderPage title="About CDB" description="Learn about the Commons Digital Backbone and its vision." />} />
            <Route path="/about/mission" element={<PlaceholderPage title="Mission" description="Our mission to accelerate interoperability and reuse." />} />
            <Route path="/about/partners" element={<PlaceholderPage title="Partners" description="Organizations and communities building with CDB." />} />
            <Route path="/about/contact" element={<PlaceholderPage title="Contact Us" description="Reach out for support, partnerships, or feedback." />} />

            {/* Policy */}
            <Route path="/policy/license" element={<PlaceholderPage title="License" description="Understand licensing for APIs, datasets, and code." />} />
            <Route path="/policy/data-governance" element={<PlaceholderPage title="Data Governance" description="Policies and processes governing data stewardship." />} />
            <Route path="/policy/privacy" element={<PlaceholderPage title="Privacy Policy" description="How we handle data privacy and user information." />} />

            {/* API Details */}
            <Route path="/api/:apiId" element={<ApiDetailPage defaultTab="docs" />} />
            <Route path="/api/:apiId/docs" element={<ApiDetailPage defaultTab="docs" />} />
            <Route path="/api/:apiId/try" element={<ApiDetailPage defaultTab="try" />} />

            <Route path="/404" element={<NotFoundPage />} />
            <Route path="*" element={<Navigate to="/404" replace />} />
          </Routes>
        </Suspense>
      </ErrorBoundary>
    </Layout>
  )
}
