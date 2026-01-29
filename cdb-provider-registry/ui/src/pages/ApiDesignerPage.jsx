import React from 'react';

export default function ApiDesignerPage() {
  return (
    <div className="h-screen flex flex-col">
      <div className="flex-1 min-h-0 overflow-y-auto overflow-x-hidden">
        <React.Suspense fallback={
          <div className="h-full flex items-center justify-center">
            <div className="text-center">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 mx-auto mb-4" style={{ borderColor: 'var(--primary)' }}></div>
              <p className="muted">Loading API Designer...</p>
            </div>
          </div>
        }>
          <ApiDesignerLazy />
        </React.Suspense>
      </div>
    </div>
  );
}

const ApiDesignerLazy = React.lazy(() => import('../modules/api-designer/index.jsx'));