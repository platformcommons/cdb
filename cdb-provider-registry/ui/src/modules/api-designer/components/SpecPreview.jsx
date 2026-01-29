import React, { useState } from 'react';
import { Copy, Download, FileText, Code } from 'lucide-react';
import { useProject } from '../context';

export function SpecPreview() {
  const { project, exportSpec, updateSpec } = useProject();
  const [format, setFormat] = useState('yaml');
  const [copied, setCopied] = useState(false);
  const [editedSpec, setEditedSpec] = useState('');

  const spec = exportSpec(format);

  React.useEffect(() => {
    setEditedSpec(spec);
  }, [spec]);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(spec);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (error) {
      console.error('Failed to copy:', error);
    }
  };

  const handleDownload = () => {
    const blob = new Blob([spec], { type: format === 'yaml' ? 'text/yaml' : 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${project.name}.${format}`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  return (
    <div style={{ height: '100%', background: 'var(--card)', display: 'flex', flexDirection: 'column' }}>
      <div style={{ padding: '24px', borderBottom: '1px solid rgba(255,255,255,0.08)' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
          <h2 style={{ fontSize: '20px', fontWeight: 600, color: 'var(--text)' }}>OpenAPI Specification</h2>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <div style={{ display: 'flex', background: 'rgba(255,255,255,0.04)', borderRadius: '8px', padding: '4px' }}>
              <button
                onClick={() => setFormat('yaml')}
                className="chip"
                style={{
                  ...(format === 'yaml'
                    ? { background: 'var(--primary)', borderColor: 'var(--primary)', color: 'white' }
                    : { color: 'var(--muted)' })
                }}
              >
                YAML
              </button>
              <button
                onClick={() => setFormat('json')}
                className="chip"
                style={{
                  ...(format === 'json'
                    ? { background: 'var(--primary)', borderColor: 'var(--primary)', color: 'white' }
                    : { color: 'var(--muted)' })
                }}
              >
                JSON
              </button>
            </div>
            
            <button
              onClick={handleCopy}
              className="btn"
              style={{ display: 'flex', alignItems: 'center', gap: '8px' }}
            >
              <Copy style={{ width: '16px', height: '16px' }} />
              {copied ? 'Copied!' : 'Copy'}
            </button>
            
            <button
              onClick={handleDownload}
              className="btn primary"
              style={{ display: 'flex', alignItems: 'center', gap: '8px' }}
            >
              <Download style={{ width: '16px', height: '16px' }} />
              Download
            </button>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '16px', fontSize: '14px', color: 'var(--muted)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <FileText style={{ width: '16px', height: '16px' }} />
            <span>OpenAPI 3.1.1</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Code style={{ width: '16px', height: '16px' }} />
            <span>{project.endpoints.length} endpoints</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <FileText style={{ width: '16px', height: '16px' }} />
            <span>{project.schemas.length} schemas</span>
          </div>
        </div>
      </div>

      <div style={{ flex: 1, overflow: 'hidden' }}>
        <div style={{ height: '100%', padding: '24px' }}>
          <div style={{ height: '100%', border: '1px solid rgba(255,255,255,0.08)', borderRadius: '8px', overflow: 'hidden' }}>
            <textarea
              value={editedSpec}
              onChange={(e) => {
                setEditedSpec(e.target.value);
              }}
              onBlur={() => {
                updateSpec(editedSpec, format);
              }}
              style={{
                height: '100%',
                width: '100%',
                padding: '16px',
                background: 'rgba(255,255,255,0.02)',
                border: 'none',
                outline: 'none',
                fontSize: '14px',
                fontFamily: 'monospace',
                color: 'var(--text)',
                lineHeight: 1.5,
                margin: 0,
                resize: 'none'
              }}
            />
          </div>
        </div>
      </div>
    </div>
  );
}