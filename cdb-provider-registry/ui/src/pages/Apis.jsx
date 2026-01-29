import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiRegistryService } from '../services/apiRegistryService.js';

export default function Apis() {
  const navigate = useNavigate();
  const [apis, setApis] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const pageSize = 12;

  // Debounce search input
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(search);
      setCurrentPage(0); // Reset to first page when search changes
    }, 300);

    return () => clearTimeout(timer);
  }, [search]);

  const loadApis = useCallback(async () => {
    try {
      setLoading(true);
      const response = await apiRegistryService.getApis(currentPage, pageSize, debouncedSearch, statusFilter);
      
      // Handle different possible response structures
      if (response.data) {
        setApis(response.data.content || response.data.apis || response.data || []);
        setTotalPages(response.data.totalPages || Math.ceil((response.data.totalElements || 0) / pageSize));
        setTotalElements(response.data.totalElements || response.data.total || 0);
      } else {
        setApis(response.content || response.apis || response || []);
        setTotalPages(response.totalPages || Math.ceil((response.totalElements || 0) / pageSize));
        setTotalElements(response.totalElements || response.total || 0);
      }
    } catch (error) {
      console.error('Failed to load APIs:', error);
      setApis([]);
      setTotalPages(0);
      setTotalElements(0);
    } finally {
      setLoading(false);
    }
  }, [currentPage, debouncedSearch, statusFilter, pageSize]);

  useEffect(() => {
    loadApis();
  }, [loadApis]);

  const handleSearch = (e) => {
    setSearch(e.target.value);
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Escape') {
      setSearch('');
    }
  };

  const clearSearch = () => {
    setSearch('');
  };

  const handleStatusChip = (value) => {
    setStatusFilter(value === statusFilter ? '' : value);
    setCurrentPage(0);
  };

  const handleApiClick = (api) => {
    navigate(`/apis/${api.id}`);
  };

  const handleEdit = (api) => {
    navigate(`/apis/${api.id}/edit`);
  };

  const handleStatusUpdate = async (apiId, newStatus) => {
    try {
      await apiRegistryService.updateStatus(apiId, newStatus);
      loadApis();
    } catch (error) {
      console.error('Failed to update status:', error);
    }
  };

  const statusClass = (status) => {
    switch (status) {
      case 'PUBLISHED': return 'badge success';
      case 'DEPRECATED': return 'badge warning';
      case 'RETIRED': return 'badge danger';
      default: return 'badge';
    }
  };

  return (
    <div className="container">
      <div className="api-header">
        <div>
          <h1 style={{ margin: 0 }}>API Registry</h1>
          <p className="muted">
            Browse, manage, and publish your APIs with a modern registry experience.
            {(debouncedSearch || statusFilter) && (
              <span className="active-filters">
                {debouncedSearch && <span className="filter-tag">Search: "{debouncedSearch}"</span>}
                {statusFilter && <span className="filter-tag">Status: {statusFilter}</span>}
              </span>
            )}
          </p>
        </div>
        <div className="api-actions">
          <button className="btn" onClick={() => loadApis()}>Refresh</button>
          <button className="btn primary" onClick={() => navigate('/apis/new')}>+ New API</button>
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
            onChange={handleSearch}
            onKeyDown={handleKeyDown}
            placeholder="Search APIs by name, description, tag... (ESC to clear)" 
          />
          {search && (
            <button 
              className="search-clear" 
              onClick={clearSearch}
              title="Clear search (ESC)"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <line x1="18" y1="6" x2="6" y2="18"/>
                <line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          )}
          {loading && debouncedSearch !== search && (
            <div className="search-loading">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="animate-spin">
                <path d="M21 12a9 9 0 11-6.219-8.56"/>
              </svg>
            </div>
          )}
        </div>
        <div className="api-filters">
          {['', 'DRAFT', 'PUBLISHED', 'DEPRECATED', 'RETIRED'].map(v => (
            <button
              key={v || 'ALL'}
              className={`chip ${statusFilter === v ? 'active' : ''}`}
              onClick={() => handleStatusChip(v)}
            >
              {v || 'All'}
            </button>
          ))}
        </div>
      </div>

      {!loading && apis.length === 0 && (
        <div className="empty-state">
          <div className="empty-graphic">{debouncedSearch || statusFilter ? '🔍' : '🔌'}</div>
          <h3>{debouncedSearch || statusFilter ? 'No matching APIs found' : 'No APIs found'}</h3>
          <p className="muted">
            {debouncedSearch || statusFilter 
              ? 'Try adjusting your search terms or filters to find what you\'re looking for.' 
              : 'Start by creating your first API entry in the registry.'
            }
          </p>
          {debouncedSearch || statusFilter ? (
            <div className="empty-actions">
              <button className="btn" onClick={() => { setSearch(''); setStatusFilter(''); }}>Clear filters</button>
              <button className="btn primary" onClick={() => navigate('/apis/new')}>Create API</button>
            </div>
          ) : (
            <button className="btn primary" onClick={() => navigate('/apis/new')}>Create API</button>
          )}
        </div>
      )}

      {loading && (
        <div className="api-grid">
          {Array.from({ length: 6 }).map((_, i) => (
            <div className="api-card skeleton" key={i} />
          ))}
        </div>
      )}

      {!loading && apis.length > 0 && (
        <div className="api-grid">
          {apis.map(api => (
            <div key={api.id} className="api-card" onClick={() => handleApiClick(api)}>
              <div className="api-card-header">
                <div className="api-avatar">{api.name?.substring(0, 2)?.toUpperCase()}</div>
                <div className="api-meta">
                  <div className="api-title">{api.name}</div>
                  <div className="api-subtitle">{api.basePath || '/'} · v{api.version}</div>
                </div>
                <span className={statusClass(api.status)}>{api.status}</span>
              </div>
              <p className="api-desc">{api.description || 'No description provided.'}</p>
              <div className="api-footer">
                <div className="muted">Updated {api.updatedAt ? new Date(api.updatedAt).toLocaleDateString() : '—'}</div>
                <div className="api-actions-inline" onClick={(e) => e.stopPropagation()}>
                  <button className="btn" onClick={() => handleEdit(api)}>Edit</button>
                  <select className="select" value={api.status} onChange={e => handleStatusUpdate(api.id, e.target.value)}>
                    <option value="DRAFT">Draft</option>
                    <option value="PUBLISHED">Published</option>
                    <option value="DEPRECATED">Deprecated</option>
                    <option value="RETIRED">Retired</option>
                  </select>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Pagination and Results Info */}
      {!loading && (
        <div className="pagination-container">
          <div className="results-info">
            <span className="muted">
              Showing {apis.length > 0 ? (currentPage * pageSize + 1) : 0} to {Math.min((currentPage + 1) * pageSize, totalElements)} of {totalElements} APIs
            </span>
          </div>
          
          {totalPages > 1 && (
            <div className="pagination">
              <button 
                className="btn" 
                disabled={currentPage === 0} 
                onClick={() => setCurrentPage(0)}
                title="First page"
              >
                ««
              </button>
              <button 
                className="btn" 
                disabled={currentPage === 0} 
                onClick={() => setCurrentPage(Math.max(0, currentPage - 1))}
                title="Previous page"
              >
                ‹ Previous
              </button>
              
              <div className="pages">
                {(() => {
                  const pages = [];
                  const maxVisiblePages = 5;
                  let startPage = Math.max(0, currentPage - Math.floor(maxVisiblePages / 2));
                  let endPage = Math.min(totalPages - 1, startPage + maxVisiblePages - 1);
                  
                  // Adjust start if we're near the end
                  if (endPage - startPage < maxVisiblePages - 1) {
                    startPage = Math.max(0, endPage - maxVisiblePages + 1);
                  }
                  
                  // Add first page if not visible
                  if (startPage > 0) {
                    pages.push(
                      <button key={0} className="chip" onClick={() => setCurrentPage(0)}>1</button>
                    );
                    if (startPage > 1) {
                      pages.push(<span key="start-ellipsis" className="pagination-ellipsis">...</span>);
                    }
                  }
                  
                  // Add visible pages
                  for (let i = startPage; i <= endPage; i++) {
                    pages.push(
                      <button 
                        key={i} 
                        className={`chip ${i === currentPage ? 'active' : ''}`} 
                        onClick={() => setCurrentPage(i)}
                      >
                        {i + 1}
                      </button>
                    );
                  }
                  
                  // Add last page if not visible
                  if (endPage < totalPages - 1) {
                    if (endPage < totalPages - 2) {
                      pages.push(<span key="end-ellipsis" className="pagination-ellipsis">...</span>);
                    }
                    pages.push(
                      <button key={totalPages - 1} className="chip" onClick={() => setCurrentPage(totalPages - 1)}>
                        {totalPages}
                      </button>
                    );
                  }
                  
                  return pages;
                })()}
              </div>
              
              <button 
                className="btn" 
                disabled={currentPage >= totalPages - 1} 
                onClick={() => setCurrentPage(Math.min(totalPages - 1, currentPage + 1))}
                title="Next page"
              >
                Next ›
              </button>
              <button 
                className="btn" 
                disabled={currentPage >= totalPages - 1} 
                onClick={() => setCurrentPage(totalPages - 1)}
                title="Last page"
              >
                »»
              </button>
            </div>
          )}
        </div>
      )}


    </div>
  );
}
