import React, { useState, useEffect } from 'react';
import { Routes, Route, Link, useNavigate, useLocation } from 'react-router-dom';
import { 
  BarChart3, Users, Map as MapIcon, 
  FileSpreadsheet, LogOut, LayoutDashboard,
  Bell, Search, Menu, X, UserCircle, ClipboardList,
  Settings, ChevronRight, Monitor, Activity, Clock, Box, ShieldCheck, Camera
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { authService } from '../services/api';
import { DataProvider } from '../services/DataContext';
import Overview from './Overview';
import MapView from './Map';
import UserManagement from './Users';
import Reports from './Reports';
import SolicitudesManagement from './Solicitudes';
import SatelitesStatus from './SatelitesStatus';
import Asistencia from './Asistencia';
import AuxiliaresSolicitudes from './AuxiliaresSolicitudes';
import AuxiliaresStatus from './AuxiliaresStatus';
import SupervisionReports from './SupervisionReports';
import TNPSMonitoring from './TNPSMonitoring';

const Dashboard = () => {
  const [isSidebarOpen, setSidebarOpen] = useState(false); // For Mobile Drawer
  const [admin, setAdmin] = useState(null);
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    const user = authService.getCurrentUser();
    setAdmin(user);
    setSidebarOpen(false); // Close sidebar on route change (for mobile)
  }, [location]);

  const handleLogout = () => {
    authService.logout();
    navigate('/login');
  };

  const navItems = [
    { name: 'Vista General', icon: LayoutDashboard, path: '/dashboard' },
    { name: 'Monitoreo de Mapa', icon: MapIcon, path: '/dashboard/map' },
    { name: 'Bolsa de Tareas', icon: ClipboardList, path: '/dashboard/solicitudes' },
    { name: 'Bolsa de Auxiliares', icon: Box, path: '/dashboard/auxiliares' },
    { name: 'Estado de Satélites', icon: Activity, path: '/dashboard/satelites' },
    { name: 'Estado de Auxiliares', icon: Activity, path: '/dashboard/estado-auxiliares' },
    { name: 'Control Asistencia', icon: Clock, path: '/dashboard/asistencia' },
    { name: 'Gestión de Personal', icon: Users, path: '/dashboard/users' },
    { name: 'Reportes Supervisión', icon: ShieldCheck, path: '/dashboard/supervision' },
    { name: 'Monitoreo TNPS', icon: Camera, path: '/dashboard/tnps' },
    { name: 'Reportes y Auditoría', icon: FileSpreadsheet, path: '/dashboard/reports' },
  ];

  return (
    <DataProvider>
      <div className="min-h-screen bg-slate-100 flex flex-col">
      
      {/* Top Header - Institutional Royal Blue */}
      <header className="enterprise-header flex items-center justify-between sticky top-0 z-[110]">
        <div className="flex items-center gap-4 md:gap-6">
            {/* Mobile Menu Toggle */}
            <button 
                onClick={() => setSidebarOpen(!isSidebarOpen)}
                className="md:hidden p-1.5 hover:bg-white/10 rounded transition-colors"
            >
                {isSidebarOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>

            <div className="flex items-center gap-2 md:gap-3">
                <div className="bg-white p-1 rounded hidden sm:block">
                    <Monitor className="w-4 h-4 md:w-5 md:h-5 text-blue-800" />
                </div>
                <div className="leading-none">
                    <h1 className="text-sm md:text-lg font-black tracking-tighter uppercase">APPMOVIL V2.1 - AUDITORÍA</h1>
                    <span className="text-[7px] md:text-[9px] font-bold opacity-60 uppercase tracking-widest">Portal de Administración</span>
                </div>
            </div>
            <div className="h-6 w-px bg-white/20 hidden md:block" />
            <div className="hidden lg:flex items-center gap-2 text-xs font-semibold opacity-80">
                <span className="opacity-60">ADMIN</span>
                <ChevronRight className="w-3 h-3" />
                <span>OPERATIVO</span>
            </div>
        </div>

        <div className="flex items-center gap-3 md:gap-6">
            <div className="hidden xl:flex items-center gap-3 bg-white/10 px-4 py-2 rounded text-sm">
                <Search className="w-4 h-4" />
                <input type="text" placeholder="Buscar..." className="bg-transparent border-none outline-none placeholder:text-white/50 w-32 focus:w-48 transition-all" />
            </div>
            <div className="flex items-center gap-3 md:gap-4 border-l border-white/20 pl-3 md:pl-6">
                <button className="relative p-1 hover:bg-white/10 rounded transition-colors">
                    <Bell className="w-5 h-5" />
                    <span className="absolute top-0.5 right-0.5 w-2 h-2 bg-red-500 rounded-full border border-blue-800" />
                </button>
                <div className="flex items-center gap-2">
                    <div className="text-right hidden sm:block">
                        <p className="text-[10px] md:text-[11px] font-bold leading-none">{admin?.userName}</p>
                        <p className="text-[8px] md:text-[9px] opacity-60 uppercase font-black tracking-tighter mt-1">{admin?.rol}</p>
                    </div>
                    <UserCircle className="w-7 h-7 md:w-8 md:h-8 opacity-80" />
                </div>
            </div>
        </div>
      </header>

      <div className="flex flex-1 relative">
        
        {/* Overlay for mobile sidebar */}
        <AnimatePresence>
            {isSidebarOpen && (
                <motion.div 
                    initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
                    onClick={() => setSidebarOpen(false)}
                    className="fixed inset-0 bg-slate-900/60 z-[100] md:hidden backdrop-blur-sm"
                />
            )}
        </AnimatePresence>

        {/* Sidebar - Desktop Sidebar & Mobile Drawer */}
        <aside className={`
            enterprise-sidebar fixed md:sticky top-[48px] md:top-[56px] z-[105] md:z-10
            transition-transform duration-300 ease-in-out
            ${isSidebarOpen ? 'translate-x-0' : '-translate-x-full md:translate-x-0'}
            overflow-y-auto w-[260px] md:w-[260px]
        `}>
            <div className="p-6">
                <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest mb-4">Menú de Navegación</p>
                <nav className="space-y-1">
                    {navItems.map((item) => {
                        const isActive = location.pathname === item.path || (item.path === '/dashboard' && location.pathname === '/dashboard/');
                        return (
                        <Link
                            key={item.name}
                            to={item.path}
                            className={`sidebar-nav-item ${isActive ? 'sidebar-nav-active' : ''}`}
                        >
                            <item.icon className="w-5 h-5" />
                            <span className="flex-1">{item.name}</span>
                            {item.isNew && (
                                <span className="bg-blue-600 text-[8px] text-white px-1.5 py-0.5 rounded font-black animate-pulse">NUEVO</span>
                            )}
                        </Link>
                        );
                    })}
                </nav>

                <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest mt-12 mb-4">Configuración</p>
                <button className="sidebar-nav-item w-full text-left">
                    <Settings className="w-5 h-5" />
                    <span>Ajustes Generales</span>
                </button>
                <button 
                    onClick={handleLogout}
                    className="sidebar-nav-item w-full text-left text-red-600 hover:bg-red-50"
                >
                    <LogOut className="w-5 h-5" />
                    <span>Cerrar Sesión</span>
                </button>
            </div>
        </aside>

        {/* Dynamic Content Container */}
        <main className="flex-1 p-4 md:p-8 overflow-y-auto w-full">
            <div className="max-w-7xl mx-auto space-y-6">
                <AnimatePresence mode="wait">
                    <motion.div
                        key={location.pathname}
                        initial={{ opacity: 0, y: 10 }}
                        animate={{ opacity: 1, y: 0 }}
                        exit={{ opacity: 0, y: -10 }}
                        transition={{ duration: 0.15 }}
                    >
                        <Routes>
                            <Route path="/" element={<Overview />} />
                            <Route path="/map" element={<MapView />} />
                            <Route path="/users" element={<UserManagement />} />
                            <Route path="/solicitudes" element={<SolicitudesManagement />} />
                            <Route path="/auxiliares" element={<AuxiliaresSolicitudes />} />
                            <Route path="/satelites" element={<SatelitesStatus />} />
                            <Route path="/estado-auxiliares" element={<AuxiliaresStatus />} />
                            <Route path="/asistencia" element={<Asistencia />} />
                            <Route path="/supervision" element={<SupervisionReports />} />
                            <Route path="/tnps" element={<TNPSMonitoring />} />
                            <Route path="/reports" element={<Reports />} />
                        </Routes>
                    </motion.div>
                </AnimatePresence>
            </div>
            
            <footer className="mt-20 py-8 border-t border-slate-200 text-center">
                <p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest leading-loose">
                    APPMOVIL S.A.C © 2026 • TODOS LOS DERECHOS RESERVADOS <br />
                    SISTEMA CORPORATIVO DE CONTROL Y DESPACHO
                </p>
            </footer>
        </main>
      </div>
    </div>
    </DataProvider>
  );
};

export default Dashboard;
