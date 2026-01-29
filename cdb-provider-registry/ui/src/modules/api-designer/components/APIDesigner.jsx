import React, { useState } from 'react';
import { EndpointList } from './EndpointList';
import { EndpointEditor } from './EndpointEditor';
import { useProject } from '../context';

export function APIDesigner() {
  const { project } = useProject();
  const [selectedEndpointId, setSelectedEndpointId] = useState(null);

  const selectedEndpoint = selectedEndpointId 
    ? project.endpoints.find(e => e.id === selectedEndpointId)
    : null;

  return (
    <div className="flex h-full min-h-0 overflow-hidden">
      <EndpointList
        selectedId={selectedEndpointId}
        onSelect={setSelectedEndpointId}
      />
      <div className="flex-1 min-h-0 overflow-y-auto">
        {selectedEndpoint ? (
          <EndpointEditor endpoint={selectedEndpoint} />
        ) : (
          <div style={{ height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center' }} className="muted">
            <div style={{ textAlign: 'center' }}>
              <h3 style={{ fontSize: '18px', fontWeight: 500, marginBottom: '8px' }}>Select an endpoint to edit</h3>
              <p>Choose an endpoint from the list or create a new one to get started</p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}