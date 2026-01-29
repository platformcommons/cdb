import React, {useEffect, useState} from 'react'
import {Link} from 'react-router-dom'
import {getCurrentUser} from '../services/authService'

const iconFor = (text = '') => {
    const t = (text || '').toLowerCase()
    if (t.includes('ai') || t.includes('ml')) return '🤖'
    if (t.includes('cloud')) return '🌥️'
    if (t.includes('security') || t.includes('auth')) return '🔒'
    if (t.includes('data') || t.includes('analytics')) return '📊'
    if (t.includes('network')) return '🌐'
    if (t.includes('api')) return '🔌'
    if (t.includes('storage')) return '💾'
    if (t.includes('devops')) return '🛠️'
    if (t.includes('payment') || t.includes('pay')) return '💳'
    if (t.includes('iot')) return '📱'
    return '🏷️'
}

export default function Landing() {
    const [providers, setProviders] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)

    useEffect(() => {
        let cancelled = false

        async function load() {
            try {
                setProviders([
                    {id: 1, name: 'Provider A', description: 'Cloud Services'},
                    {id: 2, name: 'Provider B', description: 'Security Solutions'},
                    {id: 3, name: 'Provider C', description: 'Data Analytics'},
                    {id: 4, name: 'Provider D', description: 'AI Services'},
                    {id: 5, name: 'Provider E', description: 'Network Solutions'},
                    {id: 6, name: 'Provider F', description: 'API Services'},
                ])
            } catch (e) {
                console.error(e)
            } finally {
                if (!cancelled) setLoading(false)
            }
        }

        load()
        return () => {
            cancelled = true
        }
    }, [])

    const topProviders = providers.slice(0, 5)
    const latestProviders = [...providers]
        .sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0))
        .slice(0, 6)

    const ProviderCard = ({p}) => (
        <div className="provider-card">
            <span className="provider-icon">{iconFor(p.description || p.tags?.join(' ') || p.name)}</span>
            <h4 title={p.name}>{p.name}</h4>
            <p title={p.description || ''}>{p.description || '—'}</p>
        </div>
    )

    return (
        <section className="landing-page">
            <div className="hero-section">
                <h1>Commons Digital Backbone</h1>
                <p>Provider Registry: manage providers, metadata and integrations.</p>
                <div className="actions">
                    {!getCurrentUser() ? (
                        <>
                            <Link to="/signup" className="btn primary">Get Started</Link>
                            <Link to="/login" className="btn">Login</Link>
                        </>
                    ) : (
                        <Link to="/dashboard" className="btn primary">Go to Dashboard</Link>
                    )}
                </div>
            </div>
            <div className="features">
                <div className="card">
                    <h3>Explore API Registry</h3>
                    <img src="/images/api-explorer.svg" alt="API Explorer Interface" className="feature-image"/>
                    <p>Discover and explore available APIs through an intuitive interface. Browse documentation, test
                        endpoints,
                        and manage API integrations with ease.</p>
                </div>
                <div className="card">
                    <h3>Explore Provider Registry</h3>
                    <img src="/images/provider-registry.svg" alt="Provider Registry Interface"
                         className="feature-image"/>
                    <p>Browse, search and manage service providers in a centralized registry. Monitor provider status,
                        manage integrations and view detailed provider information.</p>
                </div>
            </div>

            <footer className="footer">
                <p>&copy; Commons Digital Backbone (2025)</p>
            </footer>
        </section>
    )
}
