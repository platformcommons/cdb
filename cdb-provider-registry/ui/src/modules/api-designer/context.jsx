import React, { createContext, useContext, useState, useEffect } from 'react';

const ProjectContext = createContext();

export function ProjectProvider({ children }) {
  const [project, setProject] = useState({
    id: '',
    name: '',
    description: '',
    version: '1.0',
    contact: {
      name: '',
      email: '',
      url: ''
    },
    termsOfService: '',
    license: {
      name: '',
      url: ''
    },
    servers: [
      { url: '', description: 'SANDBOX' },
      { url: '', description: 'PROD' }
    ],
    endpoints: [],
    schemas: []
  });
  const [initialized, setInitialized] = useState(false);

  const updateProject = (updates) => {
    setProject(prev => ({ ...prev, ...updates }));
  };

  const addEndpoint = (endpoint) => {
    setProject(prev => ({
      ...prev,
      endpoints: [...prev.endpoints, endpoint]
    }));
  };

  const updateEndpoint = (id, updates) => {
    setProject(prev => ({
      ...prev,
      endpoints: prev.endpoints.map(e => e.id === id ? { ...e, ...updates } : e)
    }));
  };

  const removeEndpoint = (id) => {
    setProject(prev => ({
      ...prev,
      endpoints: prev.endpoints.filter(e => e.id !== id)
    }));
  };

  const addSchema = (schema) => {
    setProject(prev => ({
      ...prev,
      schemas: [...prev.schemas, schema]
    }));
  };

  const updateSchema = (id, updates) => {
    setProject(prev => ({
      ...prev,
      schemas: prev.schemas.map(s => s.id === id ? { ...s, ...updates } : s)
    }));
  };

  const removeSchema = (id) => {
    setProject(prev => ({
      ...prev,
      schemas: prev.schemas.filter(s => s.id !== id)
    }));
  };

  const exportSpec = (format = 'yaml') => {
    // Build a minimal but compliant OpenAPI 3.1.1 document from the project model
    const spec = {
      openapi: '3.1.1',
      jsonSchemaDialect: 'https://json-schema.org/draft/2020-12/schema',
      info: {
        title: project.name,
        description: project.description,
        version: project.version,
        contact: project.contact && (project.contact.name || project.contact.email || project.contact.url)
          ? {
              ...(project.contact.name ? { name: project.contact.name } : {}),
              ...(project.contact.email ? { email: project.contact.email } : {}),
              ...(project.contact.url ? { url: project.contact.url } : {}),
            }
          : undefined,
        termsOfService: project.termsOfService || undefined,
        license: project.license && (project.license.name || project.license.url)
          ? {
              ...(project.license.name ? { name: project.license.name } : {}),
              ...(project.license.url ? { url: project.license.url } : {}),
            }
          : undefined,
      },
      paths: project.endpoints.reduce((paths, endpoint) => {
        if (!paths[endpoint.path]) paths[endpoint.path] = {};

        // Normalize parameters array (ensure undefined when empty)
        const parameters = Array.isArray(endpoint.parameters) && endpoint.parameters.length > 0
          ? endpoint.parameters
          : undefined;

        // Normalize requestBody content structure for export
        const requestBody = endpoint.requestBody && endpoint.requestBody.content && Object.keys(endpoint.requestBody.content).length > 0
          ? endpoint.requestBody
          : undefined;

        paths[endpoint.path][endpoint.method.toLowerCase()] = {
          summary: endpoint.summary,
          description: endpoint.description,
          parameters,
          requestBody,
          responses: endpoint.responses && Object.keys(endpoint.responses).length > 0
            ? endpoint.responses
            : { '200': { description: 'Success' } }
        };
        return paths;
      }, {}),
      servers: Array.isArray(project.servers) && project.servers.length > 0
        ? project.servers.filter(s => s && s.url).map(s => ({ url: s.url, description: s.description || undefined }))
        : undefined,
      components: {
        schemas: project.schemas.reduce((schemas, schema) => {
          // Sanitize properties and build required list
          const cleanProperties = {};
          const requiredSet = new Set(Array.isArray(schema.required) ? schema.required : []);
          const props = schema.properties || {};
          Object.entries(props).forEach(([propName, propVal]) => {
            const base = propVal && typeof propVal === 'object' && propVal.schema ? { ...propVal, ...propVal.schema } : propVal || {};
            // Determine if property is marked as required on the property itself
            if (base && base.required === true) requiredSet.add(propName);
            // Build a clean property schema with allowed fields only
            const allowedKeys = [
              'type', 'format', 'description', 'example', 'deprecated',
              // numeric/string constraints that OpenAPI supports
              'minimum', 'maximum', 'exclusiveMinimum', 'exclusiveMaximum',
              'minLength', 'maxLength', 'pattern', 'enum', 'multipleOf',
              // array/object related
              'items', 'properties', '$ref', 'default'
            ];
            const clean = {};
            allowedKeys.forEach(k => {
              const v = base ? base[k] : undefined;
              const isEmptyString = typeof v === 'string' && v.trim() === '';
              if (v !== undefined && v !== null && !isEmptyString) {
                clean[k] = v;
              }
            });
            // Ensure type defaults when nested schema provided only there
            if (!clean.type && base && base.type) clean.type = base.type;
            if (Object.keys(clean).length === 0) {
              // default to string if nothing defined to avoid invalid empty schema
              clean.type = 'string';
            }
            cleanProperties[propName] = clean;
          });

          const requiredArr = Array.from(requiredSet);

          schemas[schema.name] = {
            type: schema.type || 'object',
            description: schema.description || undefined,
            properties: Object.keys(cleanProperties).length ? cleanProperties : undefined,
            required: requiredArr.length ? requiredArr : undefined,
            additionalProperties: schema.additionalProperties === false ? false : undefined
          };
          return schemas;
        }, {})
      }
    };

    const result = format === 'json' ? JSON.stringify(spec, null, 2) : convertToYAML(spec);

    // Store for global access
    if (typeof window !== 'undefined') {
      window.lastExportedYaml = result;
    }

    return result;
  };

  const convertToYAML = (obj, indent = 0) => {
    const spaces = '  '.repeat(indent);
    let yaml = '';
    for (const [key, value] of Object.entries(obj)) {
      if (value === null || value === undefined) continue;
      yaml += `${spaces}${key}:`;
      if (typeof value === 'object' && !Array.isArray(value)) {
        yaml += '\n' + convertToYAML(value, indent + 1);
      } else if (Array.isArray(value)) {
        yaml += value.length === 0 ? ' []\n' : '\n' + value.map(item => 
          typeof item === 'object' ? `${spaces}  - \n${convertToYAML(item, indent + 2)}` : `${spaces}  - ${item}\n`
        ).join('');
      } else {
        yaml += ` ${typeof value === 'string' ? `"${value}"` : value}\n`;
      }
    }
    return yaml;
  };

  // Parse initial YAML if provided
  useEffect(() => {
    if (!initialized && typeof window !== 'undefined' && window.initialApiYaml) {
      try {
        const parsedData = parseYamlToProject(window.initialApiYaml);
        if (parsedData) {
          setProject(parsedData);
        }
      } catch (e) {
        console.warn('Failed to parse initial YAML:', e);
      }
      setInitialized(true);
    }
  }, [initialized]);

  // Set up global export listener
  useEffect(() => {
    const handleExport = () => {
      exportSpec('yaml');
    };
    
    document.addEventListener('exportApiDesignerYaml', handleExport);
    return () => document.removeEventListener('exportApiDesignerYaml', handleExport);
  }, [project]);

  const parseYamlToProject = (yamlContent) => {
    try {
      // Simple YAML parser for basic OpenAPI structure
      const lines = yamlContent.split('\n');
      const parsed = { endpoints: [], schemas: [] };
      
      // Extract basic info
      const titleMatch = yamlContent.match(/title:\s*["']?([^"'\n]+)["']?/);
      const descMatch = yamlContent.match(/description:\s*["']?([^"'\n]+)["']?/);
      const versionMatch = yamlContent.match(/version:\s*["']?([^"'\n]+)["']?/);
      
      parsed.name = titleMatch ? titleMatch[1] : 'Imported API';
      parsed.description = descMatch ? descMatch[1] : 'Imported from YAML';
      parsed.version = versionMatch ? versionMatch[1] : '1.0.0';
      parsed.id = '1';
      
      // Extract paths (basic parsing)
      const pathsMatch = yamlContent.match(/paths:\s*\n([\s\S]*?)(?=\n\w|$)/);
      if (pathsMatch) {
        const pathsSection = pathsMatch[1];
        const pathRegex = /^\s{2}(\/[^:\n]+):/gm;
        let pathMatch;
        
        while ((pathMatch = pathRegex.exec(pathsSection)) !== null) {
          const path = pathMatch[1];
          const methodRegex = new RegExp(`\\s{4}(get|post|put|delete|patch):`, 'gi');
          let methodMatch;
          
          while ((methodMatch = methodRegex.exec(pathsSection)) !== null) {
            const method = methodMatch[1].toUpperCase();
            const summaryMatch = pathsSection.match(new RegExp(`${method.toLowerCase()}:[\\s\\S]*?summary:\\s*["']?([^"'\\n]+)["']?`, 'i'));
            
            parsed.endpoints.push({
              id: `${path}-${method}`.replace(/[^a-zA-Z0-9]/g, '-'),
              path,
              method,
              summary: summaryMatch ? summaryMatch[1] : `${method} ${path}`,
              description: '',
              parameters: [],
              responses: { '200': { description: 'Success' } }
            });
          }
        }
      }
      
      return parsed;
    } catch (e) {
      console.error('YAML parsing error:', e);
      return null;
    }
  };

  const updateSpec = (specContent, format = 'yaml') => {
    try {
      const parsedData = parseYamlToProject(specContent);
      if (parsedData) {
        setProject(parsedData);
      }
    } catch (e) {
      console.error('Failed to update spec:', e);
    }
  };

  return (
    <ProjectContext.Provider value={{
      project,
      updateProject,
      addEndpoint,
      updateEndpoint,
      removeEndpoint,
      addSchema,
      updateSchema,
      removeSchema,
      exportSpec,
      updateSpec,
      initialized
    }}>
      {children}
    </ProjectContext.Provider>
  );
}

export const useProject = () => {
  const context = useContext(ProjectContext);
  if (!context) throw new Error('useProject must be used within ProjectProvider');
  return context;
};