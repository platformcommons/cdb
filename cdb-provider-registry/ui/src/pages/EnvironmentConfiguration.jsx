import React, { useEffect, useState } from 'react';
import {
  getProviderEnvironments,
  createProviderEnvironment,
  updateProviderEnvironment,
  deleteProviderEnvironment,
} from '../services/providerEnvironmentService';

const ENV_TYPES = [
  { value: 'PRODUCTION', label: 'Production' },
  { value: 'SANDBOX', label: 'Sandbox' },
];

function EnvironmentForm({ initial, onSave, onCancel, disabledTypes }) {
  const [form, setForm] = useState(
    initial || {
      providerId: '',
      environmentType: 'PRODUCTION',
      baseUrl: '',
      uptimeStatus: '',
      rateLimit: '',
      remarks: '',
    }
  );

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((f) => ({ ...f, [name]: value }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    onSave(form);
  };

  return (
    <div className="form-container" style={{ marginTop: 8 }}>
      <div className="form-content">
        <form onSubmit={handleSubmit} className="form-section">
          <div className="grid-2">
            <div className="form-group">
              <label>Environment Type</label>
              <select
                name="environmentType"
                value={form.environmentType}
                onChange={handleChange}
                disabled={!!initial}
              >
                {ENV_TYPES.map((t) => (
                  <option
                    key={t.value}
                    value={t.value}
                    disabled={disabledTypes.includes(t.value)}
                  >
                    {t.label}
                  </option>
                ))}
              </select>
              <div className="form-hint">You can only have one of each environment type</div>
            </div>
            <div className="form-group">
              <label>Base URL *</label>
              <input
                name="baseUrl"
                value={form.baseUrl}
                onChange={handleChange}
                required
                placeholder="https://api.example.com"
              />
            </div>
          </div>

          <div className="grid-2">
            <div className="form-group">
              <label>Uptime Status *</label>
              <input
                name="uptimeStatus"
                value={form.uptimeStatus}
                onChange={handleChange}
                required
                placeholder="99.99%"
              />
            </div>
            <div className="form-group">
              <label>Rate Limit *</label>
              <input
                name="rateLimit"
                type="number"
                value={form.rateLimit}
                onChange={handleChange}
                required
                placeholder="1000"
              />
            </div>
          </div>

          <div className="form-group">
            <label>Remarks</label>
            <input
              name="remarks"
              value={form.remarks}
              onChange={handleChange}
              placeholder="Any notes..."
            />
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
            <button type="submit" className="btn primary">Save</button>
            <button type="button" onClick={onCancel} className="btn">Cancel</button>
          </div>
        </form>
      </div>
    </div>
  );
}

function EnvironmentCard({ env, onEdit, onDelete }) {
  return (
    <div className="card" style={{ marginBottom: 16 }}>
      <div className="section-header">
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <span className="badge" style={{ textTransform: 'capitalize' }}>{env.environmentType?.toLowerCase()}</span>
          <strong>{env.baseUrl}</strong>
        </div>
        <div className="toolbar">
          <button onClick={() => onEdit(env)} className="btn">Edit</button>
          <button onClick={() => onDelete(env.id)} className="btn">Delete</button>
        </div>
      </div>
      <div className="form-content" style={{ paddingTop: 12 }}>
        <div className="grid-2">
          <div className="form-group">
            <label>Uptime Status</label>
            <div>{env.uptimeStatus || '-'}</div>
          </div>
          <div className="form-group">
            <label>Rate Limit</label>
            <div>{env.rateLimit || '-'}</div>
          </div>
        </div>
        {env.remarks && (
          <div className="form-group" style={{ marginTop: 6 }}>
            <label>Remarks</label>
            <div>{env.remarks}</div>
          </div>
        )}
      </div>
    </div>
  );
}

export default function EnvironmentConfiguration({ providerId }) {
  const [environments, setEnvironments] = useState([]);
  const [editing, setEditing] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [loading, setLoading] = useState(false);

  const fetchEnvs = async () => {
    setLoading(true);
    try {
      const envs = await getProviderEnvironments(providerId);
      setEnvironments(envs);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (providerId) fetchEnvs();
  }, [providerId]);

  const handleAdd = () => {
    setEditing(null);
    setShowForm(true);
  };

  const handleEdit = (env) => {
    setEditing(env);
    setShowForm(true);
  };

  const handleDelete = async (id) => {
    await deleteProviderEnvironment(id);
    fetchEnvs();
  };

  const handleSave = async (data) => {
    if (editing) {
      await updateProviderEnvironment(editing.id, data);
    } else {
      await createProviderEnvironment({ ...data, providerId });
    }
    setShowForm(false);
    fetchEnvs();
  };

  // Determine which environment types are already configured
  const configuredTypes = environments.map(e => e.environmentType);
  const canAdd = configuredTypes.length < 2;
  const disabledTypes = configuredTypes;

  return (
    <div className="container">
      <div className="form-header">
        <div>
          <h1>Environment Configuration</h1>
          <p>Manage provider environments (Production, Sandbox)</p>
        </div>
        <div className="form-actions">
          {canAdd && (
            <button onClick={handleAdd} className="btn primary">Add Environment</button>
          )}
        </div>
      </div>

      {showForm && (
        <div className="modal-overlay">
          <div className="modal-panel" style={{ minWidth: 500 }}>
            <div className="modal-header">
              <h2 style={{ margin: 0 }}>{editing ? 'Edit Environment' : 'Add Environment'}</h2>
              <div className="form-actions">
                <button onClick={() => setShowForm(false)} className="btn">Close</button>
              </div>
            </div>
            <EnvironmentForm
              initial={editing}
              onSave={handleSave}
              onCancel={() => setShowForm(false)}
              disabledTypes={editing ? [] : disabledTypes}
            />
          </div>
        </div>
      )}

      {loading ? (
        <div className="card" style={{ padding: 16 }}>Loading...</div>
      ) : (
        <div>
          {environments.length === 0 && (
            <div className="empty-state">
              <div className="empty-graphic">🌐</div>
              <div>No environments configured yet.</div>
            </div>
          )}
          {environments.map((env) => (
            <EnvironmentCard
              key={env.id}
              env={env}
              onEdit={handleEdit}
              onDelete={handleDelete}
            />
          ))}
        </div>
      )}
    </div>
  );
}
