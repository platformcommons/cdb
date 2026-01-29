import React from 'react';
import { Code2, Database, FileText } from 'lucide-react';

export function Sidebar({ currentView, onViewChange }) {
  const menuItems = [
    { id: 'basic', label: 'Basic Details', icon: FileText },
    { id: 'designer', label: 'API Designer', icon: Code2 },
    { id: 'schemas', label: 'Schemas', icon: Database },
    { id: 'preview', label: 'Spec Preview', icon: FileText },
  ];

  return (
    <div className="w-64 card flex flex-col">
      <div className="p-6" style={{ borderBottom: '1px solid rgba(255,255,255,0.08)' }} >
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg flex items-center justify-center" style={{ background: 'var(--primary)' }}>
            <Code2 className="w-5 h-5 text-white" />
          </div>
            <span className="text-lg font-semibold">API Registry</span>
        </div>
      </div>

      <nav className="flex-1 p-4 space-y-2">
        {menuItems.map((item) => {
          const Icon = item.icon;
          const isActive = currentView === item.id;
          
          return (
            <button
              key={item.id}
              onClick={() => onViewChange(item.id)}
              className={`w-full flex items-center gap-3 px-3 py-2 rounded-lg text-left transition-colors ${
                isActive 
                  ? 'bg-blue-50 text-blue-700 border border-blue-200' 
                  : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
              }`}
            >
              <Icon className="w-5 h-5" />
              <span className="font-medium">{item.label}</span>
            </button>
          );
        })}
      </nav>
    </div>
  );
}