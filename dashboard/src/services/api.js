import axios from 'axios';

const API_BASE_URL = 'https://www.cicsamovil.com/api'; // Production server URL

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const authService = {
  login: async (email, password) => {
    try {
      const response = await api.post('/login.php', { celular: email, password });
      if (response.data.status === 'success') {
        const user = response.data;
        // Verify if the user is an admin
        if (user.rol.toLowerCase() !== 'admin') {
          throw new Error('Acceso denegado: Se requiere privilegios de administrador.');
        }
        localStorage.setItem('admin_user', JSON.stringify(user));
        return user;
      } else {
        throw new Error(response.data.message || 'Error de autenticación');
      }
    } catch (error) {
      console.error('Login error:', error);
      throw error;
    }
  },

  register: async (userData) => {
    try {
      const response = await api.post('/register.php', userData);
      return response.data;
    } catch (error) {
      console.error('Register error:', error);
      throw error;
    }
  },
  
  logout: () => {
    localStorage.removeItem('admin_user');
  },
  
  getCurrentUser: () => {
    const user = localStorage.getItem('admin_user');
    return user ? JSON.parse(user) : null;
  }
};

export const dataService = {
  getUsersStatus: async () => {
    try {
      const response = await api.get('/admin_get_all_users.php');
      return Array.isArray(response.data) ? response.data : [];
    } catch (error) {
      console.error("Error fetching admin users:", error);
      return [];
    }
  },
  
  getSolicitudes: async (params = {}) => {
    const response = await api.get('/get_solicitudes.php', { params });
    return response.data;
  },

  createSolicitud: async (solicitudData) => {
    try {
      const response = await api.post('/create_solicitud.php', solicitudData);
      return response.data;
    } catch (error) {
      console.error('Create Solicitud error:', error);
      throw error;
    }
  },

  updateSolicitudStatus: async (id, nuevoEstado, idUsuario = 0, comentario = null) => {
    try {
      const response = await api.post('/update_solicitud_status.php', {
        id,
        nuevo_estado: nuevoEstado,
        id_usuario: idUsuario,
        comentario
      });
      return response.data;
    } catch (error) {
      console.error('Update Solicitud error:', error);
      throw error;
    }
  },
  
  exportAttendance: async (month, year) => {
    // Generate URL for download
    return `${API_BASE_URL}/export_asistencias_mensual.php?month=${month}&year=${year}`;
  },

  exportSolicitudes: (startDate, endDate) => {
    let url = `${API_BASE_URL}/export_solicitudes.php`;
    if (startDate && endDate) {
      url += `?start_date=${startDate}&end_date=${endDate}`;
    }
    return url;
  },

  getTopsPerformance: async () => {
    try {
      const response = await api.get('/get_tops_performance.php');
      return Array.isArray(response.data) ? response.data : [];
    } catch (error) {
      console.error("Error fetching tops:", error);
      return [];
    }
  },

  resetLunchStatus: async (userId) => {
    try {
      const response = await api.post('/reset_lunch_status.php', { userId });
      return response.data;
    } catch (error) {
      console.error('Reset Lunch error:', error);
      throw error;
    }
  },

  updateUserStatus: async (userId, estado) => {
    try {
      const response = await api.post('/update_status.php', { userId, estado });
      return response.data;
    } catch (error) {
      console.error('Update User Status error:', error);
      throw error;
    }
  },

  getAttendance: async (fecha) => {
    try {
      const params = fecha ? { fecha } : {};
      const response = await api.get('/get_attendance.php', { params });
      return Array.isArray(response.data) ? response.data : [];
    } catch (error) {
      console.error('Error fetching attendance:', error);
      return [];
    }
  },

  exportHorariosRuta: (startDate, endDate) => {
    if (startDate && endDate) {
      return `${API_BASE_URL}/export_horarios_ruta.php?start=${startDate}&end=${endDate}`;
    }
    return `${API_BASE_URL}/export_horarios_ruta.php`;
  },

  getSupervisiones: async (params = {}) => {
    try {
      const response = await api.get('/get_all_supervisiones.php', { params });
      return response.data;
    } catch (error) {
      console.error('Error fetching supervisiones:', error);
      throw error;
    }
  },

  exportSupervisiones: (startDate, endDate) => {
    let url = `${API_BASE_URL}/export_supervisiones.php`;
    if (startDate && endDate) {
      url += `?start_date=${startDate}&end_date=${endDate}`;
    }
    return url;
  },

  getSupervisorProductivity: async (params = {}) => {
    try {
      const response = await api.get('/get_supervisor_productivity.php', { params });
      return response.data;
    } catch (error) {
      console.error('Error fetching supervisor productivity:', error);
      throw error;
    }
  },

  getTNPS: async (params = {}) => {
    try {
      const response = await api.get('/get_tnps_evidencias.php', { params });
      return response.data;
    } catch (error) {
      console.error('Error fetching TNPS:', error);
      throw error;
    }
  }
};

export default api;
