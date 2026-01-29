import React, {useState, useEffect} from 'react'
import {
    searchProviders,
    createProviderRequest,
    getMyRequests,
    registerSimpleProvider
} from '../services/providerService'
import {fetchMyProviders, setExecutiveContext} from "../services/authService.js";
import {useNavigate} from "react-router-dom";

export default function NoProvider() {
    const [searchTerm, setSearchTerm] = useState('')
    const [providers, setProviders] = useState([])
    const [myRequests, setMyRequests] = useState([])
    const [loading, setLoading] = useState(false)
    const [requesting, setRequesting] = useState(false)
    const [error, setError] = useState('')
    const [success, setSuccess] = useState('')
    const [showAddProvider, setShowAddProvider] = useState(false)
    const [newProvider, setNewProvider] = useState({
        name: '',
        code: '',
        description: '',
        contactEmail: '',
        contactPhone: ''
    })
    const [registering, setRegistering] = useState(false)
    const [providerOptions, setProviderOptions] = useState([])
    const [showProviderModal, setShowProviderModal] = useState(false)
    const [selecting, setSelecting] = useState(false)
    const navigate = useNavigate()

    useEffect(() => {
        //loadMyRequests()
    }, [])

    const loadMyRequests = async () => {
        try {
            const requests = await getMyRequests()
            setMyRequests(requests)
        } catch (err) {
            console.error('Failed to load requests:', err)
        }
    }

    const handleSearch = async () => {
        if (!searchTerm.trim()) return
        setLoading(true)
        setError('')
        try {
            const results = await searchProviders(searchTerm)
            setProviders(results)
        } catch (err) {
            setError('Failed to search providers')
        } finally {
            setLoading(false)
        }
    }

    const handleRequest = async (providerId, providerName) => {
        setRequesting(true)
        setError('')
        setSuccess('')
        try {
            await createProviderRequest({
                providerId,
                requestMessage: `I would like to join ${providerName} as a user.`,
                requestedRole: 'USER'
            })
            setSuccess('Request sent successfully')
            await loadMyRequests()
        } catch (err) {
            setError(err.message || 'Failed to send request')
        } finally {
            setRequesting(false)
        }
    }

    const isRequested = (providerId) => {
        return myRequests.some(req => req.providerId === providerId && req.status === 'PENDING')
    }

    const handleAddProvider = async () => {
        if (!newProvider.name.trim() || !newProvider.code.trim()) {
            setError('Provider name and code are required')
            return
        }

        setRegistering(true)
        setError('')
        setSuccess('')
        try {
            await registerSimpleProvider(newProvider)
            setSuccess('Provider registered successfully! You now have access.')
            const options = await fetchMyProviders()
            if (Array.isArray(options) && options.length > 1) {
                setProviderOptions(options)
                setShowProviderModal(true)
            } else if (Array.isArray(options) && options.length === 1) {
                // Auto select single option
                setSelecting(true)
                try {
                    await setExecutiveContext(options[0].providerCode)
                    // Refresh auth state before navigation
                    if (window.refreshAuth) window.refreshAuth()
                    navigate('/dashboard', {replace: true})
                } finally {
                    setSelecting(false)
                }
            }
            setShowAddProvider(false)
            setNewProvider({name: '', code: '', description: '', contactEmail: '', contactPhone: ''})
            // Refresh the page or redirect to dashboard
            //  setTimeout(() => window.location.reload(), 2000)
        } catch (err) {
            setError(err.message || 'Failed to register provider')
        } finally {
            setRegistering(false)
        }
    }

    const handleProviderSelect = async (providerCode) => {
        setSelecting(true)
        try {
            await setExecutiveContext(providerCode)
            setShowProviderModal(false)
            // Refresh auth state before navigation
            if (window.refreshAuth) window.refreshAuth()
            navigate('/dashboard', { replace: true })
        } catch (e) {
            setError(e.message || 'Failed to set provider context')
        } finally {
            setSelecting(false)
        }
    }

    return (
        <div className="container" style={{maxWidth: '900px'}}>
            <div style={{textAlign: 'center', marginBottom: '40px'}}>
                <h1>No Provider Access</h1>
                <p style={{color: '#6b7280', fontSize: '18px'}}>
                    You are not currently associated with any provider.
                </p>
                <button
                    onClick={() => setShowAddProvider(true)}
                    className="btn btn-primary"
                    style={{
                        marginTop: '20px',
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '8px'
                    }}
                >
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <line x1="12" y1="5" x2="12" y2="19"></line>
                        <line x1="5" y1="12" x2="19" y2="12"></line>
                    </svg>
                    Add New Provider
                </button>
            </div>

            {error && (
                <div className="alert error">
                    <svg className="alert-icon" viewBox="0 0 20 20" fill="currentColor">
                        <path fillRule="evenodd"
                              d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z"
                              clipRule="evenodd"/>
                    </svg>
                    {error}
                </div>
            )}

            {success && (
                <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '12px',
                    background: 'rgba(16, 185, 129, 0.1)',
                    border: '1px solid rgba(16, 185, 129, 0.2)',
                    color: '#34d399',
                    padding: '12px',
                    borderRadius: '12px',
                    marginBottom: '20px'
                }}>
                    <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                        <path fillRule="evenodd"
                              d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                              clipRule="evenodd"/>
                    </svg>
                    {success}
                </div>
            )}

            {showAddProvider && (
                <div className="modal-overlay">
                    <div className="modal-content" style={{maxWidth: '650px', width: '90vw'}}>
                        <div style={{
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                            marginBottom: '24px'
                        }}>
                            <div style={{display: 'flex', alignItems: 'center', gap: '12px'}}>
                                <div style={{
                                    width: '40px',
                                    height: '40px',
                                    background: 'linear-gradient(135deg, var(--primary), #6366f1)',
                                    borderRadius: '12px',
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center'
                                }}>
                                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="white"
                                         strokeWidth="2">
                                        <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                                        <circle cx="8.5" cy="7" r="4"/>
                                        <line x1="20" y1="8" x2="20" y2="14"/>
                                        <line x1="23" y1="11" x2="17" y2="11"/>
                                    </svg>
                                </div>
                                <div>
                                    <h2 style={{
                                        margin: 0,
                                        fontSize: '20px',
                                        fontWeight: '600',
                                        color: 'var(--text)'
                                    }}>Add New Provider</h2>
                                    <p style={{margin: 0, fontSize: '14px', color: 'var(--muted)'}}>Create your provider
                                        organization</p>
                                </div>
                            </div>
                            <button
                                onClick={() => {
                                    setShowAddProvider(false)
                                    setNewProvider({
                                        name: '',
                                        code: '',
                                        description: '',
                                        contactEmail: '',
                                        contactPhone: ''
                                    })
                                    setError('')
                                }}
                                style={{
                                    background: 'none',
                                    border: 'none',
                                    color: 'var(--muted)',
                                    cursor: 'pointer',
                                    padding: '4px',
                                    borderRadius: '6px'
                                }}
                            >
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                                     strokeWidth="2">
                                    <line x1="18" y1="6" x2="6" y2="18"></line>
                                    <line x1="6" y1="6" x2="18" y2="18"></line>
                                </svg>
                            </button>
                        </div>

                        <div style={{display: 'grid', gap: '20px'}}>
                            <div style={{display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px'}}>
                                <div className="form-group" style={{marginBottom: 0}}>
                                    <label className="form-label">Provider Name *</label>
                                    <input
                                        type="text"
                                        className="form-input"
                                        value={newProvider.name}
                                        onChange={(e) => setNewProvider({...newProvider, name: e.target.value})}
                                        placeholder="Enter provider name"
                                    />
                                    <div style={{height: '20px'}}></div>
                                </div>

                                <div className="form-group" style={{marginBottom: 0}}>
                                    <label className="form-label">Provider Code *</label>
                                    <input
                                        type="text"
                                        className="form-input"
                                        value={newProvider.code}
                                        onChange={(e) => setNewProvider({...newProvider, code: e.target.value})}
                                        placeholder="e.g. my-company"
                                    />
                                    <small className="form-hint">Unique identifier for your provider</small>
                                </div>
                            </div>

                            <div className="form-group" style={{marginBottom: 0}}>
                                <label className="form-label">Description</label>
                                <textarea
                                    className="form-input"
                                    value={newProvider.description}
                                    onChange={(e) => setNewProvider({...newProvider, description: e.target.value})}
                                    placeholder="Brief description of your provider"
                                    rows={3}
                                    style={{resize: 'vertical'}}
                                />
                            </div>

                            <div style={{
                                display: 'grid',
                                gridTemplateColumns: '1fr 1fr',
                                gap: '16px',
                                alignItems: 'start'
                            }}>
                                <div className="form-group" style={{marginBottom: 0}}>
                                    <label className="form-label">Contact Email</label>
                                    <input
                                        type="email"
                                        className="form-input"
                                        value={newProvider.contactEmail}
                                        onChange={(e) => setNewProvider({...newProvider, contactEmail: e.target.value})}
                                        placeholder="contact@yourcompany.com"
                                    />
                                </div>

                                <div className="form-group" style={{marginBottom: 0}}>
                                    <label className="form-label">Contact Phone</label>
                                    <input
                                        type="tel"
                                        className="form-input"
                                        value={newProvider.contactPhone}
                                        onChange={(e) => setNewProvider({...newProvider, contactPhone: e.target.value})}
                                        placeholder="+1 (555) 123-4567"
                                    />
                                </div>
                            </div>
                        </div>

                        <div style={{
                            display: 'flex',
                            gap: '12px',
                            justifyContent: 'flex-end',
                            marginTop: '32px',
                            paddingTop: '24px',
                            borderTop: '1px solid rgba(255,255,255,0.08)'
                        }}>
                            <button
                                onClick={() => {
                                    setShowAddProvider(false)
                                    setNewProvider({
                                        name: '',
                                        code: '',
                                        description: '',
                                        contactEmail: '',
                                        contactPhone: ''
                                    })
                                    setError('')
                                }}
                                disabled={registering}
                                className="btn btn-secondary"
                            >
                                Cancel
                            </button>
                            <button
                                onClick={handleAddProvider}
                                disabled={registering || !newProvider.name.trim() || !newProvider.code.trim()}
                                className="btn btn-primary"
                                style={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: '8px'
                                }}
                            >
                                {registering ? (
                                    <>
                                        <svg className="btn-spinner" width="16" height="16" viewBox="0 0 24 24">
                                            <circle cx="12" cy="12" r="10" fill="none" stroke="currentColor"
                                                    strokeWidth="4" opacity="0.25"/>
                                            <path fill="currentColor"
                                                  d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                                                  opacity="0.75"/>
                                        </svg>
                                        Creating...
                                    </>
                                ) : (
                                    <>
                                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none"
                                             stroke="currentColor" strokeWidth="2">
                                            <line x1="12" y1="5" x2="12" y2="19"></line>
                                            <line x1="5" y1="12" x2="19" y2="12"></line>
                                        </svg>
                                        Create Provider
                                    </>
                                )}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            <div style={{marginBottom: '30px'}}>
                <h2 style={{color: 'var(--text)', marginBottom: '16px'}}>Search Existing Providers</h2>
                <div className="api-search" style={{marginTop: '0'}}>
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <circle cx="11" cy="11" r="8"></circle>
                        <path d="m21 21-4.35-4.35"></path>
                    </svg>
                    <input
                        type="text"
                        placeholder="Search by provider name or code..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
                    />
                    <button
                        onClick={handleSearch}
                        disabled={loading || !searchTerm.trim()}
                        className="btn btn-primary"
                        style={{marginLeft: '12px'}}
                    >
                        {loading ? 'Searching...' : 'Search'}
                    </button>
                </div>
            </div>

            {providers.length > 0 && (
                <div style={{marginBottom: '30px'}}>
                    <h3 style={{color: 'var(--text)', marginBottom: '16px'}}>Available Providers</h3>
                    <div style={{display: 'grid', gap: '16px', marginTop: '10px'}}>
                        {providers.map(provider => (
                            <div key={provider.id} className="card" style={{
                                display: 'flex',
                                justifyContent: 'space-between',
                                alignItems: 'center',
                                padding: '20px'
                            }}>
                                <div style={{flex: 1}}>
                                    <h4 style={{
                                        margin: '0 0 8px 0',
                                        color: 'var(--text)',
                                        fontSize: '16px',
                                        fontWeight: '600'
                                    }}>{provider.name}</h4>
                                    <div style={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        gap: '16px',
                                        marginBottom: '8px'
                                    }}>
                                        <span className="badge" style={{fontSize: '12px'}}>Code: {provider.code}</span>
                                        <span
                                            className={`badge ${provider.status === 'ACTIVE' ? 'success' : 'warning'}`}>
                      {provider.status}
                    </span>
                                    </div>
                                    {provider.description && (
                                        <p style={{
                                            margin: '0',
                                            fontSize: '14px',
                                            color: 'var(--muted)'
                                        }}>{provider.description}</p>
                                    )}
                                </div>
                                <button
                                    /*  onClick={() => handleRequest(provider.id, provider.name)}*/
                                    disabled={requesting || isRequested(provider.id)}
                                    className={`btn ${isRequested(provider.id) ? 'btn-secondary' : 'btn-primary'}`}
                                    style={{
                                        opacity: isRequested(provider.id) ? 0.6 : 1,
                                        cursor: isRequested(provider.id) ? 'not-allowed' : 'pointer'
                                    }}
                                >
                                    {isRequested(provider.id) ? 'Requested' : 'Request Access'}
                                </button>
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {myRequests.length > 0 && (
                <div>
                    <h3 style={{color: 'var(--text)', marginBottom: '16px'}}>My Requests</h3>
                    <div style={{display: 'grid', gap: '16px', marginTop: '10px'}}>
                        {myRequests.map(request => (
                            <div key={request.id} className="card" style={{padding: '20px'}}>
                                <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'start'}}>
                                    <div style={{flex: 1}}>
                                        <h4 style={{
                                            margin: '0 0 8px 0',
                                            color: 'var(--text)',
                                            fontSize: '16px',
                                            fontWeight: '600'
                                        }}>{request.providerName}</h4>
                                        <div style={{
                                            display: 'flex',
                                            alignItems: 'center',
                                            gap: '16px',
                                            marginBottom: '8px'
                                        }}>
                                            <span className="badge"
                                                  style={{fontSize: '12px'}}>Code: {request.providerCode}</span>
                                            <span className="badge"
                                                  style={{fontSize: '12px'}}>Role: {request.requestedRole}</span>
                                        </div>
                                        {request.requestMessage && (
                                            <p style={{
                                                margin: '0',
                                                fontSize: '14px',
                                                color: 'var(--muted)'
                                            }}>{request.requestMessage}</p>
                                        )}
                                    </div>
                                    <span className={`badge ${
                                        request.status === 'PENDING' ? 'warning' :
                                            request.status === 'APPROVED' ? 'success' : 'danger'
                                    }`}>
                    {request.status}
                  </span>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {showProviderModal && (
                <div style={modalStyles.backdrop}>
                    <div style={modalStyles.modal} role="dialog" aria-modal="true">
                        <div style={{display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '20px'}}>
                            <div style={{
                                width: '40px',
                                height: '40px',
                                background: 'linear-gradient(135deg, var(--primary), #6366f1)',
                                borderRadius: '12px',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center'
                            }}>
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2">
                                    <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                                    <circle cx="8.5" cy="7" r="4"/>
                                    <line x1="20" y1="8" x2="20" y2="14"/>
                                    <line x1="23" y1="11" x2="17" y2="11"/>
                                </svg>
                            </div>
                            <div>
                                <h3 style={{margin: 0, fontSize: '20px', fontWeight: '600', color: 'var(--text)'}}>Choose Provider Context</h3>
                                <p style={{margin: 0, fontSize: '14px', color: 'var(--muted)'}}>Select which provider context you want to use for this session.</p>
                            </div>
                        </div>
                        <div style={{display:'grid', gap:'0px'}}>
                            {providerOptions.map((opt, idx) => (
                                <button key={idx}
                                        disabled={selecting}
                                        style={modalStyles.optionBtn}
                                        onMouseEnter={(e) => {
                                            e.target.style.borderColor = 'rgba(255,255,255,0.25)'
                                            e.target.style.background = 'rgba(255,255,255,0.08)'
                                            e.target.style.transform = 'translateY(-1px)'
                                        }}
                                        onMouseLeave={(e) => {
                                            e.target.style.borderColor = 'rgba(255,255,255,0.15)'
                                            e.target.style.background = 'rgba(255,255,255,0.04)'
                                            e.target.style.transform = 'translateY(0px)'
                                        }}
                                        onClick={() => handleProviderSelect(opt.providerCode)}>
                                    <div style={{display: 'flex', alignItems: 'center', gap: '12px'}}>
                                        <div style={{
                                            width: '32px',
                                            height: '32px',
                                            background: 'rgba(79,70,229,0.2)',
                                            borderRadius: '8px',
                                            display: 'flex',
                                            alignItems: 'center',
                                            justifyContent: 'center',
                                            fontSize: '14px',
                                            fontWeight: '700',
                                            color: '#c7c9ff'
                                        }}>
                                            {opt.providerCode.charAt(0).toUpperCase()}
                                        </div>
                                        <div style={{flex: 1, textAlign: 'left'}}>
                                            <div style={{fontWeight: 600, fontSize: '16px', color: 'var(--text)'}}>{opt.providerCode}</div>
                                            <div style={{fontSize: '12px', color: 'var(--muted)'}}>ID: {opt.providerId ?? '—'}</div>
                                        </div>
                                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--muted)" strokeWidth="2">
                                            <polyline points="9,18 15,12 9,6"></polyline>
                                        </svg>
                                    </div>
                                </button>
                            ))}
                        </div>
                        <div style={{marginTop:'20px', display:'flex', justifyContent:'flex-end', gap:'12px'}}>
                            <button 
                                className="btn" 
                                onClick={() => setShowProviderModal(false)} 
                                disabled={selecting}
                                style={{
                                    padding: '10px 16px',
                                    borderRadius: '8px',
                                    border: '1px solid rgba(255,255,255,0.15)',
                                    background: 'rgba(255,255,255,0.04)',
                                    color: 'var(--text)',
                                    cursor: 'pointer'
                                }}
                            >
                                Cancel
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    )
}

const modalStyles = {
    backdrop: {
        position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.55)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000
    },
    modal: {
        background: 'var(--card)', padding: '24px', borderRadius: '16px', width: 'min(500px, 96vw)', boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)', border: '1px solid rgba(255,255,255,0.1)', backdropFilter: 'blur(10px)'
    },
    optionBtn: {
        textAlign: 'left', border: '1px solid rgba(255,255,255,0.15)', borderRadius: '12px', padding: '14px 16px', background: 'rgba(255,255,255,0.04)', cursor: 'pointer', color: 'var(--text)', transition: 'all 0.2s ease', marginBottom: '8px'
    }
}