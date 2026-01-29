import React from 'react';
import { Plus, Trash2 } from 'lucide-react';
import { useProject } from '../context';

export function EndpointList({ selectedId, onSelect }) {
  const { project, addEndpoint, removeEndpoint } = useProject();

  const getMethodColor = (method) => {
    const colors = {
      GET: 'bg-green-100 text-green-800',
      POST: 'bg-blue-100 text-blue-800',
      PUT: 'bg-orange-100 text-orange-800',
      DELETE: 'bg-red-100 text-red-800',
      PATCH: 'bg-purple-100 text-purple-800',
    };
    return colors[method] || 'bg-gray-100 text-gray-800';
  };

  const handleAddEndpoint = () => {
    const newEndpoint = {
      id: Date.now().toString(),
      path: '/new-endpoint',
      method: 'GET',
      summary: 'New Endpoint',
      description: '',
      parameters: [],
      requestBody: null,
      responses: {
        '200': {
          description: 'Successful response',
          content: {},
        },
      },
    };
    addEndpoint(newEndpoint);
    onSelect(newEndpoint.id);
  };

  return (
    <div className="w-80 card h-full min-h-0 flex flex-col">
      <div className="p-4" style={{ borderBottom: '1px solid rgba(255,255,255,0.08)' }}>
        <div className="flex items-center justify-between mb-4">
          <h3 className="font-semibold">Endpoints</h3>
          <button
            onClick={handleAddEndpoint}
            className="p-2 rounded-lg transition-colors"
            style={{ color: 'var(--primary)' }}
            onMouseEnter={(e) => { e.currentTarget.style.background = 'rgba(255,255,255,0.06)'; }}
            onMouseLeave={(e) => { e.currentTarget.style.background = ''; }}
          >
            <Plus className="w-4 h-4" />
          </button>
        </div>
      </div>

      <div className="flex-1 min-h-0 overflow-y-auto p-4 space-y-2">
        {project.endpoints.map((endpoint) => (
          <div
            key={endpoint.id}
            className={"group p-3 rounded-lg border cursor-pointer transition-all"}
            style={
              selectedId === endpoint.id
                ? { border: '1px solid var(--primary)', background: 'rgba(79,70,229,0.12)', boxShadow: '0 1px 2px rgba(0,0,0,0.2)' }
                : { border: '1px solid rgba(255,255,255,0.12)' }
            }
            onMouseEnter={(e) => {
              if (selectedId !== endpoint.id) {
                e.currentTarget.style.border = '1px solid rgba(255,255,255,0.2)';
                e.currentTarget.style.background = 'rgba(255,255,255,0.04)';
              }
            }}
            onMouseLeave={(e) => {
              if (selectedId !== endpoint.id) {
                e.currentTarget.style.border = '1px solid rgba(255,255,255,0.12)';
                e.currentTarget.style.background = '';
              }
            }}
            onClick={() => onSelect(endpoint.id)}
          >
            <div className="flex items-center justify-between mb-2">
              <span className={`px-2 py-1 text-xs font-medium rounded ${getMethodColor(endpoint.method)}`}>
                {endpoint.method}
              </span>
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  removeEndpoint(endpoint.id);
                }}
                className="opacity-0 group-hover:opacity-100 p-1 text-red-500 hover:bg-red-50 rounded transition-all"
              >
                <Trash2 className="w-3 h-3" />
              </button>
            </div>
            
            <div className="font-mono text-sm mb-1" style={{ color: 'var(--muted)' }}>
              {endpoint.path}
            </div>
            
            <div className="text-sm font-medium">
              {endpoint.summary}
            </div>
          </div>
        ))}
        
        {project.endpoints.length === 0 && (
          <div className="text-center muted py-8">
            <p className="mb-2">No endpoints yet</p>
            <button
              onClick={handleAddEndpoint}
              className="font-medium"
              style={{ color: 'var(--primary)' }}
            >
              Create your first endpoint
            </button>
          </div>
        )}
      </div>
    </div>
  );
}