import React, { useState } from 'react';

export default function ParameterDetails({ parameter, onUpdate, onClose }) {
  const [formData, setFormData] = useState({
    description: parameter.description || '',
    required: parameter.required || false,
    type: parameter.schema?.type || 'string',
    format: parameter.schema?.format || '',
    minimum: parameter.schema?.minimum || '',
    maximum: parameter.schema?.maximum || '',
    minLength: parameter.schema?.minLength || '',
    maxLength: parameter.schema?.maxLength || '',
    pattern: parameter.schema?.pattern || '',
    enum: parameter.schema?.enum ? parameter.schema.enum.join(', ') : '',
    default: parameter.schema?.default || '',
    example: parameter.example || '',
    deprecated: parameter.deprecated || false,
    multipleOf: parameter.schema?.multipleOf || '',
    exclusiveMinimum: parameter.schema?.exclusiveMinimum || false,
    exclusiveMaximum: parameter.schema?.exclusiveMaximum || false
  });

  const handleChange = (field, value) => {
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  const handleSave = () => {
    const schemaUpdates = {
      type: formData.type,
      format: formData.format || undefined,
      minimum: formData.minimum ? parseFloat(formData.minimum) : undefined,
      maximum: formData.maximum ? parseFloat(formData.maximum) : undefined,
      minLength: formData.minLength ? parseInt(formData.minLength) : undefined,
      maxLength: formData.maxLength ? parseInt(formData.maxLength) : undefined,
      pattern: formData.pattern || undefined,
      enum: formData.enum ? formData.enum.split(',').map(v => v.trim()).filter(v => v) : undefined,
      default: formData.default || undefined,
      multipleOf: formData.multipleOf ? parseFloat(formData.multipleOf) : undefined,
      exclusiveMinimum: formData.exclusiveMinimum || undefined,
      exclusiveMaximum: formData.exclusiveMaximum || undefined
    };
    
    // Remove undefined values
    Object.keys(schemaUpdates).forEach(key => {
      if (schemaUpdates[key] === undefined) {
        delete schemaUpdates[key];
      }
    });
    
    const updates = {
      description: formData.description,
      required: formData.required,
      deprecated: formData.deprecated,
      example: formData.example || undefined,
      schema: { ...parameter.schema, ...schemaUpdates }
    };
    
    onUpdate(updates);
    onClose();
  };

  const getFormatOptions = (type) => {
    switch (type) {
      case 'string':
        return ['', 'date-time', 'date', 'time', 'email', 'hostname', 'ipv4', 'ipv6', 'uri', 'uuid'];
      case 'number':
        return ['', 'float', 'double'];
      case 'integer':
        return ['', 'int32', 'int64'];
      default:
        return [''];
    }
  };

  return (
    <div className="modal-overlay" style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.3)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
      <div className="modal-content" style={{ background: 'var(--card)', borderRadius: '8px', padding: '20px', width: 'min(384px, 90vw)', maxHeight: '80.5vh', overflowY: 'auto', border: '1px solid rgba(255,255,255,0.1)' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
          <h3 style={{ fontSize: '16px', fontWeight: 600, color: 'var(--text)' }}>Parameter Details</h3>
          <button onClick={onClose} style={{ background: 'none', border: 'none', fontSize: '18px', cursor: 'pointer', color: 'var(--muted)' }}>×</button>
        </div>

        <div style={{ display: 'grid', gap: '12px' }}>
          <div>
            <label style={{ display: 'block', fontSize: '14px', fontWeight: 500, color: 'var(--text)', marginBottom: '8px' }}>
              Description
            </label>
            <textarea
              value={formData.description}
              onChange={(e) => handleChange('description', e.target.value)}
              placeholder="Describe this parameter"
              rows={3}
              style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.15)', background: 'rgba(255,255,255,0.04)', color: 'var(--text)', resize: 'vertical' }}
            />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
            <div>
              <label style={{ display: 'block', fontSize: '14px', fontWeight: 500, color: 'var(--text)', marginBottom: '8px' }}>
                Type
              </label>
              <select
                value={formData.type}
                onChange={(e) => handleChange('type', e.target.value)}
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
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '14px', fontWeight: 500, color: 'var(--text)', marginBottom: '8px' }}>
                Format
              </label>
              <select
                value={formData.format}
                onChange={(e) => handleChange('format', e.target.value)}
                className="select"
                style={{ width: '100%' }}
              >
                {getFormatOptions(formData.type).map(format => (
                  <option key={format} value={format}>{format || 'None'}</option>
                ))}
              </select>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <label style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--text)', fontSize: '14px' }}>
              <input
                type="checkbox"
                checked={formData.required}
                onChange={(e) => handleChange('required', e.target.checked)}
                style={{ borderRadius: '4px' }}
              />
              Required
            </label>
            <label style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--text)', fontSize: '14px' }}>
              <input
                type="checkbox"
                checked={formData.deprecated}
                onChange={(e) => handleChange('deprecated', e.target.checked)}
                style={{ borderRadius: '4px' }}
              />
              Deprecated
            </label>
          </div>

          {(formData.type === 'string') && (
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '14px', fontWeight: 500, color: 'var(--text)', marginBottom: '8px' }}>
                  Min Length
                </label>
                <input
                  type="number"
                  value={formData.minLength}
                  onChange={(e) => handleChange('minLength', e.target.value)}
                  style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.15)', background: 'rgba(255,255,255,0.04)', color: 'var(--text)' }}
                />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '14px', fontWeight: 500, color: 'var(--text)', marginBottom: '8px' }}>
                  Max Length
                </label>
                <input
                  type="number"
                  value={formData.maxLength}
                  onChange={(e) => handleChange('maxLength', e.target.value)}
                  style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.15)', background: 'rgba(255,255,255,0.04)', color: 'var(--text)' }}
                />
              </div>
            </div>
          )}

          {(formData.type === 'number' || formData.type === 'integer') && (
            <>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '14px', fontWeight: 500, color: 'var(--text)', marginBottom: '8px' }}>
                    Minimum
                  </label>
                  <input
                    type="number"
                    value={formData.minimum}
                    onChange={(e) => handleChange('minimum', e.target.value)}
                    style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.15)', background: 'rgba(255,255,255,0.04)', color: 'var(--text)' }}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '14px', fontWeight: 500, color: 'var(--text)', marginBottom: '8px' }}>
                    Maximum
                  </label>
                  <input
                    type="number"
                    value={formData.maximum}
                    onChange={(e) => handleChange('maximum', e.target.value)}
                    style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.15)', background: 'rgba(255,255,255,0.04)', color: 'var(--text)' }}
                  />
                </div>
              </div>
              
              <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--text)', fontSize: '14px' }}>
                  <input
                    type="checkbox"
                    checked={formData.exclusiveMinimum}
                    onChange={(e) => handleChange('exclusiveMinimum', e.target.checked)}
                    style={{ borderRadius: '4px' }}
                  />
                  Exclusive Min
                </label>
                <label style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--text)', fontSize: '14px' }}>
                  <input
                    type="checkbox"
                    checked={formData.exclusiveMaximum}
                    onChange={(e) => handleChange('exclusiveMaximum', e.target.checked)}
                    style={{ borderRadius: '4px' }}
                  />
                  Exclusive Max
                </label>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '14px', fontWeight: 500, color: 'var(--text)', marginBottom: '8px' }}>
                  Multiple Of
                </label>
                <input
                  type="number"
                  value={formData.multipleOf}
                  onChange={(e) => handleChange('multipleOf', e.target.value)}
                  style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.15)', background: 'rgba(255,255,255,0.04)', color: 'var(--text)' }}
                />
              </div>
            </>
          )}

          {formData.type === 'string' && (
            <div>
              <label style={{ display: 'block', fontSize: '14px', fontWeight: 500, color: 'var(--text)', marginBottom: '8px' }}>
                Pattern (Regex)
              </label>
              <input
                type="text"
                value={formData.pattern}
                onChange={(e) => handleChange('pattern', e.target.value)}
                placeholder="^[a-zA-Z0-9]+$"
                style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.15)', background: 'rgba(255,255,255,0.04)', color: 'var(--text)' }}
              />
            </div>
          )}

          <div>
            <label style={{ display: 'block', fontSize: '14px', fontWeight: 500, color: 'var(--text)', marginBottom: '8px' }}>
              Enum Values (comma-separated)
            </label>
            <input
              type="text"
              value={formData.enum}
              onChange={(e) => handleChange('enum', e.target.value)}
              placeholder="value1, value2, value3"
              style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.15)', background: 'rgba(255,255,255,0.04)', color: 'var(--text)' }}
            />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
            <div>
              <label style={{ display: 'block', fontSize: '14px', fontWeight: 500, color: 'var(--text)', marginBottom: '8px' }}>
                Default Value
              </label>
              <input
                type="text"
                value={formData.default}
                onChange={(e) => handleChange('default', e.target.value)}
                style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.15)', background: 'rgba(255,255,255,0.04)', color: 'var(--text)' }}
              />
            </div>
            <div>
              <label style={{ display: 'block', fontSize: '14px', fontWeight: 500, color: 'var(--text)', marginBottom: '8px' }}>
                Example
              </label>
              <input
                type="text"
                value={formData.example}
                onChange={(e) => handleChange('example', e.target.value)}
                style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.15)', background: 'rgba(255,255,255,0.04)', color: 'var(--text)' }}
              />
            </div>
          </div>
        </div>

        <div style={{ display: 'flex', gap: '8px', marginTop: '16px', justifyContent: 'flex-end' }}>
          <button onClick={onClose} className="btn" style={{ padding: '8px 16px', fontSize: '14px' }}>
            Cancel
          </button>
          <button onClick={handleSave} className="btn primary" style={{ padding: '8px 16px', fontSize: '14px' }}>
            Save Changes
          </button>
        </div>
      </div>
    </div>
  );
}