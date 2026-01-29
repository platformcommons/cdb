import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { generateProviderKey } from '../services/providerService';

function useProviderId() {
  const { search } = useLocation();
  const params = new URLSearchParams(search);
  const fromQuery = params.get('providerId');
  const stored = window.localStorage.getItem('providerId');
  const id = fromQuery || stored || '1';
  if (fromQuery) window.localStorage.setItem('providerId', fromQuery);
  return id;
}

export default function AddKey() {
  const navigate = useNavigate();
  const providerId = useProviderId();
  const [formData, setFormData] = useState({
    title: '',
    keyType: 'SIGNING',
    keyStatus: 'ACTIVE',
    environment: 'SANDBOX',
    clientId: 'default',
    expiresAt: '',
    scopes: [],
  });

  const [activeTab, setActiveTab] = useState('basic');
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [apiError, setApiError] = useState('');

  // Dialog state for showing generated keys
  const [showDialog, setShowDialog] = useState(false);
  const [generated, setGenerated] = useState(null); // response from API
  const [dialogTab, setDialogTab] = useState('private'); // 'private' | 'public'
  const [ackSaved, setAckSaved] = useState(false);

  const keyTypes = [
    { value: 'SIGNING', label: 'Asymmetric Signing Key' },
    { value: 'ENCRYPTION', label: 'Symmetric Encryption Key' }
  ];

  const availableScopes = [
    'read:apis',
    'write:apis',
    'read:providers',
    'write:providers',
    'admin:registry'
  ];

  const handleInputChange = (field, value) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    if (errors[field]) {
      setErrors(prev => ({ ...prev, [field]: null }));
    }
  };

  const handleScopeToggle = (scope) => {
    setFormData(prev => ({
      ...prev,
      scopes: prev.scopes.includes(scope)
        ? prev.scopes.filter(s => s !== scope)
        : [...prev.scopes, scope]
    }));
  };

  const validateForm = () => {
    const newErrors = {};
    if (!formData.title.trim()) newErrors.title = 'Title is required';
    if (!formData.keyType) newErrors.keyType = 'Type is required';
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setApiError('');
    if (!validateForm()) return;
    setSubmitting(true);
    try {
      const payload = {
        title: formData.title,
        keyType: formData.keyType,
        keyStatus: formData.keyStatus || 'ACTIVE',
        environment: formData.environment,
        clientId: formData.clientId || 'default',
        scopes: formData.scopes,
        expiresAt: formData.expiresAt ? new Date(formData.expiresAt).toISOString() : null,
      };
      const resp = await generateProviderKey(providerId, payload);
      setGenerated(resp);
      setDialogTab('private');
      setAckSaved(false);
      setShowDialog(true);
    } catch (err) {
      console.error(err);
      setApiError(err.message || 'Failed to generate key');
    } finally {
      setSubmitting(false);
    }
  };

  const handleCancel = () => {
    navigate('/keys' + (providerId ? `?providerId=${providerId}` : ''));
  };

  const copyToClipboard = async (text) => {
    try {
      await navigator.clipboard.writeText(text || '');
    } catch (e) {
      console.warn('Clipboard not available', e);
    }
  };

  const downloadPem = (filename, content) => {
    try {
      const blob = new Blob([content || ''], { type: 'application/x-pem-file' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = filename;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch (e) {
      console.warn('Download failed', e);
    }
  };

  return (
    <div className="container">
      <div className="form-header">
        <div>
          <h1>Add New Key</h1>
          <p className="muted">Generate an asymmetric key pair. The private key is shown only once.</p>
        </div>
        <div className="form-actions">
          <button type="button" className="btn" onClick={handleCancel}>Cancel</button>
          <button type="submit" form="key-form" className="btn primary" disabled={submitting}>{submitting ? 'Creating…' : 'Create Key'}</button>
        </div>
      </div>

      <div className="form-container">
        <div className="form-tabs">
          <button 
            className={`tab ${activeTab === 'basic' ? 'active' : ''}`}
            onClick={() => setActiveTab('basic')}
          >
            Basic
          </button>
          <button
            className={`tab ${activeTab === 'permissions' ? 'active' : ''}`}
            onClick={() => setActiveTab('permissions')}
          >
            Permissions
          </button>
          <button 
            className={`tab ${activeTab === 'advanced' ? 'active' : ''}`}
            onClick={() => setActiveTab('advanced')}
          >
            Advanced
          </button>
        </div>

        {apiError && (
          <div className="empty-state" style={{border:'1px solid #fecaca', background:'#fef2f2', color:'#991b1b'}}>
            {apiError}
          </div>
        )}

        <form id="key-form" onSubmit={handleSubmit} className="form-content">
          {activeTab === 'basic' && (
            <div className="form-section">
              <div className="form-group">
                <label htmlFor="title">Title *</label>
                <input
                  id="title"
                  type="text"
                  value={formData.title}
                  onChange={(e) => handleInputChange('title', e.target.value)}
                  placeholder="e.g., Production Signing Key"
                  className={errors.title ? 'error' : ''}
                />
                {errors.title && <span className="error-text">{errors.title}</span>}
              </div>

              <div className="form-group">
                <label htmlFor="keyType">Type *</label>
                <select
                  id="keyType"
                  value={formData.keyType}
                  onChange={(e) => handleInputChange('keyType', e.target.value)}
                  className={errors.keyType ? 'error' : ''}
                >
                  {keyTypes.map(type => (
                    <option key={type.value} value={type.value}>{type.label}</option>
                  ))}
                </select>
                {errors.keyType && <span className="error-text">{errors.keyType}</span>}
              </div>

              <div className="form-group">
                <label htmlFor="keyStatus">Status</label>
                <select
                  id="keyStatus"
                  value={formData.keyStatus}
                  onChange={(e) => handleInputChange('keyStatus', e.target.value)}
                >
                  <option value="ACTIVE">ACTIVE</option>
                  <option value="NOT_ACTIVE">NOT_ACTIVE</option>
                </select>
              </div>

              <div className="form-group">
                <label htmlFor="environment">Environment *</label>
                <select
                  id="environment"
                  value={formData.environment}
                  onChange={(e) => handleInputChange('environment', e.target.value)}
                >
                  <option value="SANDBOX">SANDBOX</option>
                  <option value="PRODUCTION">PRODUCTION</option>
                </select>
              </div>

              {formData.keyType === 'ENCRYPTION' && (
                <div className="form-group">
                  <label htmlFor="clientId">Client ID</label>
                  <input
                    id="clientId"
                    type="text"
                    value={formData.clientId}
                    onChange={(e) => handleInputChange('clientId', e.target.value.replace(/\s/g, ''))}
                    placeholder="default"
                    maxLength="100"
                  />
                  <small className="form-hint">Unique identifier for this key (no spaces, max 100 chars)</small>
                </div>
              )}

              <div className="form-group">
                <label htmlFor="expiresAt">Expires At</label>
                <input
                  id="expiresAt"
                  type="datetime-local"
                  value={formData.expiresAt}
                  onChange={(e) => handleInputChange('expiresAt', e.target.value)}
                />
                <small className="form-hint">Leave empty for no expiration</small>
              </div>
            </div>
          )}

          {activeTab === 'permissions' && (
            <div className="form-section">
              <div className="form-group">
                <label>Scopes *</label>
                <div className="scope-grid">
                  {availableScopes.map(scope => (
                    <label key={scope} className="scope-item">
                      <input
                        type="checkbox"
                        checked={formData.scopes.includes(scope)}
                        onChange={() => handleScopeToggle(scope)}
                      />
                      <span className="scope-name">{scope}</span>
                      <span className="scope-desc">
                        {scope.includes('read') ? 'Read access' :
                         scope.includes('write') ? 'Write access' :
                         scope.includes('admin') ? 'Administrative access' : 'Access'}
                      </span>
                    </label>
                  ))}
                </div>
                {errors.scopes && <span className="error-text">{errors.scopes}</span>}
              </div>
            </div>
          )}

          {activeTab === 'advanced' && (
            <div className="form-section">
              <div className="form-group">
                <label>Rate Limiting</label>
                <div className="rate-limit-controls">
                  <input
                    type="number"
                    placeholder="1000"
                    min="1"
                  />
                  <span>requests per</span>
                  <select>
                    <option value="minute">minute</option>
                    <option value="hour">hour</option>
                    <option value="day">day</option>
                  </select>
                </div>
              </div>

              <div className="form-group">
                <label>IP Restrictions</label>
                <textarea
                  placeholder="192.168.1.0/24&#10;10.0.0.1"
                  rows="3"
                />
                <small className="form-hint">One IP address or CIDR block per line</small>
              </div>

              <div className="form-group">
                <label className="checkbox-label">
                  <input type="checkbox" />
                  <span>Enable audit logging</span>
                </label>
              </div>

              <div className="form-group">
                <label className="checkbox-label">
                  <input type="checkbox" />
                  <span>Require HTTPS</span>
                </label>
              </div>
            </div>
          )}
        </form>
      </div>

      {showDialog && generated && (
        <div style={{position:'fixed', inset:0, background:'rgba(0,0,0,0.6)', display:'flex', alignItems:'center', justifyContent:'center', zIndex:9999, padding:'20px'}}>
          <div style={{background:'var(--card)', width:'min(600px, 100%)', maxHeight:'85vh', borderRadius:'12px', border:'1px solid rgba(255,255,255,0.1)', overflow:'hidden', display:'flex', flexDirection:'column'}}>
            <div style={{padding:'16px 20px', borderBottom:'1px solid rgba(255,255,255,0.08)', display:'flex', justifyContent:'space-between', alignItems:'center'}}>
              <div>
                <h2 style={{margin:0, color: 'var(--text)'}}>Key Generated</h2>
                <p className="muted" style={{margin:'4px 0 0 0'}}>Save your private key now. It will not be shown again.</p>
              </div>
              <button className="btn" onClick={() => setShowDialog(false)} title="Close">✕</button>
            </div>

            <div style={{padding:'20px', overflow:'auto', flex:1}}>
              <div className="form-tabs" style={{marginBottom:'16px'}}>
                <button
                  className={`tab ${dialogTab === 'private' ? 'active' : ''}`}
                  onClick={() => setDialogTab('private')}
                >
                  Private Key
                </button>
                <button
                  className={`tab ${dialogTab === 'public' ? 'active' : ''}`}
                  onClick={() => setDialogTab('public')}
                >
                  Public Key
                </button>
              </div>

              <div className="section-card">
                <div className="section-header">
                  <div style={{display:'flex', alignItems:'center', gap:'8px'}}>
                    <span style={{fontWeight:600, color: 'var(--text)'}}>{dialogTab === 'private' ? 'Private key (PEM)' : 'Public key (PEM)'}</span>
                    {dialogTab === 'private' && (
                      <span className="badge warning">Shown only once</span>
                    )}
                  </div>
                  <div className="toolbar">
                    <button className="btn" onClick={() => copyToClipboard(dialogTab==='private'? generated.privateKeyPem : generated.publicKeyPem)} title="Copy">
                      📋
                    </button>
                    <button className="btn" onClick={() => downloadPem(`${dialogTab==='private'?'private':'public'}-${generated.keyId}.pem`, dialogTab==='private'? generated.privateKeyPem : generated.publicKeyPem)} title="Download">
                      💾
                    </button>
                  </div>
                </div>
                <pre className="preview-pane mono" style={{maxHeight:'35vh'}}>
{dialogTab === 'private' ? (generated.privateKeyPem || '') : (generated.publicKeyPem || '')}
                </pre>
              </div>

              <div style={{marginTop:'16px', padding:'12px', background:'rgba(239,68,68,0.1)', border:'1px solid rgba(239,68,68,0.2)', color:'#f87171', borderRadius:'8px'}}>
                Important: This private key cannot be displayed later. Store it securely now. You will need it to authenticate and sign requests.
              </div>

              <div style={{marginTop:'16px'}}>
                <label className="checkbox-label">
                  <input type="checkbox" checked={ackSaved} onChange={(e) => setAckSaved(e.target.checked)} />
                  <span>I have securely saved the private key.</span>
                </label>
              </div>

              <div style={{display:'flex', justifyContent:'flex-end', gap:'8px', marginTop:'20px', paddingTop:'16px', borderTop:'1px solid rgba(255,255,255,0.08)'}}>
                <button className="btn" onClick={() => setShowDialog(false)}>Back</button>
                <button className="btn primary" disabled={!ackSaved} onClick={() => navigate('/keys' + (providerId ? `?providerId=${providerId}` : ''))}>Done</button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}