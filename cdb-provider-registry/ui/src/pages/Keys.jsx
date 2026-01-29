import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { listProviderKeys, deactivateProviderKey } from '../services/providerService';

function useProviderId() {
  const { search } = useLocation();
  const params = new URLSearchParams(search);
  const fromQuery = params.get('providerId');
  const stored = window.localStorage.getItem('providerId');
  const id = fromQuery || stored || '1';
  useEffect(() => {
    if (fromQuery) window.localStorage.setItem('providerId', fromQuery);
  }, [fromQuery]);
  return id;
}

export default function Keys() {
  const navigate = useNavigate();
  const providerId = useProviderId();
  const [keys, setKeys] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [showPubDialog, setShowPubDialog] = useState(false);
  const [showDeactivateDialog, setShowDeactivateDialog] = useState(false);
  const [selectedKey, setSelectedKey] = useState(null);
  const [confirmText, setConfirmText] = useState('');
  const [deactivating, setDeactivating] = useState(false);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError('');
      try {
        const data = await listProviderKeys(providerId);
        setKeys(data || []);
      } catch (e) {
        console.error(e);
        setError('Failed to load keys');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [providerId]);

  const handleAddKey = () => {
    navigate('/keys/new' + (providerId ? `?providerId=${providerId}` : ''));
  };

  const statusClass = (status) => {
    switch (status) {
      case 'ACTIVE': return 'badge success';
      case 'NOT_ACTIVE': return 'badge';
      default: return 'badge';
    }
  };

  const filteredKeys = keys.filter(key =>
    (key.title || '').toLowerCase().includes(search.toLowerCase()) ||
    (key.keyType || '').toLowerCase().includes(search.toLowerCase()) ||
    (key.environment || '').toLowerCase().includes(search.toLowerCase()) ||
    (key.clientId || '').toLowerCase().includes(search.toLowerCase())
  );

  const copyToClipboard = async (text) => {
    try {
      await navigator.clipboard.writeText(text || '');
    } catch (e) {
      console.warn('Clipboard not available', e);
    }
  };

  const handleDeactivate = async () => {
    if (confirmText !== 'Permanently Delete' || !selectedKey) return;
    
    setDeactivating(true);
    try {
      await deactivateProviderKey(providerId, selectedKey.keyId);
      setShowDeactivateDialog(false);
      setConfirmText('');
      setSelectedKey(null);
      // Reload keys
      const data = await listProviderKeys(providerId);
      setKeys(data || []);
    } catch (e) {
      console.error(e);
      setError('Failed to deactivate key');
    } finally {
      setDeactivating(false);
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
      <div className="api-header">
        <div>
          <h1 style={{ margin: 0 }}>Provider Keys</h1>
          <p className="muted">Manage public keys for your provider. Private keys are shown only once upon creation.</p>
        </div>
        <div className="api-actions">
          <button className="btn primary" onClick={handleAddKey}>+ Add Key</button>
        </div>
      </div>

      <div className="api-toolbar">
        <div className="api-search">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="11" cy="11" r="8"/>
            <line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
          <input 
            value={search} 
            onChange={(e) => setSearch(e.target.value)} 
            placeholder="Search keys by title, type, or environment..." 
          />
        </div>
      </div>

      {error && (
        <div className="empty-state" style={{border:'1px solid #fecaca', background:'#fef2f2', color:'#991b1b'}}>
          {error}
        </div>
      )}

      {filteredKeys.length === 0 && !loading && !error && (
        <div className="empty-state">
          <div className="empty-graphic">🔑</div>
          <h3>No keys found</h3>
          <p className="muted">Create your first key to get started.</p>
          <button className="btn primary" onClick={handleAddKey}>Add Key</button>
        </div>
      )}

      {loading && (
        <div className="empty-state">
          <div className="empty-graphic">⏳</div>
          <h3>Loading keys…</h3>
        </div>
      )}

      {filteredKeys.length > 0 && !loading && (
        <div className="keys-table">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Key ID</th>
                <th>Type</th>
                <th>Environment</th>
                <th>Client ID</th>
                <th>Status</th>
                <th>Issued</th>
                <th>Expires</th>
                <th>Private Checksum</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredKeys.map(key => (
                <tr key={key.id}>
                  <td>
                    <div className="key-name">
                      <div className="key-icon">🔑</div>
                      <span>{key.title || key.keyId}</span>
                    </div>
                  </td>
                  <td style={{fontFamily:'monospace'}}>{key.keyId}</td>
                  <td>{key.keyType}</td>
                  <td>
                    <span className={`badge ${key.environment === 'PRODUCTION' ? 'warning' : 'info'}`}>
                      {key.environment}
                    </span>
                  </td>
                  <td style={{fontFamily:'monospace'}}>{key.clientId || 'default'}</td>
                  <td><span className={statusClass(key.keyStatus)}>{key.keyStatus}</span></td>
                  <td>{key.issuedAt ? new Date(key.issuedAt).toLocaleString() : '—'}</td>
                  <td>{key.expiresAt ? new Date(key.expiresAt).toLocaleString() : '—'}</td>
                  <td>
                    {key.privateKeyChecksum ? (
                      <div style={{display:'flex', alignItems:'center', gap:8}}>
                        <code title={key.privateKeyChecksum} style={{fontFamily:'monospace'}}>{key.privateKeyChecksum.slice(0,12)}…</code>
                        <button className="btn" onClick={() => copyToClipboard(key.privateKeyChecksum)} title="Copy checksum">
                          📋
                        </button>
                      </div>
                    ) : '—'}
                  </td>
                  <td>
                    <div className="key-actions" style={{display:'flex', gap:8}}>
                      <button className="btn" onClick={() => { setSelectedKey(key); setShowPubDialog(true); }} title="View Public Key">
                        🔍
                      </button>
                      {key.keyStatus === 'ACTIVE' && (
                        <button 
                          className="btn" 
                          onClick={() => { setSelectedKey(key); setShowDeactivateDialog(true); }} 
                          title="Deactivate Key"
                          style={{color: '#dc2626'}}
                        >
                          🗑️
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {showPubDialog && selectedKey && (
        <div style={{position:'fixed', inset:0, background:'rgba(0,0,0,0.6)', display:'flex', alignItems:'center', justifyContent:'center', zIndex:9999, padding:'20px'}}>
          <div style={{background:'var(--card)', width:'min(600px, 100%)', maxHeight:'85vh', borderRadius:'12px', border:'1px solid rgba(255,255,255,0.1)', overflow:'hidden', display:'flex', flexDirection:'column'}}>
            <div style={{padding:'16px 20px', borderBottom:'1px solid rgba(255,255,255,0.08)', display:'flex', justifyContent:'space-between', alignItems:'center'}}>
              <div>
                <h2 style={{margin:0, color: 'var(--text)'}}>Public Key</h2>
                <p className="muted" style={{margin:'4px 0 0 0'}}>Key: {selectedKey.title || selectedKey.keyId}</p>
              </div>
              <button className="btn" onClick={() => setShowPubDialog(false)} title="Close">✕</button>
            </div>

            <div style={{padding:'20px', overflow:'auto', flex:1}}>
              <div className="section-card">
                <div className="section-header">
                  <span style={{fontWeight:600, color: 'var(--text)'}}>Public key (PEM)</span>
                  <div className="toolbar">
                    <button className="btn" onClick={() => copyToClipboard(selectedKey.publicKeyPem)} title="Copy">
                      📋
                    </button>
                    <button className="btn" onClick={() => downloadPem(`public-${selectedKey.keyId}.pem`, selectedKey.publicKeyPem)} title="Download">
                      💾
                    </button>
                  </div>
                </div>
                <pre className="preview-pane mono" style={{maxHeight:'35vh'}}>
{selectedKey.publicKeyPem || ''}
                </pre>
              </div>

              <div style={{marginTop:'16px', padding:'12px', background:'rgba(255,255,255,0.04)', border:'1px solid rgba(255,255,255,0.08)', borderRadius:'8px'}}>
                <div style={{display:'flex', justifyContent:'space-between', alignItems:'center', gap:'8px', flexWrap:'wrap'}}>
                  <div>
                    <div style={{fontSize:12, color:'var(--muted)'}}>Private Key Checksum</div>
                    <code style={{fontFamily:'monospace', color:'var(--text)'}}>{selectedKey.privateKeyChecksum || '—'}</code>
                  </div>
                  {selectedKey.privateKeyChecksum && (
                    <button className="btn" onClick={() => copyToClipboard(selectedKey.privateKeyChecksum)} title="Copy checksum">
                      📋
                    </button>
                  )}
                </div>
              </div>

              <div style={{display:'flex', justifyContent:'flex-end', gap:'8px', marginTop:'20px', paddingTop:'16px', borderTop:'1px solid rgba(255,255,255,0.08)'}}>
                <button className="btn" onClick={() => setShowPubDialog(false)}>Close</button>
              </div>
            </div>
          </div>
        </div>
      )}

      {showDeactivateDialog && selectedKey && (
        <div style={{position:'fixed', inset:0, background:'rgba(0,0,0,0.6)', display:'flex', alignItems:'center', justifyContent:'center', zIndex:9999, padding:'20px'}}>
          <div style={{background:'var(--card)', width:'min(500px, 100%)', borderRadius:'12px', border:'1px solid rgba(255,255,255,0.1)', overflow:'hidden'}}>
            <div style={{padding:'16px 20px', borderBottom:'1px solid rgba(255,255,255,0.08)', display:'flex', justifyContent:'space-between', alignItems:'center'}}>
              <div>
                <h2 style={{margin:0, color: '#dc2626'}}>⚠️ Deactivate API Key</h2>
                <p className="muted" style={{margin:'4px 0 0 0'}}>Key: {selectedKey.title || selectedKey.keyId}</p>
              </div>
              <button className="btn" onClick={() => { setShowDeactivateDialog(false); setConfirmText(''); }} title="Close">✕</button>
            </div>

            <div style={{padding:'20px'}}>
              <div style={{marginBottom:'16px', padding:'12px', background:'rgba(220, 38, 38, 0.1)', border:'1px solid rgba(220, 38, 38, 0.2)', borderRadius:'8px'}}>
                <p style={{margin:0, color:'#dc2626', fontWeight:600}}>Warning: This action is not reversible!</p>
                <p style={{margin:'8px 0 0 0', color:'var(--text)', fontSize:'14px'}}>Once deactivated, this API key will no longer work and cannot be reactivated. Any applications using this key will lose access immediately.</p>
              </div>

              <div style={{marginBottom:'20px'}}>
                <label style={{display:'block', marginBottom:'8px', fontWeight:600, color:'var(--text)'}}>Type "Permanently Delete" to confirm:</label>
                <input 
                  type="text" 
                  value={confirmText}
                  onChange={(e) => setConfirmText(e.target.value)}
                  placeholder="Permanently Delete"
                  style={{
                    width:'100%', 
                    padding:'8px 12px', 
                    border:'1px solid rgba(255,255,255,0.2)', 
                    borderRadius:'6px', 
                    background:'var(--input-bg)', 
                    color:'var(--text)'
                  }}
                />
              </div>

              <div style={{display:'flex', justifyContent:'flex-end', gap:'8px'}}>
                <button 
                  className="btn" 
                  onClick={() => { setShowDeactivateDialog(false); setConfirmText(''); }}
                  disabled={deactivating}
                >
                  Cancel
                </button>
                <button 
                  className="btn" 
                  onClick={handleDeactivate}
                  disabled={confirmText !== 'Permanently Delete' || deactivating}
                  style={{
                    background: confirmText === 'Permanently Delete' && !deactivating ? '#dc2626' : 'rgba(220, 38, 38, 0.3)',
                    color: 'white',
                    cursor: confirmText === 'Permanently Delete' && !deactivating ? 'pointer' : 'not-allowed'
                  }}
                >
                  {deactivating ? 'Deactivating...' : 'Deactivate Key'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}