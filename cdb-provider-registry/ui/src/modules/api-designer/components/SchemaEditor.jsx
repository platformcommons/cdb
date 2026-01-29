import React, { useState } from 'react';
import { Plus, Trash2, Save } from 'lucide-react';
import { useProject } from '../context';
import { SCHEMA_TYPES } from '../types';
import ParameterDetails from './ParameterDetails';

export function SchemaEditor() {
  const { project, addSchema, updateSchema, removeSchema } = useProject();
  const [selectedSchemaId, setSelectedSchemaId] = useState(null);
  const [showNewSchemaForm, setShowNewSchemaForm] = useState(false);
  const [newSchemaName, setNewSchemaName] = useState('');
  const [selectedParam, setSelectedParam] = useState(null);
  const [showDetails, setShowDetails] = useState(false);

  const selectedSchema = selectedSchemaId 
    ? project.schemas.find(s => s.id === selectedSchemaId)
    : null;

  const handleCreateSchema = () => {
    if (!newSchemaName.trim()) return;

    const newSchema = {
      id: Date.now().toString(),
      name: newSchemaName,
      type: 'object',
      properties: {},
      required: []
    };

    addSchema(newSchema);
    setSelectedSchemaId(newSchema.id);
    setNewSchemaName('');
    setShowNewSchemaForm(false);
  };

  const handleUpdateSchema = (updates) => {
    if (!selectedSchema) return;
    updateSchema(selectedSchema.id, updates);
  };

  const addProperty = () => {
    if (!selectedSchema) return;
    
    const newPropertyName = `property${Object.keys(selectedSchema.properties).length + 1}`;
    const updatedProperties = {
      ...selectedSchema.properties,
      [newPropertyName]: { 
        type: 'string',
        description: '',
        required: false,
        format: '',
        validation: {}
      }
    };
    
    handleUpdateSchema({ properties: updatedProperties });
  };

  const openDetails = (propertyName, propertySchema) => {
    setSelectedParam({ name: propertyName, ...propertySchema });
    setShowDetails(true);
  };

  const closeDetails = () => {
    setShowDetails(false);
    setSelectedParam(null);
  };

  const getTypeIcon = (type) => {
    switch (type) {
      case 'string': return '📝';
      case 'number': return '🔢';
      case 'integer': return '#️⃣';
      case 'boolean': return '✅';
      case 'array': return '📋';
      case 'object': return '📦';
      default: return '📄';
    }
  };

  const updateProperty = (oldName, newName, propertySchema) => {
    if (!selectedSchema) return;
    
    const updatedProperties = { ...selectedSchema.properties };
    
    if (oldName !== newName) {
      delete updatedProperties[oldName];
      updatedProperties[newName] = propertySchema;
      
      const required = selectedSchema.required || [];
      const updatedRequired = required.map(req => req === oldName ? newName : req);
      handleUpdateSchema({ properties: updatedProperties, required: updatedRequired });
    } else {
      updatedProperties[newName] = propertySchema;
      handleUpdateSchema({ properties: updatedProperties });
    }
  };

  const removeProperty = (propertyName) => {
    if (!selectedSchema) return;
    
    const updatedProperties = { ...selectedSchema.properties };
    delete updatedProperties[propertyName];
    
    const required = selectedSchema.required || [];
    const updatedRequired = required.filter(req => req !== propertyName);
    
    handleUpdateSchema({ properties: updatedProperties, required: updatedRequired });
  };

  const toggleRequired = (propertyName, isRequired) => {
    if (!selectedSchema) return;
    
    const required = selectedSchema.required || [];
    const updatedRequired = isRequired
      ? [...required, propertyName]
      : required.filter(req => req !== propertyName);
    
    handleUpdateSchema({ required: updatedRequired });
  };

  return (
    <div className="flex h-full min-h-0 overflow-hidden" style={{ background: 'var(--card)' }}>
      {/* Schema List */}
      <div className="h-full min-h-0 flex flex-col" style={{ width: '320px', borderRight: '1px solid rgba(255,255,255,0.08)' }}>
        <div style={{ padding: '16px', borderBottom: '1px solid rgba(255,255,255,0.08)' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
            <h3 style={{ fontWeight: 600, color: 'var(--text)' }}>Schemas</h3>
            <button
              onClick={() => setShowNewSchemaForm(true)}
              style={{ padding: '8px', color: 'var(--primary)', borderRadius: '8px', background: 'transparent', border: 'none', cursor: 'pointer' }}
              onMouseEnter={(e) => { e.currentTarget.style.background = 'rgba(255,255,255,0.06)'; }}
              onMouseLeave={(e) => { e.currentTarget.style.background = 'transparent'; }}
            >
              <Plus style={{ width: '16px', height: '16px' }} />
            </button>
          </div>

          {showNewSchemaForm && (
            <div style={{ display: 'grid', gap: '12px', padding: '12px', background: 'rgba(79,70,229,0.1)', borderRadius: '8px' }}>
              <input
                type="text"
                value={newSchemaName}
                onChange={(e) => setNewSchemaName(e.target.value)}
                placeholder="Schema name"
                style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.15)', background: 'rgba(255,255,255,0.04)', color: 'var(--text)' }}
              />
              <div style={{ display: 'flex', gap: '8px' }}>
                <button
                  onClick={handleCreateSchema}
                  disabled={!newSchemaName.trim()}
                  className="btn primary"
                  style={{ flex: 1, fontSize: '14px', opacity: !newSchemaName.trim() ? 0.5 : 1, cursor: !newSchemaName.trim() ? 'not-allowed' : 'pointer' }}
                >
                  Create
                </button>
                <button
                  onClick={() => {
                    setShowNewSchemaForm(false);
                    setNewSchemaName('');
                  }}
                  className="btn"
                  style={{ flex: 1, fontSize: '14px' }}
                >
                  Cancel
                </button>
              </div>
            </div>
          )}
        </div>

        <div className="flex-1 min-h-0 overflow-y-auto" style={{ padding: '16px', display: 'grid', gap: '8px' }}>
          {project.schemas.map((schema) => {
            const isSelected = selectedSchemaId === schema.id;
            return (
              <div
                key={schema.id}
                style={{
                  padding: '12px',
                  borderRadius: '8px',
                  border: isSelected ? '1px solid var(--primary)' : '1px solid rgba(255,255,255,0.12)',
                  background: isSelected ? 'rgba(79,70,229,0.12)' : 'transparent',
                  cursor: 'pointer',
                  transition: 'all 0.2s ease'
                }}
                onMouseEnter={(e) => {
                  if (!isSelected) {
                    e.currentTarget.style.border = '1px solid rgba(255,255,255,0.2)';
                    e.currentTarget.style.background = 'rgba(255,255,255,0.04)';
                  }
                }}
                onMouseLeave={(e) => {
                  if (!isSelected) {
                    e.currentTarget.style.border = '1px solid rgba(255,255,255,0.12)';
                    e.currentTarget.style.background = 'transparent';
                  }
                }}
                onClick={() => setSelectedSchemaId(schema.id)}
              >
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
                  <span style={{ fontWeight: 500, color: 'var(--text)' }}>{schema.name}</span>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      removeSchema(schema.id);
                      if (selectedSchemaId === schema.id) {
                        setSelectedSchemaId(null);
                      }
                    }}
                    style={{ padding: '4px', color: '#ef4444', background: 'transparent', border: 'none', borderRadius: '4px', cursor: 'pointer', opacity: 0 }}
                    onMouseEnter={(e) => { e.currentTarget.style.background = 'rgba(239,68,68,0.1)'; e.currentTarget.parentElement.parentElement.style.setProperty('--show-delete', '1'); }}
                    onMouseLeave={(e) => { e.currentTarget.style.background = 'transparent'; }}
                    ref={(el) => {
                      if (el) {
                        const parent = el.closest('[data-schema-item]');
                        if (parent) {
                          parent.addEventListener('mouseenter', () => { el.style.opacity = '1'; });
                          parent.addEventListener('mouseleave', () => { el.style.opacity = '0'; });
                        }
                      }
                    }}
                  >
                    <Trash2 style={{ width: '12px', height: '12px' }} />
                  </button>
                </div>
                
                <div style={{ fontSize: '14px', color: 'var(--muted)' }}>
                  {schema.type} • {Object.keys(schema.properties).length} properties
                </div>
              </div>
            );
          })}
          
          {project.schemas.length === 0 && (
            <div style={{ textAlign: 'center', padding: '32px 0' }} className="muted">
              <p style={{ marginBottom: '8px' }}>No schemas yet</p>
              <button
                onClick={() => setShowNewSchemaForm(true)}
                style={{ color: 'var(--primary)', background: 'transparent', border: 'none', fontWeight: 500, cursor: 'pointer' }}
              >
                Create your first schema
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Schema Editor */}
      <div className="flex-1 min-h-0 overflow-hidden">
        {selectedSchema ? (
          <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
            <div style={{ padding: '24px', borderBottom: '1px solid rgba(255,255,255,0.08)' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
                <h2 style={{ fontSize: '20px', fontWeight: 600, color: 'var(--text)' }}>Edit Schema</h2>
                <button className="btn primary" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <Save style={{ width: '16px', height: '16px' }} />
                  Save Changes
                </button>
              </div>

              <div style={{ display: 'grid', gap: '16px' }}>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                  <div>
                    <label style={{ display: 'block', fontSize: '14px', fontWeight: 500, color: 'var(--text)', marginBottom: '8px' }}>
                      Schema Name
                    </label>
                    <input
                      type="text"
                      value={selectedSchema.name}
                      onChange={(e) => handleUpdateSchema({ name: e.target.value })}
                      style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.15)', background: 'rgba(255,255,255,0.04)', color: 'var(--text)' }}
                    />
                  </div>
                  
                  <div>
                    <label style={{ display: 'block', fontSize: '14px', fontWeight: 500, color: 'var(--text)', marginBottom: '8px' }}>
                      Type
                    </label>
                    <select
                      value={selectedSchema.type}
                      onChange={(e) => handleUpdateSchema({ type: e.target.value })}
                      className="select"
                      style={{ width: '100%' }}
                    >
                      {SCHEMA_TYPES.map(type => (
                        <option key={type} value={type}>{type}</option>
                      ))}
                    </select>
                  </div>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '14px', fontWeight: 500, color: 'var(--text)', marginBottom: '8px' }}>
                    Description
                  </label>
                  <textarea
                    value={selectedSchema.description || ''}
                    onChange={(e) => handleUpdateSchema({ description: e.target.value })}
                    rows={3}
                    style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.15)', background: 'rgba(255,255,255,0.04)', color: 'var(--text)', resize: 'vertical' }}
                    placeholder="Schema description for documentation"
                  />
                </div>
              </div>
            </div>

            <div style={{ flex: 1, overflowY: 'auto', padding: '24px' }}>
              {selectedSchema.type === 'object' && (
                <div style={{ display: 'grid', gap: '16px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <h3 style={{ fontSize: '18px', fontWeight: 500, color: 'var(--text)' }}>Properties</h3>
                    <button
                      onClick={addProperty}
                      className="btn"
                      style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--primary)' }}
                    >
                      <Plus style={{ width: '16px', height: '16px' }} />
                      Add Property
                    </button>
                  </div>

                  <div style={{ display: 'grid', gap: '12px' }}>
                    {Object.entries(selectedSchema.properties).map(([propertyName, propertySchema]) => (
                      <div key={propertyName} className="parameter-item" style={{ border: '1px solid rgba(255,255,255,0.12)', borderRadius: '8px', padding: '16px' }}>
                        <div className="parameter-basic" style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                          <div className="parameter-icon" style={{ fontSize: '20px' }}>{getTypeIcon(propertySchema.type)}</div>
                          <div className="parameter-info" style={{ flex: 1 }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
                              <input
                                className="parameter-name"
                                value={propertyName}
                                onChange={(e) => updateProperty(propertyName, e.target.value, propertySchema)}
                                placeholder="Parameter name"
                                style={{ width: '70%', padding: '8px 12px', borderRadius: '6px', border: '1px solid rgba(255,255,255,0.15)', background: 'rgba(255,255,255,0.04)', color: 'var(--text)' }}
                              />
                              <select
                                value={propertySchema.type}
                                onChange={(e) => updateProperty(propertyName, propertyName, { ...propertySchema, type: e.target.value })}
                                className="type-select"
                                style={{ width: '30%', padding: '8px 12px', borderRadius: '6px', border: '1px solid rgba(255,255,255,0.15)', background: 'rgba(255,255,255,0.04)', color: 'var(--text)', fontSize: '14px' }}
                              >
                                <option value="string">string</option>
                                <option value="number">number</option>
                                <option value="integer">integer</option>
                                <option value="boolean">boolean</option>
                                <option value="array">array</option>
                                <option value="object">object</option>
                              </select>
                            </div>
                            <div className="parameter-meta" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                              {(selectedSchema.required || []).includes(propertyName) && (
                                <span className="required-badge" style={{ padding: '2px 6px', borderRadius: '4px', background: '#ef4444', color: 'white', fontSize: '10px', fontWeight: 500 }}>Required</span>
                              )}
                            </div>
                          </div>
                          <div className="parameter-actions" style={{ display: 'flex', gap: '4px' }}>
                            <button 
                              className="btn-icon details-btn"
                              onClick={() => openDetails(propertyName, propertySchema)}
                              title="Edit details"
                              style={{ padding: '6px', background: 'rgba(79,70,229,0.1)', border: 'none', borderRadius: '4px', cursor: 'pointer', color: 'var(--primary)' }}
                            >
                              ⚙️
                            </button>
                            <button 
                              className="btn-icon delete-btn"
                              onClick={() => removeProperty(propertyName)}
                              title="Delete parameter"
                              style={{ padding: '6px', background: 'rgba(239,68,68,0.1)', border: 'none', borderRadius: '4px', cursor: 'pointer', color: '#ef4444' }}
                            >
                              🗑️
                            </button>
                          </div>
                        </div>
                        {propertySchema.description && (
                          <div className="parameter-description" style={{ marginTop: '8px', padding: '8px', background: 'rgba(255,255,255,0.04)', borderRadius: '4px', fontSize: '14px', color: 'var(--muted)' }}>
                            {propertySchema.description}
                          </div>
                        )}
                      </div>
                    ))}

                    {Object.keys(selectedSchema.properties).length === 0 && (
                      <div style={{ textAlign: 'center', padding: '32px 0' }} className="muted">
                        <p style={{ marginBottom: '8px' }}>No properties defined</p>
                        <button
                          onClick={addProperty}
                          style={{ color: 'var(--primary)', background: 'transparent', border: 'none', fontWeight: 500, cursor: 'pointer' }}
                        >
                          Add your first property
                        </button>
                      </div>
                    )}
                  </div>
                </div>
              )}

              {selectedSchema.type === 'array' && (
                <div style={{ display: 'grid', gap: '16px' }}>
                  <h3 style={{ fontSize: '18px', fontWeight: 500, color: 'var(--text)' }}>Array Items</h3>
                  <div>
                    <label style={{ display: 'block', fontSize: '14px', fontWeight: 500, color: 'var(--text)', marginBottom: '8px' }}>
                      Items Type
                    </label>
                    <select
                      value={selectedSchema.items?.type || 'string'}
                      onChange={(e) => handleUpdateSchema({ 
                        items: { type: e.target.value } 
                      })}
                      className="select"
                      style={{ width: '100%' }}
                    >
                      <option value="string">String</option>
                      <option value="number">Number</option>
                      <option value="boolean">Boolean</option>
                      <option value="object">Object</option>
                    </select>
                  </div>
                </div>
              )}
            </div>
          </div>
        ) : (
          <div style={{ height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center' }} className="muted">
            <div style={{ textAlign: 'center' }}>
              <h3 style={{ fontSize: '18px', fontWeight: 500, marginBottom: '8px' }}>Select a schema to edit</h3>
              <p>Choose a schema from the list or create a new one to get started</p>
            </div>
          </div>
        )}
      </div>

      {showDetails && selectedParam && (
        <ParameterDetails
          parameter={selectedParam}
          onUpdate={(updates) => {
            const propertyName = selectedParam.name;
            const currentProperty = selectedSchema.properties[propertyName];
            updateProperty(propertyName, propertyName, { ...currentProperty, ...updates });
          }}
          onClose={closeDetails}
        />
      )}
    </div>
  );
}