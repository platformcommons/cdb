import React, { useState, Suspense } from 'react';
import { ProjectProvider } from './context';
import { Sidebar } from './components/Sidebar';
import { APIDesigner } from './components/APIDesigner';
import { SchemaEditor } from './components/SchemaEditor';
import { SpecPreview } from './components/SpecPreview';
import { BasicDetails } from './components/BasicDetails';

function ApiDesignerModule() {
  const [currentView, setCurrentView] = useState('basic');

  return (
    <ProjectProvider>
      <div className="h-full min-h-0 flex">
        <Sidebar currentView={currentView} onViewChange={setCurrentView} />
        <div className="flex-1 flex flex-col min-h-0">
          <main className="flex-1 min-h-0 overflow-hidden">
            {currentView === 'basic' && <BasicDetails />}
            {currentView === 'designer' && <APIDesigner />}
            {currentView === 'schemas' && <SchemaEditor />}
            {currentView === 'preview' && <SpecPreview />}
          </main>
        </div>
      </div>
    </ProjectProvider>
  );
}

export default function ApiDesignerLazy() {
  return (
    <Suspense fallback={
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 mx-auto mb-4" style={{ borderColor: 'var(--primary)' }}></div>
          <p className="muted">Loading API Designer...</p>
        </div>
      </div>
    }>
      <ApiDesignerModule />
    </Suspense>
  );
}