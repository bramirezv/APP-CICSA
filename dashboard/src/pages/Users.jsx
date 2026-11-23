import React, { useState, useEffect, memo } from 'react';
import { 
  Users as UsersIcon, Plus, MoreVertical, 
  Search, Filter, Mail, Phone, Calendar,
  ShieldCheck, UserCircle, Download, X,
  Briefcase, MapPin, CheckCircle2, AlertCircle,
  ArrowRight, Pencil, Trash2, UserPlus, Database, Utensils
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { dataService, authService } from '../services/api';
import { useData } from '../services/DataContext';

const UserRow = memo(({ user, handleResetLunch }) => (
    <tr key={user.id} className="group hover:bg-slate-50 transition-colors">
        <td className="py-4 px-4">
            <div className="flex items-center gap-3">
                <div className="w-8 h-8 rounded bg-slate-100 flex items-center justify-center font-black text-blue-800 border border-slate-200 text-[10px]">
                    {(user.nombre || 'U').charAt(0)}
                </div>
                <div>
                    <div className="flex items-center gap-2">
                        <p className="font-bold text-slate-800 text-[12px] leading-tight">{user.nombre}</p>
                        {user.almuerzo_usado === 1 && (
                            <Utensils className="w-3 h-3 text-orange-500" title="Almuerzo usado" />
                        )}
                    </div>
                    <p className="text-[9px] font-black text-slate-400 uppercase tracking-widest mt-1">{user.rol}</p>
                </div>
            </div>
        </td>
        <td className="py-4 px-4">
            <span className="text-[9px] font-black text-slate-500 uppercase tracking-widest bg-slate-100 px-2.5 py-1 rounded border border-slate-200">
                {user.zona}
            </span>
        </td>
        <td className="py-4 px-4">
            <div className={`badge-std ${user.estado === 'Disponible' ? 'badge-green' : 'badge-yellow'} inline-flex items-center gap-2 px-2 py-0.5`}>
                <div className={`w-1.5 h-1.5 rounded-full ${user.estado === 'Disponible' ? 'bg-green-600' : 'bg-yellow-600'}`} />
                <span className="text-[9px]">{user.estado?.toUpperCase()}</span>
            </div>
        </td>
        <td className="py-4 px-4 text-slate-500 font-bold text-[10px]">
            <div className="flex flex-col gap-1">
                <span className="lowercase">{user.email || 'N/A'}</span>
                <span className="text-slate-400 font-black">{user.telefono || '—'}</span>
            </div>
        </td>
        <td className="py-4 px-4 text-right">
            <div className="inline-flex gap-2">
                {user.rol === 'satelite' && user.almuerzo_usado === 1 && (
                    <button 
                        onClick={() => handleResetLunch(user.id)}
                        className="p-1.5 text-orange-500 hover:text-orange-700 hover:bg-orange-50 rounded transition-all"
                        title="Resetear Almuerzo"
                    >
                        <Utensils className="w-3.5 h-3.5" />
                    </button>
                )}
                <button className="p-1.5 text-slate-400 hover:text-blue-800 hover:bg-slate-100 rounded transition-all">
                    <Pencil className="w-3.5 h-3.5" />
                </button>
                <button className="p-1.5 text-slate-400 hover:text-red-700 hover:bg-red-50 rounded transition-all">
                    <Trash2 className="w-3.5 h-3.5" />
                </button>
            </div>
        </td>
    </tr>
));

const UserManagement = () => {
    const { users, loading, refreshData, isRefreshing } = useData();
    const [formData, setFormData] = useState({
        name: '', email: '', telefono: '', password: '', rol: 'satelite', zona: 'SIN ZONA'
    });
    const [searchTerm, setSearchTerm] = useState('');
    const [isRegisterModalOpen, setRegisterModalOpen] = useState(false);
    const [submitting, setSubmitting] = useState(false);

    // No local useEffect for fetchUsers, we use the context data.

    const handleSubmit = async (e) => {
        e.preventDefault();
        setSubmitting(true);
        try {
            const res = await authService.register(formData);
            if (res.status === 'success') {
                setRegisterModalOpen(false);
                refreshData(); // Global refresh
                setFormData({ name: '', email: '', telefono: '', password: '', rol: 'satelite', zona: 'SIN ZONA' });
            } else {
                alert(res.message);
            }
        } catch (err) {
            alert("Error al registrar usuario");
        } finally {
            setSubmitting(false);
        }
    };

    const handleResetLunch = async (userId) => {
        if (window.confirm("¿Habilitar permiso de almuerzo para este usuario?")) {
            try {
                const res = await dataService.resetLunchStatus(userId);
                if (res.status === 'success') {
                    refreshData(); // Global refresh
                } else {
                    alert(res.message);
                }
            } catch (err) {
                alert("Error al resetear almuerzo");
            }
        }
    };

    const filteredUsers = users.filter(user => 
        (user.nombre || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
        (user.zona || '').toLowerCase().includes(searchTerm.toLowerCase())
    );

    return (
        <div className="space-y-6">
            {/* Header / Actions responsive layout */}
            <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4 border-b-2 border-slate-200 pb-5">
                <div className="text-left">
                    <h2 className="text-xl md:text-2xl font-bold text-slate-800 tracking-tight flex items-center gap-3">
                        <UsersIcon className="w-6 h-6 md:w-7 md:h-7 text-blue-800" />
                        Directorio de Personal
                    </h2>
                    <p className="text-[10px] text-slate-500 font-bold uppercase tracking-widest mt-1">Gestión Centralizada APPMOVIL</p>
                </div>
                <div className="flex flex-col sm:flex-row items-center gap-3 w-full lg:w-auto">
                    <button className="flex items-center justify-center gap-2 px-4 py-2.5 bg-white border border-slate-300 rounded text-[10px] font-black text-slate-600 hover:bg-slate-50 shadow-sm transition-all w-full sm:w-auto uppercase tracking-widest">
                        <Download className="w-3.5 h-3.5" /> Exportar
                    </button>
                    <button 
                        onClick={() => setRegisterModalOpen(true)}
                        className="btn-enterprise flex items-center justify-center gap-2 w-full sm:w-auto text-[10px] uppercase tracking-widest py-2.5"
                    >
                        <UserPlus className="w-4 h-4" /> Nuevo Registro
                    </button>
                </div>
            </div>

            {/* Filter Section responsive */}
            <div className="content-card p-3 md:p-4 flex flex-col md:flex-row items-center gap-3 md:gap-4">
                <div className="flex-1 w-full">
                    <div className="relative">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
                        <input 
                            type="text" 
                            className="form-input pl-10 h-10 text-[11px] md:text-xs font-bold" 
                            placeholder="Buscar por nombre o zona..." 
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                    </div>
                </div>
                <button className="flex items-center justify-center gap-2 px-4 py-2 border border-slate-300 rounded text-[10px] font-black text-slate-600 h-10 w-full md:w-auto uppercase tracking-widest">
                    <Filter className="w-4 h-4" /> Filtrar Filtros
                </button>
            </div>

            {/* Responsive User Table */}
            <div className="border border-slate-200 rounded-sm overflow-hidden shadow-sm bg-white">
                <div className="overflow-x-auto">
                    <table className="zebra-table min-w-[700px]">
                        <thead>
                            <tr className="bg-slate-50">
                                <th className="py-4 px-4 text-[10px] font-black uppercase tracking-widest">Identidad y Rol</th>
                                <th className="py-4 px-4 text-[10px] font-black uppercase tracking-widest">Zona</th>
                                <th className="py-4 px-4 text-[10px] font-black uppercase tracking-widest">Estado</th>
                                <th className="py-4 px-4 text-[10px] font-black uppercase tracking-widest">Contacto</th>
                                <th className="py-4 px-4 text-right text-[10px] font-black uppercase tracking-widest">Acciones</th>
                            </tr>
                        </thead>
                        <tbody>
                            {!loading && filteredUsers.map((user) => (
                                <UserRow key={user.id} user={user} handleResetLunch={handleResetLunch} />
                            ))}
                        </tbody>
                    </table>
                </div>
                
                {loading && (
                    <div className="py-24 flex flex-col items-center justify-center gap-4 bg-white/50">
                        <div className="w-6 h-6 border-3 border-blue-800 border-t-transparent rounded-full animate-spin" />
                        <p className="text-[9px] font-black text-slate-400 uppercase tracking-widest">Sincronizando Usuarios...</p>
                    </div>
                )}
            </div>

            {/* Adaptive Registration Modal */}
            <AnimatePresence>
                {isRegisterModalOpen && (
                    <div className="fixed inset-0 z-[1000] flex items-center justify-center p-4 md:p-6 bg-slate-900/40 backdrop-blur-sm">
                        <motion.div 
                            initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.95 }}
                            className="bg-white border border-slate-300 rounded shadow-2xl w-full max-w-xl overflow-y-auto max-h-[90vh]"
                        >
                            <div className="bg-blue-800 px-6 md:px-8 py-4 flex items-center justify-between text-white sticky top-0 z-10">
                                <h3 className="font-bold text-xs md:text-sm tracking-widest uppercase flex items-center gap-2 leading-none">
                                    <Database className="w-4 h-4" />
                                    Alta de Nuevo Personal
                                </h3>
                                <button onClick={() => setRegisterModalOpen(false)} className="hover:bg-white/10 p-1 rounded transition-colors">
                                    <X className="w-5 h-5" />
                                </button>
                            </div>

                            <form onSubmit={handleSubmit} className="p-6 md:p-10 space-y-6">
                                <div className="grid grid-cols-1 sm:grid-cols-2 gap-5 md:gap-6">
                                    <div className="space-y-1">
                                        <label className="text-[9px] font-black text-slate-400 uppercase tracking-widest ml-1">Nombre Completo</label>
                                        <input required type="text" className="form-input h-11" placeholder="Ej. Carlos Mendoza" value={formData.name} onChange={(e) => setFormData({...formData, name: e.target.value})} />
                                    </div>
                                    <div className="space-y-1">
                                        <label className="text-[9px] font-black text-slate-400 uppercase tracking-widest ml-1">Terminal Telefónica</label>
                                        <input required type="tel" className="form-input h-11" placeholder="987654321" value={formData.telefono} onChange={(e) => setFormData({...formData, telefono: e.target.value})} />
                                    </div>
                                </div>

                                <div className="space-y-1">
                                    <label className="text-[9px] font-black text-slate-400 uppercase tracking-widest ml-1">Correo Institucional</label>
                                    <input required type="email" className="form-input h-11" placeholder="usuario@appmovil.com" value={formData.email} onChange={(e) => setFormData({...formData, email: e.target.value})} />
                                </div>

                                <div className="grid grid-cols-1 sm:grid-cols-2 gap-5 md:gap-6">
                                    <div className="space-y-1">
                                        <label className="text-[9px] font-black text-slate-400 uppercase tracking-widest ml-1">Clave de Acceso</label>
                                        <input required type="password" className="form-input h-11" placeholder="********" value={formData.password} onChange={(e) => setFormData({...formData, password: e.target.value})} />
                                    </div>
                                    <div className="space-y-1">
                                        <label className="text-[9px] font-black text-slate-400 uppercase tracking-widest ml-1">Rol de Usuario</label>
                                        <select className="form-input bg-slate-50 h-11 text-[11px] font-bold" value={formData.rol} onChange={(e) => setFormData({...formData, rol: e.target.value})}>
                                            <option value="satelite">Satélite (Personal de Campo)</option>
                                            <option value="supervisor">Supervisor (Mando)</option>
                                            <option value="admin">Administrador General</option>
                                        </select>
                                    </div>
                                </div>

                                <div className="space-y-1">
                                    <label className="text-[9px] font-black text-slate-400 uppercase tracking-widest ml-1">Sector Operativo Asignado</label>
                                    <select className="form-input bg-slate-50 h-11 text-[11px] font-bold" value={formData.zona} onChange={(e) => setFormData({...formData, zona: e.target.value})}>
                                        <option value="SIN ZONA">Sin Sector Asignado</option>
                                        <option value="NORTE">Centro / Norte</option>
                                        <option value="SUR">Sur</option>
                                        <option value="ESTE">Este</option>
                                        <option value="OESTE">Oeste</option>
                                    </select>
                                </div>

                                <div className="pt-4">
                                    <button type="submit" disabled={submitting} className="w-full btn-enterprise h-12 flex items-center justify-center gap-3 text-[11px] uppercase tracking-widest shadow-xl shadow-blue-800/10 active:scale-[0.98] transition-transform">
                                        {submitting ? (
                                            <>
                                                <div className="w-4 h-4 border-2 border-white/20 border-t-white rounded-full animate-spin" />
                                                Procesando...
                                            </>
                                        ) : 'Finalizar Registro Maestro'}
                                    </button>
                                </div>
                            </form>
                        </motion.div>
                    </div>
                )}
            </AnimatePresence>
        </div>
    );
};

export default UserManagement;
