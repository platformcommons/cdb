import React from 'react';
import { Plus, Trash2 } from 'lucide-react';
import { useProject } from '../context';

export function BasicDetails() {
  const { project, updateProject } = useProject();

  const handleChange = (field, value) => {
    updateProject({ [field]: value });
  };

  const handleNestedChange = (objField, field, value) => {
    updateProject({ [objField]: { ...(project[objField] || {}), [field]: value } });
  };

  const handleServerChange = (index, field, value) => {
    const servers = Array.isArray(project.servers) ? [...project.servers] : [];
    servers[index] = { ...(servers[index] || {}), [field]: value };
    updateProject({ servers });
  };

  const addServer = () => {
    const servers = Array.isArray(project.servers) ? [...project.servers] : [];
    servers.push({ url: '', description: '' });
    updateProject({ servers });
  };

  const removeServer = (index) => {
    const servers = Array.isArray(project.servers) ? [...project.servers] : [];
    servers.splice(index, 1);
    updateProject({ servers });
  };

  return (
    <div className="flex flex-col h-full min-h-0">
      <div style={{ padding: '24px', borderBottom: '1px solid rgba(255,255,255,0.08)' }}>
        <h2 style={{ fontSize: '20px', fontWeight: 600 }}>Basic Details</h2>
        <p className="muted" style={{ marginTop: 6 }}>Define the top-level OpenAPI info and servers for your API.</p>
      </div>

      <div className="flex-1 min-h-0 overflow-y-auto overflow-x-hidden p-6 space-y-8">
        {/* General Info */}
        <section className="card p-6">
          <h3 className="font-semibold mb-4">Info</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm mb-1">Title</label>
              <input
                className="input"
                value={project.name || ''}
                onChange={(e) => handleChange('name', e.target.value)}
                placeholder="API Name (Service)"
              />
            </div>
            <div>
              <label className="block text-sm mb-1">Version</label>
              <input
                className="input"
                value={project.version || ''}
                onChange={(e) => handleChange('version', e.target.value)}
                placeholder="1.0"
              />
            </div>
            <div className="md:col-span-2">
              <label className="block text-sm mb-1">Description</label>
              <textarea
                className="w-full rounded-md"
                rows={8}
                style={{ minHeight: '160px', background: 'rgba(255,255,255,0.02)', color: 'var(--text)', border: '1px solid rgba(255,255,255,0.08)', padding: '12px' }}
                value={project.description || ''}
                onChange={(e) => handleChange('description', e.target.value)}
                placeholder="Description"
              />
            </div>
          </div>
        </section>

        {/* Contact */}
        <section className="card p-6">
          <h3 className="font-semibold mb-4">Contact</h3>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="block text-sm mb-1">Name</label>
              <input
                className="input"
                value={project.contact?.name || ''}
                onChange={(e) => handleNestedChange('contact', 'name', e.target.value)}
                placeholder="Organisation"
              />
            </div>
            <div>
              <label className="block text-sm mb-1">Email</label>
              <input
                className="input"
                type="email"
                value={project.contact?.email || ''}
                onChange={(e) => handleNestedChange('contact', 'email', e.target.value)}
                placeholder="contact@example.com"
              />
            </div>
            <div>
              <label className="block text-sm mb-1">URL</label>
              <input
                className="input"
                value={project.contact?.url || ''}
                onChange={(e) => handleNestedChange('contact', 'url', e.target.value)}
                placeholder="https://example.org"
              />
            </div>
          </div>
        </section>

        {/* Terms & License */}
        <section className="card p-6">
          <h3 className="font-semibold mb-4">Terms & License</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm mb-1">Terms of Service URL</label>
              <input
                className="input"
                value={project.termsOfService || ''}
                onChange={(e) => handleChange('termsOfService', e.target.value)}
                placeholder="https://example.com/terms"
              />
            </div>
            <div>
              <label className="block text-sm mb-1">License Name</label>
              <input
                className="input"
                value={project.license?.name || ''}
                onChange={(e) => handleNestedChange('license', 'name', e.target.value)}
                placeholder="Organisation name"
              />
            </div>
            <div>
              <label className="block text-sm mb-1">License URL</label>
              <input
                className="input"
                value={project.license?.url || ''}
                onChange={(e) => handleNestedChange('license', 'url', e.target.value)}
                placeholder="https://example.com/license"
              />
            </div>
          </div>
        </section>

        {/* Servers */}
        <section className="card p-6">
          <div className="flex items-center justify-between mb-4">
            <h3 className="font-semibold">Servers</h3>
            <button className="btn" onClick={addServer}>
              <Plus className="w-4 h-4" />
            </button>
          </div>
          <div className="space-y-3">
            {(project.servers || []).map((srv, idx) => (
              <div key={idx} className="p-4 rounded-lg border">
                <div className="grid grid-cols-1 md:grid-cols-12 gap-3 items-center">
                  <div className="md:col-span-4">
                    <label className="block text-sm mb-1">URL</label>
                    <input
                      className="input"
                      value={srv.url || ''}
                      onChange={(e) => handleServerChange(idx, 'url', e.target.value)}
                      placeholder="https://api.example.com"
                    />
                  </div>
                  <div className="md:col-span-7">
                    <label className="block text-sm mb-1">Description</label>
                    <input
                      className="input"
                      value={srv.description || ''}
                      onChange={(e) => handleServerChange(idx, 'description', e.target.value)}
                      placeholder="Production"
                    />
                  </div>
                  <div className="md:col-span-1 flex items-end justify-end">
                    <button className="btn text-red-600" onClick={() => removeServer(idx)}>
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              </div>
            ))}
            {(!project.servers || project.servers.length === 0) && (
              <div className="muted text-sm">No servers defined. Add one to specify environments.</div>
            )}
          </div>
        </section>
      </div>
    </div>
  );
}
