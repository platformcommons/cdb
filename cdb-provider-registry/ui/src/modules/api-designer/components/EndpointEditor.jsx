import React, { useState } from 'react';
import { Save, Plus, Trash2, Settings } from 'lucide-react';
import { useProject } from '../context';
import { API_METHODS, PARAMETER_LOCATIONS, SCHEMA_TYPES, CONTENT_TYPES } from '../types';
import ParameterDetails from './ParameterDetails';

export function EndpointEditor({ endpoint }) {
  const { project, updateEndpoint } = useProject();
  const [activeTab, setActiveTab] = useState('general');
  const [selectedParameter, setSelectedParameter] = useState(null);
  const [showParameterDetails, setShowParameterDetails] = useState(false);

  const handleUpdate = (updates) => {
    updateEndpoint(endpoint.id, updates);
  };

  const addParameter = () => {
    const newParam = {
      name: 'newParam',
      in: 'query',
      required: false,
      schema: { type: 'string' },
    };
    handleUpdate({
      parameters: [...(endpoint.parameters || []), newParam],
    });
  };

  const updateParameter = (index, updates) => {
    const updatedParams = [...(endpoint.parameters || [])];
    updatedParams[index] = { ...updatedParams[index], ...updates };
    handleUpdate({ parameters: updatedParams });
  };

  const removeParameter = (index) => {
    const updatedParams = (endpoint.parameters || []).filter((_, i) => i !== index);
    handleUpdate({ parameters: updatedParams });
  };

  const addRequestBodyContent = (contentType) => {
    const newContent = {
      schema: { type: 'object', properties: {} },
      example: contentType === 'application/json' ? '{}' : ''
    };
    const updatedRequestBody = {
      ...endpoint.requestBody,
      content: {
        ...(endpoint.requestBody?.content || {}),
        [contentType]: newContent
      }
    };
    handleUpdate({ requestBody: updatedRequestBody });
  };

  const removeRequestBodyContent = (contentType) => {
    const updatedContent = { ...(endpoint.requestBody?.content || {}) };
    delete updatedContent[contentType];
    const updatedRequestBody = {
      ...endpoint.requestBody,
      content: updatedContent
    };
    handleUpdate({ requestBody: updatedRequestBody });
  };

  const updateRequestBodyContent = (contentType, updates) => {
    const updatedRequestBody = {
      ...endpoint.requestBody,
      content: {
        ...(endpoint.requestBody?.content || {}),
        [contentType]: {
          ...(endpoint.requestBody?.content?.[contentType] || {}),
          ...updates
        }
      }
    };
    handleUpdate({ requestBody: updatedRequestBody });
  };

  const addSchemaProperty = (contentType) => {
    const currentSchema = endpoint.requestBody?.content?.[contentType]?.schema || { type: 'object', properties: {} };
    const newPropertyName = `property${Object.keys(currentSchema.properties || {}).length + 1}`;
    const updatedSchema = {
      ...currentSchema,
      properties: {
        ...(currentSchema.properties || {}),
        [newPropertyName]: { type: 'string', description: '' }
      }
    };
    updateRequestBodyContent(contentType, { schema: updatedSchema });
  };

  const updateSchemaProperty = (contentType, oldName, newName, propertySchema) => {
    const currentSchema = endpoint.requestBody?.content?.[contentType]?.schema || { type: 'object', properties: {} };
    const updatedProperties = { ...(currentSchema.properties || {}) };
    
    if (oldName !== newName) {
      delete updatedProperties[oldName];
    }
    updatedProperties[newName] = propertySchema;
    
    const updatedSchema = {
      ...currentSchema,
      properties: updatedProperties
    };
    updateRequestBodyContent(contentType, { schema: updatedSchema });
  };

  const removeSchemaProperty = (contentType, propertyName) => {
    const currentSchema = endpoint.requestBody?.content?.[contentType]?.schema || { type: 'object', properties: {} };
    const updatedProperties = { ...(currentSchema.properties || {}) };
    delete updatedProperties[propertyName];
    
    const updatedSchema = {
      ...currentSchema,
      properties: updatedProperties
    };
    updateRequestBodyContent(contentType, { schema: updatedSchema });
  };

  const tabs = [
    { id: 'general', label: 'General' },
    { id: 'parameters', label: 'Parameters' },
    ...(endpoint.method !== 'GET' ? [{ id: 'requestBody', label: 'Request Body' }] : []),
    { id: 'responses', label: 'Responses' },
  ];

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column', background: 'var(--card)' }}>
      <div style={{ padding: '24px', borderBottom: '1px solid rgba(255,255,255,0.08)' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
          <h2 style={{ fontSize: '20px', fontWeight: 600, color: 'var(--text)' }}>Edit Endpoint</h2>
          <button className="btn primary" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Save style={{ width: '16px', height: '16px' }} />
            Save Changes
          </button>
        </div>

        <div style={{ display: 'flex', gap: '4px', background: 'rgba(255,255,255,0.04)', borderRadius: '8px', padding: '4px' }}>
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className="chip"
              style={{
                ...(activeTab === tab.id
                  ? { background: 'var(--primary)', borderColor: 'var(--primary)', color: 'white' }
                  : { color: 'var(--muted)' })
              }}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      <div style={{ flex: 1, overflow: 'auto', padding: '24px' }}>
        {activeTab === 'general' && (
          <div style={{ display: 'grid', gap: '24px' }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '14px', fontWeight: 500, color: 'var(--text)', marginBottom: '8px' }}>
                  Method
                </label>
                <select
                  value={endpoint.method}
                  onChange={(e) => handleUpdate({ method: e.target.value })}
                  className="select"
                  style={{ width: '100%' }}
                >
                  {API_METHODS.map(method => (
                    <option key={method} value={method}>{method}</option>
                  ))}
                </select>
              </div>
              
              <div>
                <label style={{ display: 'block', fontSize: '14px', fontWeight: 500, color: 'var(--text)', marginBottom: '8px' }}>
                  Path
                </label>
                <input
                  type="text"
                  value={endpoint.path}
                  onChange={(e) => handleUpdate({ path: e.target.value })}
                  style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.15)', background: 'rgba(255,255,255,0.04)', color: 'var(--text)', fontFamily: 'monospace' }}
                  placeholder="/api/endpoint"
                />
              </div>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '14px', fontWeight: 500, color: 'var(--text)', marginBottom: '8px' }}>
                Summary
              </label>
              <input
                type="text"
                value={endpoint.summary}
                onChange={(e) => handleUpdate({ summary: e.target.value })}
                style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.15)', background: 'rgba(255,255,255,0.04)', color: 'var(--text)' }}
                placeholder="Brief description of the endpoint"
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '14px', fontWeight: 500, color: 'var(--text)', marginBottom: '8px' }}>
                Description
              </label>
              <textarea
                value={endpoint.description}
                onChange={(e) => handleUpdate({ description: e.target.value })}
                rows={4}
                style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.15)', background: 'rgba(255,255,255,0.04)', color: 'var(--text)', resize: 'vertical' }}
                placeholder="Detailed description of the endpoint functionality"
              />
            </div>
          </div>
        )}

        {activeTab === 'parameters' && (
          <div style={{ display: 'grid', gap: '16px' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <h3 style={{ fontSize: '18px', fontWeight: 500, color: 'var(--text)' }}>Parameters</h3>
              <button
                onClick={addParameter}
                className="btn"
                style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--primary)' }}
              >
                <Plus style={{ width: '16px', height: '16px' }} />
                Add Parameter
              </button>
            </div>

            {(endpoint.parameters || []).map((param, index) => (
              <div key={index} style={{ border: '1px solid rgba(255,255,255,0.08)', borderRadius: '8px', padding: '16px', background: 'rgba(255,255,255,0.02)' }}>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 120px 120px auto auto auto', gap: '12px', alignItems: 'end' }}>
                  <div>
                    <label style={{ display: 'block', fontSize: '14px', fontWeight: 500, color: 'var(--text)', marginBottom: '4px' }}>
                      Name
                    </label>
                    <input
                      type="text"
                      value={param.name}
                      onChange={(e) => updateParameter(index, { name: e.target.value })}
                      style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.15)', background: 'rgba(255,255,255,0.04)', color: 'var(--text)' }}
                    />
                  </div>
                  
                  <div>
                    <label style={{ display: 'block', fontSize: '14px', fontWeight: 500, color: 'var(--text)', marginBottom: '4px' }}>
                      Location
                    </label>
                    <select
                      value={param.in}
                      onChange={(e) => updateParameter(index, { in: e.target.value })}
                      className="select"
                      style={{ width: '100%' }}
                    >
                      {PARAMETER_LOCATIONS.map(location => (
                        <option key={location} value={location}>{location.charAt(0).toUpperCase() + location.slice(1)}</option>
                      ))}
                    </select>
                  </div>
                  
                  <div>
                    <label style={{ display: 'block', fontSize: '14px', fontWeight: 500, color: 'var(--text)', marginBottom: '4px' }}>
                      Type
                    </label>
                    <select
                      value={param.schema?.type || 'string'}
                      onChange={(e) => updateParameter(index, { 
                        schema: { ...param.schema, type: e.target.value }
                      })}
                      className="select"
                      style={{ width: '100%' }}
                    >
                      {SCHEMA_TYPES.map(type => (
                        <option key={type} value={type}>{type.charAt(0).toUpperCase() + type.slice(1)}</option>
                      ))}
                    </select>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <label style={{ display: 'flex', alignItems: 'center', gap: '4px', cursor: 'pointer', fontSize: '14px', color: 'var(--text)' }}>
                      <input
                        type="checkbox"
                        checked={param.required}
                        onChange={(e) => updateParameter(index, { required: e.target.checked })}
                        style={{ borderRadius: '4px', border: '1px solid rgba(255,255,255,0.3)' }}
                      />
                      Required
                    </label>
                  </div>

                  <button
                    onClick={() => {
                      setSelectedParameter({ ...param, index });
                      setShowParameterDetails(true);
                    }}
                    style={{ padding: '8px', color: 'var(--muted)', background: 'transparent', border: '1px solid rgba(255,255,255,0.15)', borderRadius: '6px', cursor: 'pointer', transition: 'all 0.2s' }}
                    onMouseEnter={(e) => {
                      e.target.style.background = 'rgba(255,255,255,0.05)';
                      e.target.style.color = 'var(--text)';
                    }}
                    onMouseLeave={(e) => {
                      e.target.style.background = 'transparent';
                      e.target.style.color = 'var(--muted)';
                    }}
                    title="Configure parameter details"
                  >
                    <Settings style={{ width: '16px', height: '16px' }} />
                  </button>
                  
                  <button
                    onClick={() => removeParameter(index)}
                    style={{ padding: '8px', color: '#ef4444', background: 'transparent', border: '1px solid rgba(239, 68, 68, 0.3)', borderRadius: '6px', cursor: 'pointer', transition: 'background-color 0.2s' }}
                    onMouseEnter={(e) => e.target.style.background = 'rgba(239, 68, 68, 0.1)'}
                    onMouseLeave={(e) => e.target.style.background = 'transparent'}
                    title="Remove parameter"
                  >
                    <Trash2 style={{ width: '16px', height: '16px' }} />
                  </button>
                </div>

                <div style={{ marginTop: '12px' }}>
                  <label style={{ display: 'block', fontSize: '14px', fontWeight: 500, color: 'var(--text)', marginBottom: '4px' }}>
                    Description
                  </label>
                  <textarea
                    value={param.description || ''}
                    onChange={(e) => updateParameter(index, { description: e.target.value })}
                    rows={2}
                    style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.15)', background: 'rgba(255,255,255,0.04)', color: 'var(--text)', resize: 'vertical' }}
                    placeholder="Parameter description for documentation"
                  />
                </div>
              </div>
            ))}

            {(!endpoint.parameters || endpoint.parameters.length === 0) && (
              <div style={{ textAlign: 'center', color: 'var(--muted)', padding: '32px 0' }}>
                <p style={{ marginBottom: '8px' }}>No parameters defined</p>
                <button
                  onClick={addParameter}
                  style={{ color: 'var(--primary)', background: 'transparent', border: 'none', fontWeight: 500, cursor: 'pointer' }}
                >
                  Add your first parameter
                </button>
              </div>
            )}
          </div>
        )}

        {activeTab === 'requestBody' && endpoint.method !== 'GET' && (
          <div style={{ display: 'grid', gap: '16px' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <h3 style={{ fontSize: '18px', fontWeight: 500, color: 'var(--text)' }}>Request Body</h3>
              <button
                onClick={() => {
                  const updatedRequestBody = {
                    ...endpoint.requestBody,
                    required: false,
                    content: {}
                  };
                  handleUpdate({ requestBody: null });
                }}
                style={{ color: '#ef4444', background: 'transparent', border: 'none', fontSize: '14px', cursor: 'pointer' }}
              >
                Remove Request Body
              </button>
            </div>

            {endpoint.requestBody ? (
              <>
                <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                  <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer' }}>
                    <input
                      type="checkbox"
                      checked={endpoint.requestBody?.required || false}
                      onChange={(e) => {
                        const updatedRequestBody = {
                          ...endpoint.requestBody,
                          required: e.target.checked
                        };
                        handleUpdate({ requestBody: updatedRequestBody });
                      }}
                      style={{ borderRadius: '4px', border: '1px solid rgba(255,255,255,0.3)' }}
                    />
                    <span style={{ fontSize: '14px', color: 'var(--text)' }}>Required</span>
                  </label>
                </div>

                <div style={{ maxHeight: '60vh', overflowY: 'auto' }}>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '12px' }}>
                    <label style={{ fontSize: '14px', fontWeight: 500, color: 'var(--text)' }}>Content Types</label>
                    <select
                      onChange={(e) => {
                        if (e.target.value) {
                          addRequestBodyContent(e.target.value);
                          e.target.value = '';
                        }
                      }}
                      className="select"
                      style={{ minWidth: '200px' }}
                    >
                      <option value="">+ Add Content Type</option>
                      <option value="application/json">application/json</option>
                      <option value="application/xml">application/xml</option>
                      <option value="application/x-www-form-urlencoded">application/x-www-form-urlencoded</option>
                      <option value="multipart/form-data">multipart/form-data</option>
                      <option value="text/plain">text/plain</option>
                    </select>
                  </div>

                  {Object.entries(endpoint.requestBody?.content || {}).map(([contentType, content]) => (
                    <div key={contentType} style={{ border: '1px solid rgba(255,255,255,0.08)', borderRadius: '8px', marginBottom: '16px', overflow: 'hidden' }}>
                      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '12px 16px', background: 'rgba(255,255,255,0.02)', borderBottom: '1px solid rgba(255,255,255,0.08)' }}>
                        <span style={{ fontSize: '14px', fontFamily: 'monospace', color: 'var(--text)' }}>{contentType}</span>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <span style={{ fontSize: '12px', color: 'var(--muted)' }}>Schema Type:</span>
                            <select
                              value={content.schema?.type || 'object'}
                              onChange={(e) => {
                                const updatedSchema = e.target.value === 'object' 
                                  ? { type: 'object', properties: {} }
                                  : { type: e.target.value };
                                updateRequestBodyContent(contentType, { schema: updatedSchema });
                              }}
                              className="select"
                              style={{ fontSize: '12px', padding: '4px 8px' }}
                            >
                              <option value="object">Inline Schema</option>
                              <option value="string">Reference Schema</option>
                            </select>
                          </div>
                          <button
                            onClick={() => removeRequestBodyContent(contentType)}
                            style={{ padding: '4px', color: '#ef4444', background: 'transparent', border: 'none', cursor: 'pointer' }}
                          >
                            <Trash2 style={{ width: '14px', height: '14px' }} />
                          </button>
                        </div>
                      </div>

                      <div style={{ padding: '16px' }}>
                        {content.schema?.type === 'object' ? (
                          <div style={{ display: 'grid', gap: '16px' }}>
                            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                              <h4 style={{ fontSize: '16px', fontWeight: 500, color: 'var(--text)' }}>Properties</h4>
                              <button
                                onClick={() => addSchemaProperty(contentType)}
                                className="btn"
                                style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--primary)', fontSize: '14px', padding: '6px 12px' }}
                              >
                                <Plus style={{ width: '14px', height: '14px' }} />
                                Add Property
                              </button>
                            </div>

                            {Object.entries(content.schema?.properties || {}).map(([propertyName, propertySchema]) => (
                              <div key={propertyName} style={{ border: '1px solid rgba(255,255,255,0.08)', borderRadius: '6px', padding: '12px', background: 'rgba(255,255,255,0.02)' }}>
                                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr auto', gap: '12px', alignItems: 'center' }}>
                                  <input
                                    type="text"
                                    value={propertyName}
                                    onChange={(e) => updateSchemaProperty(contentType, propertyName, e.target.value, propertySchema)}
                                    style={{ padding: '8px 12px', borderRadius: '6px', border: '1px solid rgba(255,255,255,0.15)', background: 'rgba(255,255,255,0.04)', color: 'var(--text)' }}
                                    placeholder="Property name"
                                  />
                                  <select
                                    value={propertySchema.type || 'string'}
                                    onChange={(e) => updateSchemaProperty(contentType, propertyName, propertyName, { ...propertySchema, type: e.target.value })}
                                    className="select"
                                    style={{ width: '100%' }}
                                  >
                                    <option value="string">String</option>
                                    <option value="number">Number</option>
                                    <option value="integer">Integer</option>
                                    <option value="boolean">Boolean</option>
                                    <option value="array">Array</option>
                                    <option value="object">Object</option>
                                  </select>
                                  <button
                                    onClick={() => removeSchemaProperty(contentType, propertyName)}
                                    style={{ padding: '6px', color: '#ef4444', background: 'transparent', border: 'none', cursor: 'pointer' }}
                                  >
                                    <Trash2 style={{ width: '14px', height: '14px' }} />
                                  </button>
                                </div>
                                <textarea
                                  value={propertySchema.description || ''}
                                  onChange={(e) => updateSchemaProperty(contentType, propertyName, propertyName, { ...propertySchema, description: e.target.value })}
                                  rows={2}
                                  style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid rgba(255,255,255,0.15)', background: 'rgba(255,255,255,0.04)', color: 'var(--text)', marginTop: '8px', resize: 'vertical' }}
                                  placeholder="Property description"
                                />
                              </div>
                            ))}

                            {Object.keys(content.schema?.properties || {}).length === 0 && (
                              <div style={{ textAlign: 'center', color: 'var(--muted)', padding: '24px 0' }}>
                                <p style={{ marginBottom: '8px' }}>No properties defined</p>
                                <button
                                  onClick={() => addSchemaProperty(contentType)}
                                  style={{ color: 'var(--primary)', background: 'transparent', border: 'none', fontWeight: 500, cursor: 'pointer' }}
                                >
                                  Add your first property
                                </button>
                              </div>
                            )}
                          </div>
                        ) : (
                          <div>
                            <label style={{ display: 'block', fontSize: '14px', fontWeight: 500, color: 'var(--text)', marginBottom: '8px' }}>
                              Schema Reference
                            </label>
                            <select
                              value={content.schema?.$ref || ''}
                              onChange={(e) => updateRequestBodyContent(contentType, { schema: { $ref: e.target.value } })}
                              className="select"
                              style={{ width: '100%' }}
                            >
                              <option value="">Select a schema</option>
                              {(project?.schemas || []).map(schema => (
                                <option key={schema.id} value={`#/components/schemas/${schema.name}`}>
                                  {schema.name}
                                </option>
                              ))}
                            </select>
                          </div>
                        )}

                        <div style={{ marginTop: '16px' }}>
                          <label style={{ display: 'block', fontSize: '14px', fontWeight: 500, color: 'var(--text)', marginBottom: '8px' }}>
                            Example
                          </label>
                          <textarea
                            value={content.example || ''}
                            onChange={(e) => updateRequestBodyContent(contentType, { example: e.target.value })}
                            rows={4}
                            style={{ width: '100%', padding: '10px 12px', borderRadius: '6px', border: '1px solid rgba(255,255,255,0.15)', background: 'rgba(255,255,255,0.04)', color: 'var(--text)', fontFamily: 'monospace', fontSize: '12px', resize: 'vertical' }}
                            placeholder={`Example ${contentType} request body`}
                          />
                        </div>
                      </div>
                    </div>
                  ))}

                  {Object.keys(endpoint.requestBody?.content || {}).length === 0 && (
                    <div style={{ textAlign: 'center', color: 'var(--muted)', padding: '32px 0', border: '1px solid rgba(255,255,255,0.08)', borderRadius: '8px' }}>
                      <p style={{ marginBottom: '8px' }}>No content types defined</p>
                      <p style={{ fontSize: '14px' }}>Add a content type to define the request body schema</p>
                    </div>
                  )}
                </div>
              </>
            ) : (
              <div style={{ textAlign: 'center', color: 'var(--muted)', padding: '32px 0', border: '1px solid rgba(255,255,255,0.08)', borderRadius: '8px' }}>
                <p style={{ marginBottom: '8px' }}>No request body defined</p>
                <button
                  onClick={() => {
                    const newRequestBody = {
                      required: false,
                      content: {
                        'application/json': {
                          schema: { type: 'object', properties: {} },
                          example: '{}'
                        }
                      }
                    };
                    handleUpdate({ requestBody: newRequestBody });
                  }}
                  style={{ color: 'var(--primary)', background: 'transparent', border: 'none', fontWeight: 500, cursor: 'pointer' }}
                >
                  Add request body
                </button>
              </div>
            )}
          </div>
        )}

        {activeTab === 'responses' && (
          <div style={{ display: 'grid', gap: '16px' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <h3 style={{ fontSize: '18px', fontWeight: 500, color: 'var(--text)' }}>Responses</h3>
              <select
                onChange={(e) => {
                  if (e.target.value) {
                    const newResponse = {
                      description: e.target.value === '200' ? 'OK' : e.target.value === '201' ? 'Created' : e.target.value === '400' ? 'Bad Request' : e.target.value === '401' ? 'Unauthorized' : e.target.value === '403' ? 'Forbidden' : e.target.value === '404' ? 'Not Found' : e.target.value === '500' ? 'Internal Server Error' : 'Response',
                      headers: {},
                      content: {}
                    };
                    const updatedResponses = {
                      ...endpoint.responses,
                      [e.target.value]: newResponse
                    };
                    handleUpdate({ responses: updatedResponses });
                    e.target.value = '';
                  }
                }}
                className="select"
                style={{ minWidth: '120px' }}
              >
                <option value="">+ Add</option>
                <option value="200">200: OK</option>
                <option value="201">201: Created</option>
                <option value="400">400: Bad Request</option>
                <option value="401">401: Unauthorized</option>
                <option value="403">403: Forbidden</option>
                <option value="404">404: Not Found</option>
                <option value="500">500: Server Error</option>
              </select>
            </div>

            {Object.entries(endpoint.responses || {}).map(([status, response]) => (
              <div key={status} style={{ border: '1px solid rgba(255,255,255,0.08)', borderRadius: '8px', background: 'rgba(255,255,255,0.02)' }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '12px 16px', borderBottom: '1px solid rgba(255,255,255,0.08)' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                    <span className={`badge ${
                      status.startsWith('2') ? 'success' :
                      status.startsWith('4') ? 'danger' :
                      'warning'
                    }`}>
                      {status}
                    </span>
                    <span style={{ fontSize: '14px', color: 'var(--text)' }}>Response</span>
                  </div>
                  <button
                    onClick={() => {
                      const updatedResponses = { ...endpoint.responses };
                      delete updatedResponses[status];
                      handleUpdate({ responses: updatedResponses });
                    }}
                    style={{ padding: '4px', color: '#ef4444', background: 'transparent', border: 'none', borderRadius: '4px', cursor: 'pointer' }}
                  >
                    <Trash2 style={{ width: '16px', height: '16px' }} />
                  </button>
                </div>
                
                <div style={{ padding: '16px' }}>
                  <div style={{ marginBottom: '16px' }}>
                    <label style={{ display: 'block', fontSize: '14px', fontWeight: 500, color: 'var(--text)', marginBottom: '4px' }}>
                      Description
                    </label>
                    <textarea
                      value={response.description || ''}
                      onChange={(e) => {
                        const updatedResponses = {
                          ...endpoint.responses,
                          [status]: { ...response, description: e.target.value }
                        };
                        handleUpdate({ responses: updatedResponses });
                      }}
                      rows={2}
                      style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.15)', background: 'rgba(255,255,255,0.04)', color: 'var(--text)', resize: 'vertical' }}
                      placeholder="Response description"
                    />
                  </div>

                  <div style={{ marginBottom: '16px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
                      <label style={{ fontSize: '14px', fontWeight: 500, color: 'var(--text)' }}>Headers</label>
                      <button
                        onClick={() => {
                          const newHeaderName = `header${Object.keys(response.headers || {}).length + 1}`;
                          const updatedResponses = {
                            ...endpoint.responses,
                            [status]: {
                              ...response,
                              headers: {
                                ...(response.headers || {}),
                                [newHeaderName]: { description: '', schema: { type: 'string' } }
                              }
                            }
                          };
                          handleUpdate({ responses: updatedResponses });
                        }}
                        className="btn"
                        style={{ fontSize: '12px', padding: '4px 8px', color: 'var(--primary)' }}
                      >
                        + Add
                      </button>
                    </div>
                    
                    {Object.keys(response.headers || {}).length === 0 ? (
                      <div style={{ padding: '12px', border: '1px solid rgba(255,255,255,0.08)', borderRadius: '6px', textAlign: 'center', color: 'var(--muted)', fontSize: '14px' }}>
                        No headers defined.
                      </div>
                    ) : (
                      <div style={{ display: 'grid', gap: '8px' }}>
                        {Object.entries(response.headers || {}).map(([headerName, header]) => (
                          <div key={headerName} style={{ display: 'grid', gridTemplateColumns: '1fr 2fr auto', gap: '8px', alignItems: 'center', padding: '8px', border: '1px solid rgba(255,255,255,0.08)', borderRadius: '6px' }}>
                            <input
                              type="text"
                              value={headerName}
                              onChange={(e) => {
                                const updatedHeaders = { ...(response.headers || {}) };
                                delete updatedHeaders[headerName];
                                updatedHeaders[e.target.value] = header;
                                const updatedResponses = {
                                  ...endpoint.responses,
                                  [status]: { ...response, headers: updatedHeaders }
                                };
                                handleUpdate({ responses: updatedResponses });
                              }}
                              style={{ padding: '6px 8px', borderRadius: '4px', border: '1px solid rgba(255,255,255,0.15)', background: 'rgba(255,255,255,0.04)', color: 'var(--text)', fontSize: '12px' }}
                              placeholder="Header name"
                            />
                            <input
                              type="text"
                              value={header.description || ''}
                              onChange={(e) => {
                                const updatedResponses = {
                                  ...endpoint.responses,
                                  [status]: {
                                    ...response,
                                    headers: {
                                      ...(response.headers || {}),
                                      [headerName]: { ...header, description: e.target.value }
                                    }
                                  }
                                };
                                handleUpdate({ responses: updatedResponses });
                              }}
                              style={{ padding: '6px 8px', borderRadius: '4px', border: '1px solid rgba(255,255,255,0.15)', background: 'rgba(255,255,255,0.04)', color: 'var(--text)', fontSize: '12px' }}
                              placeholder="Description"
                            />
                            <button
                              onClick={() => {
                                const updatedHeaders = { ...(response.headers || {}) };
                                delete updatedHeaders[headerName];
                                const updatedResponses = {
                                  ...endpoint.responses,
                                  [status]: { ...response, headers: updatedHeaders }
                                };
                                handleUpdate({ responses: updatedResponses });
                              }}
                              style={{ padding: '2px', color: '#ef4444', background: 'transparent', border: 'none', cursor: 'pointer' }}
                            >
                              <Trash2 style={{ width: '12px', height: '12px' }} />
                            </button>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>

                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
                      <label style={{ fontSize: '14px', fontWeight: 500, color: 'var(--text)' }}>Body</label>
                      <select
                        onChange={(e) => {
                          if (e.target.value) {
                            const updatedResponses = {
                              ...endpoint.responses,
                              [status]: {
                                ...response,
                                content: {
                                  ...(response.content || {}),
                                  [e.target.value]: {
                                    schema: { type: 'object' },
                                    example: e.target.value === 'application/json' ? '{}' : ''
                                  }
                                }
                              }
                            };
                            handleUpdate({ responses: updatedResponses });
                            e.target.value = '';
                          }
                        }}
                        className="select"
                        style={{ fontSize: '12px', padding: '4px 8px' }}
                      >
                        <option value="">+ Add</option>
                        <option value="application/json">application/json</option>
                        <option value="application/xml">application/xml</option>
                        <option value="text/plain">text/plain</option>
                        <option value="text/html">text/html</option>
                      </select>
                    </div>
                    
                    {Object.keys(response.content || {}).length === 0 ? (
                      <div style={{ padding: '12px', border: '1px solid rgba(255,255,255,0.08)', borderRadius: '6px', textAlign: 'center', color: 'var(--muted)', fontSize: '14px' }}>
                        No body content defined.
                      </div>
                    ) : (
                      <div style={{ display: 'grid', gap: '8px' }}>
                        {Object.entries(response.content || {}).map(([contentType, content]) => (
                          <div key={contentType} style={{ border: '1px solid rgba(255,255,255,0.08)', borderRadius: '6px', overflow: 'hidden' }}>
                            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '8px 12px', background: 'rgba(255,255,255,0.02)', borderBottom: '1px solid rgba(255,255,255,0.08)' }}>
                              <span style={{ fontSize: '12px', fontFamily: 'monospace', color: 'var(--text)' }}>{contentType}</span>
                              <button
                                onClick={() => {
                                  const updatedContent = { ...(response.content || {}) };
                                  delete updatedContent[contentType];
                                  const updatedResponses = {
                                    ...endpoint.responses,
                                    [status]: { ...response, content: updatedContent }
                                  };
                                  handleUpdate({ responses: updatedResponses });
                                }}
                                style={{ padding: '2px', color: '#ef4444', background: 'transparent', border: 'none', cursor: 'pointer' }}
                              >
                                <Trash2 style={{ width: '12px', height: '12px' }} />
                              </button>
                            </div>
                            <div style={{ padding: '12px', maxHeight: '300px', overflow: 'auto' }}>
                              <textarea
                                value={content.example || ''}
                                onChange={(e) => {
                                  const updatedResponses = {
                                    ...endpoint.responses,
                                    [status]: {
                                      ...response,
                                      content: {
                                        ...(response.content || {}),
                                        [contentType]: { ...content, example: e.target.value }
                                      }
                                    }
                                  };
                                  handleUpdate({ responses: updatedResponses });
                                }}
                                rows={6}
                                style={{ width: '100%', padding: '8px', borderRadius: '4px', border: '1px solid rgba(255,255,255,0.15)', background: 'rgba(255,255,255,0.04)', color: 'var(--text)', fontFamily: 'monospace', fontSize: '12px', resize: 'vertical', minHeight: '120px' }}
                                placeholder={`Example ${contentType} response body`}
                              />
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              </div>
            ))}

            {Object.keys(endpoint.responses || {}).length === 0 && (
              <div style={{ textAlign: 'center', color: 'var(--muted)', padding: '32px 0' }}>
                <p style={{ marginBottom: '8px' }}>No responses defined</p>
                <p style={{ fontSize: '14px' }}>Add response codes to document your API behavior</p>
              </div>
            )}
          </div>
        )}
      </div>

      {showParameterDetails && selectedParameter && (
        <ParameterDetails
          parameter={selectedParameter}
          onUpdate={(updates) => {
            updateParameter(selectedParameter.index, updates);
            setShowParameterDetails(false);
            setSelectedParameter(null);
          }}
          onClose={() => {
            setShowParameterDetails(false);
            setSelectedParameter(null);
          }}
        />
      )}
    </div>
  );
}