import api from './api.js';
const API_REGISTRY_SERVICE_PREFIX = '/cdb-api-registry'

export const apiRegistryService = {
  // Get paginated list of APIs
  getApis: async (page = 0, size = 10, search = '', status = '') => {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    
    if (search && search.trim()) {
      params.append('search', search.trim());
    }
    if (status && status.trim()) {
      params.append('status', status.trim());
    }
    
    const response = await api.get(`${API_REGISTRY_SERVICE_PREFIX}/api/v1/registry/apis?${params}`);
    return response.data;
  },

  // Get API details
  getApiDetail: async (apiId) => {
    const response = await api.get(`${API_REGISTRY_SERVICE_PREFIX}/api/v1/registry/apis/${apiId}`);
    return response.data;
  },

  // Create new API
  createApi: async (apiData) => {
    const response = await api.post(`${API_REGISTRY_SERVICE_PREFIX}/api/v1/registry/apis`, apiData);
    return response.data;
  },

  // Update API
  updateApi: async (apiId, apiData) => {
    const response = await api.put(`${API_REGISTRY_SERVICE_PREFIX}/api/v1/registry/apis/${apiId}`, apiData);
    return response.data;
  },

  // Update API status
  updateStatus: async (apiId, status) => {
    const response = await api.patch(`${API_REGISTRY_SERVICE_PREFIX}/api/v1/registry/apis/${apiId}/status?status=${status}`);
    return response.data;
  },

  // Rollback to version
  rollbackToVersion: async (apiId, version) => {
    const response = await api.post(`${API_REGISTRY_SERVICE_PREFIX}/api/v1/registry/apis/${apiId}/rollback/${version}`);
    return response.data;
  },

  // Search tags for autocomplete
  searchTags: async (query) => {
    const response = await api.get(`${API_REGISTRY_SERVICE_PREFIX}/api/v1/registry/apis/tags/search?query=${encodeURIComponent(query)}`);
    return response.data;
  },

  // Search domains for autocomplete
  searchDomains: async (query) => {
    const response = await api.get(`${API_REGISTRY_SERVICE_PREFIX}/api/v1/registry/apis/domains/search?query=${encodeURIComponent(query)}`);
    return response.data;
  }
};