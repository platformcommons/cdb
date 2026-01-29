import React, {useEffect, useMemo, useState} from 'react';
import {useNavigate, useParams} from 'react-router-dom';
import {apiRegistryService} from '../services/apiRegistryService.js';

export default function ApiView() {
    const navigate = useNavigate();
    const {id} = useParams();
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [apiDetail, setApiDetail] = useState(null);

    const isValidId = (id) => {
        return !isNaN(id) && Number.isInteger(Number(id));
    };

    useEffect(() => {
        let mounted = true;
        const load = async () => {
            try {
                setLoading(true);
                let detail = {};
                if (isValidId(id)) {
                    detail = await apiRegistryService.getApiDetail(id);
                }
                if (mounted) {
                    setApiDetail(detail);
                }
            } catch (e) {
                console.error('Failed to load API detail', e);
                if (mounted) {
                    setError(e?.response?.data?.message || 'Failed to load API');
                }
            } finally {
                if (mounted) setLoading(false);
            }
        };
        load();
        return () => {
            mounted = false
        };
    }, [id]);

    const api = apiDetail?.api || apiDetail;
    const auditHistory = apiDetail?.auditHistory || [];
    const availableVersions = apiDetail?.availableVersions || [];

    const statusClass = (status) => {
        switch (status) {
            case 'PUBLISHED':
                return 'badge success';
            case 'DEPRECATED':
                return 'badge warning';
            case 'RETIRED':
                return 'badge danger';
            default:
                return 'badge';
        }
    };

    return (
        <div style={{minHeight: '100vh', background: 'var(--bg)', color: 'var(--text)'}}>
            <div style={{maxWidth: '1200px', margin: '0 auto', padding: '24px'}}>
                <div className="api-header">
                    <div>
                        <h1 style={{margin: 0, color: 'var(--text)'}}>
                            {loading ? 'Loading…' : api?.name || 'API Details'}
                        </h1>
                        <p style={{color: 'var(--muted)', margin: '8px 0 0'}}>
                            {loading ? 'Fetching API information...' : 'View API metadata, specification, and audit history'}
                        </p>
                    </div>
                    <div className="api-actions">
                        <button
                            onClick={() => navigate(`/apis/${id}/edit`)}
                            className="btn primary"
                            disabled={loading}
                        >
                            Edit API
                        </button>
                        <button onClick={() => navigate('/apis')} className="btn">Back to APIs</button>
                    </div>
                </div>

                {error && (
                    <div className="alert error" style={{marginBottom: 16}}>{error}</div>
                )}

                {loading && (
                    <div style={{textAlign: 'center', padding: '48px', color: 'var(--muted)'}}>
                        <div style={{
                            width: '32px',
                            height: '32px',
                            border: '2px solid var(--muted)',
                            borderTop: '2px solid var(--primary)',
                            borderRadius: '50%',
                            animation: 'spin 1s linear infinite',
                            margin: '0 auto 16px'
                        }}></div>
                        Loading API details...
                    </div>
                )}

                {!loading && api && (
                    <>
                        <div className="grid-2" style={{gap: 16, marginTop: 24}}>
                            <div className="card" style={{padding: '20px'}}>
                                <h3 style={{margin: '0 0 16px', color: 'var(--text)'}}>Basic Information</h3>
                                <div style={{display: 'grid', gap: '12px'}}>
                                    <div>
                                        <strong style={{color: 'var(--text)'}}>Name:</strong>
                                        <span style={{marginLeft: '8px', color: 'var(--muted)'}}>{api.name}</span>
                                    </div>
                                    <div>
                                        <strong style={{color: 'var(--text)'}}>Owner:</strong>
                                        <span style={{marginLeft: '8px', color: 'var(--muted)'}}>{api.owner}</span>
                                    </div>
                                    <div>
                                        <strong style={{color: 'var(--text)'}}>Base Path:</strong>
                                        <span style={{
                                            marginLeft: '8px',
                                            color: 'var(--muted)',
                                            fontFamily: 'monospace'
                                        }}>{api.basePath || '/'}</span>
                                    </div>
                                    <div>
                                        <strong style={{color: 'var(--text)'}}>Version:</strong>
                                        <span style={{marginLeft: '8px', color: 'var(--muted)'}}>{api.version}</span>
                                    </div>
                                    <div>
                                        <strong style={{color: 'var(--text)'}}>Status:</strong>
                                        <span className={statusClass(api.status)}
                                              style={{marginLeft: '8px'}}>{api.status}</span>
                                    </div>
                                    <div>
                                        <strong style={{color: 'var(--text)'}}>Tags:</strong>
                                        <span style={{marginLeft: '8px', color: 'var(--muted)'}}>
                      {api.tags?.length ? api.tags.join(', ') : 'None'}
                    </span>
                                    </div>
                                </div>
                            </div>

                            <div className="card" style={{padding: '20px'}}>
                                <h3 style={{margin: '0 0 16px', color: 'var(--text)'}}>Metadata</h3>
                                <div style={{display: 'grid', gap: '12px'}}>
                                    <div>
                                        <strong style={{color: 'var(--text)'}}>Created:</strong>
                                        <span style={{marginLeft: '8px', color: 'var(--muted)'}}>
                      {api.createdAt ? new Date(api.createdAt).toLocaleString() : '—'}
                    </span>
                                    </div>
                                    <div>
                                        <strong style={{color: 'var(--text)'}}>Updated:</strong>
                                        <span style={{marginLeft: '8px', color: 'var(--muted)'}}>
                      {api.updatedAt ? new Date(api.updatedAt).toLocaleString() : '—'}
                    </span>
                                    </div>
                                    <div>
                                        <strong style={{color: 'var(--text)'}}>Created By:</strong>
                                        <span style={{
                                            marginLeft: '8px',
                                            color: 'var(--muted)'
                                        }}>{api.createdBy || '—'}</span>
                                    </div>
                                    <div>
                                        <strong style={{color: 'var(--text)'}}>Updated By:</strong>
                                        <span style={{
                                            marginLeft: '8px',
                                            color: 'var(--muted)'
                                        }}>{api.updatedBy || '—'}</span>
                                    </div>
                                    <div>
                                        <strong style={{color: 'var(--text)'}}>Available Versions:</strong>
                                        <span style={{marginLeft: '8px', color: 'var(--muted)'}}>
                      {availableVersions.length ? availableVersions.join(', ') : 'Current only'}
                    </span>
                                    </div>
                                </div>
                            </div>
                        </div>

                        {api.description && (
                            <div className="section-card" style={{marginTop: 24}}>
                                <div className="section-header">
                                    <div>Description</div>
                                </div>
                                <div style={{padding: '16px'}}>
                                    <p style={{margin: 0, color: 'var(--text)'}}>{api.description}</p>
                                </div>
                            </div>
                        )}

                        {api.detailedDescription && (
                            <div className="section-card" style={{marginTop: 24}}>
                                <div className="section-header">
                                    <div>Detailed Description</div>
                                </div>
                                <div style={{padding: '16px'}}>
                                    <DescriptionPreview text={api.detailedDescription}/>
                                </div>
                            </div>
                        )}

                        {api.openApiSpec && (
                            <div className="section-card" style={{marginTop: 24}}>
                                <div className="section-header">
                                    <div>OpenAPI Specification</div>
                                    <div>
                                        <span className="badge success">YAML</span>
                                    </div>
                                </div>
                                <div style={{padding: '16px'}}>
                  <pre className="preview-pane mono" style={{margin: 0, maxHeight: '400px', overflow: 'auto'}}>
                    {api.openApiSpec}
                  </pre>
                                </div>
                            </div>
                        )}

                        {auditHistory.length > 0 && (
                            <div className="section-card" style={{marginTop: 24}}>
                                <div className="section-header">
                                    <div>Audit History</div>
                                    <div>
                                        <span className="badge">{auditHistory.length} entries</span>
                                    </div>
                                </div>
                                <div style={{padding: '16px'}}>
                                    <div style={{display: 'grid', gap: '12px'}}>
                                        {auditHistory.slice(0, 10).map((entry, index) => (
                                            <div key={index} style={{
                                                padding: '12px',
                                                background: 'rgba(255,255,255,0.02)',
                                                border: '1px solid rgba(255,255,255,0.08)',
                                                borderRadius: '8px'
                                            }}>
                                                <div style={{
                                                    display: 'flex',
                                                    justifyContent: 'space-between',
                                                    alignItems: 'center',
                                                    marginBottom: '8px'
                                                }}>
                                                    <span style={{
                                                        fontWeight: '600',
                                                        color: 'var(--text)'
                                                    }}>{entry.action}</span>
                                                    <span style={{fontSize: '12px', color: 'var(--muted)'}}>
                            {entry.changedAt ? new Date(entry.changedAt).toLocaleString() : '—'}
                          </span>
                                                </div>
                                                <div style={{fontSize: '14px', color: 'var(--muted)'}}>
                                                    <div>Version: {entry.version}</div>
                                                    <div>By: {entry.changedBy}</div>
                                                    {entry.changeDescription &&
                                                        <div>Changes: {entry.changeDescription}</div>}
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            </div>
                        )}
                    </>
                )}
            </div>
        </div>
    );
}

function DescriptionPreview({text}) {
    const html = useMemo(() => {
        if (!text) return '<span style="color: var(--muted)">No description provided.</span>';
        return text
            .replace(/^###### (.*$)/gim, '<h6>$1</h6>')
            .replace(/^##### (.*$)/gim, '<h5>$1</h5>')
            .replace(/^#### (.*$)/gim, '<h4>$1</h4>')
            .replace(/^### (.*$)/gim, '<h3>$1</h3>')
            .replace(/^## (.*$)/gim, '<h2>$1</h2>')
            .replace(/^# (.*$)/gim, '<h1>$1</h1>')
            .replace(/\*\*(.*?)\*\*/gim, '<strong>$1</strong>')
            .replace(/\*(.*?)\*/gim, '<em>$1</em>')
            .replace(/`(.*?)`/gim, '<code>$1</code>')
            .replace(/\n$/gim, '<br />');
    }, [text]);
    return <div style={{color: 'var(--text)'}} dangerouslySetInnerHTML={{__html: html}}/>;
}