import React, { useState, useEffect, memo } from 'react';
import { 
  FilePlus, ClipboardList, Clock, CheckCircle2, 
  MapPin, User, Search, Filter, X, Send, 
  MoreVertical, Camera, Plus, Zap, ArrowRight,
  TrendingUp, Package, ShieldCheck, Box,
  FileText, ClipboardCheck, ArrowUpRight, UserPlus, FileSpreadsheet,
  Calendar, Map as MapIcon, ExternalLink, RefreshCw, Settings,
  AlertTriangle, MessageSquare, XCircle, Activity
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { dataService, authService } from '../services/api';
import { useData } from '../services/DataContext';

// Memoized row component for performance
const SolicitudRow = memo(({ sol, formatDate, getStatusStyle, getPhotoUrl, setActiveDropdown, activeDropdown, satellites, handleAssign }) => (
    <tr key={sol.id} className="group hover:bg-slate-50 transition-colors">
        <td className="text-[10px] font-bold text-slate-400 py-4 px-4 whitespace-nowrap">
            {formatDate(sol.fecha_creacion)}
        </td>
        <td className="py-4 px-4">
            <span className={`badge-std ${getStatusStyle(sol.estado)} px-2 py-0.5 text-[9px] font-black uppercase`}>
                {sol.estado?.toUpperCase()}
            </span>
        </td>
        <td className="py-5 px-4 min-w-[240px]">
            <div className="flex items-center gap-2 mb-3">
                <div className="relative">
                    <User className="w-5 h-5 text-slate-600" />
                    <Settings className="w-2.5 h-2.5 text-slate-500 absolute -bottom-0.5 -right-0.5 bg-white rounded-full" />
                </div>
                <span className="font-black text-[13px] text-slate-800 leading-tight">
                    {sol.tecnico || 'Técnico Externo'}
                </span>
            </div>
            <div className="border-l-[3px] border-red-600 bg-slate-50/80 p-3.5 rounded-r-md space-y-1.5 shadow-sm border border-slate-200 border-l-red-600">
                <p className="text-[11px] leading-tight text-slate-600">
                    <span className="font-black text-slate-800">Motivo:</span> {sol.motivo}
                </p>
                <p className="text-[11px] leading-tight text-slate-600">
                    <span className="font-black text-slate-800">SOT / req:</span> {sol.sot}
                </p>
                <p className="text-[11px] leading-tight text-slate-600">
                    <span className="font-black text-slate-800">Materiales:</span> {sol.material || 'Revisión técnica'}
                </p>
                <p className="text-[11px] leading-tight bg-white/50 -mx-3.5 px-3.5 py-1 mt-1 border-t border-slate-100">
                    <span className="font-black text-slate-800">Distrito:</span> <span className="text-blue-600 font-black">{sol.zona}</span>
                </p>

                {sol.comentario_satelite && (
                    <div className={`mt-3 p-3 rounded border-l-[4px] shadow-sm ${
                        sol.estado?.toLowerCase() === 'rechazada' 
                        ? 'bg-red-50 border-red-200 border-l-red-600 text-red-800' 
                        : 'bg-amber-50 border-amber-200 border-l-amber-500 text-amber-800'
                    }`}>
                        <div className="flex items-center gap-1.5 mb-1.5">
                            {sol.estado?.toLowerCase() === 'rechazada' ? <XCircle className="w-3.5 h-3.5" /> : <AlertTriangle className="w-3.5 h-3.5" />}
                            <span className="text-[10px] font-black uppercase tracking-wider">
                                {sol.estado?.toLowerCase() === 'rechazada' ? 'Motivo de Rechazo:' : 'Motivo de Corrección:'}
                            </span>
                        </div>
                        <p className="text-[11px] font-bold leading-tight bg-white/40 p-2 rounded-sm border border-black/5">
                            {sol.comentario_satelite}
                        </p>
                    </div>
                )}
            </div>
        </td>
        <td className="py-4 px-4">
            <span className="text-[10px] font-black text-slate-600 uppercase truncate max-w-[80px] block border-b border-slate-100">{sol.nombre_supervisor}</span>
        </td>
        <td className="py-4 px-4">
            <div className="flex items-center gap-2">
                {sol.nombre_satelite ? (
                    <div className="flex items-center gap-1.5 overflow-hidden">
                        <div className="w-5 h-5 rounded bg-blue-50 flex-shrink-0 flex items-center justify-center text-[9px] font-black text-blue-600 border border-blue-100">
                            {sol.nombre_satelite.charAt(0)}
                        </div>
                        <span className="text-[10px] font-bold text-slate-800 truncate">{sol.nombre_satelite}</span>
                    </div>
                ) : (
                    <span className="text-[9px] font-bold text-slate-300 italic">VACANTE</span>
                )}
            </div>
        </td>
        <td className="py-4 px-4">
            <div className="flex items-center gap-2">
                {sol.coordenadas ? (
                    <a href={`https://www.google.com/maps/search/?api=1&query=${sol.coordenadas}`} target="_blank" rel="noreferrer" className="text-blue-500 hover:text-blue-800 flex items-center gap-1">
                        <MapIcon className="w-3.5 h-3.5" />
                        <span className="text-[9px] font-bold font-mono">MAPA</span>
                    </a>
                ) : <span className="text-slate-300">--</span>}
            </div>
        </td>
        <td className="py-4 px-4">
            <span className="text-[9px] font-black text-slate-400 uppercase tracking-widest border border-slate-100 px-1.5 py-0.5 rounded">
                {sol.zona}
            </span>
        </td>
        <td className="py-4 px-4 text-center">
            {sol.foto_evidencia ? (() => {
                const photos = sol.foto_evidencia.split(',').map(p => p.trim()).filter(p => p && p !== 'null');
                if (photos.length === 0) return <span className="text-[8px] text-slate-200">N/A</span>;
                return (
                    <div className="flex items-center justify-center gap-1 flex-wrap">
                        {photos.map((photo, idx) => (
                            <a key={idx} href={getPhotoUrl(photo)} target="_blank" rel="noreferrer" 
                               className="p-1 bg-emerald-50 text-emerald-600 rounded border border-emerald-100 hover:bg-emerald-100 transition-all font-black text-[8px] uppercase tracking-tighter">
                                📷{photos.length > 1 ? idx + 1 : ''}
                            </a>
                        ))}
                        {photos.length > 1 && (
                            <span className="text-[8px] font-black text-emerald-600">{photos.length}</span>
                        )}
                    </div>
                );
            })() : <span className="text-[8px] text-slate-200">N/A</span>}
        </td>
        <td className="py-4 px-4 text-right relative">
            <button 
                onClick={() => setActiveDropdown(activeDropdown === sol.id ? null : sol.id)}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-blue-800 text-white rounded text-[9px] font-black shadow-sm hover:translate-y-[-1px] transition-all uppercase tracking-widest leading-none"
            >
                {sol.nombre_satelite ? 'Reasig' : 'Asig'}
                <MoreVertical className="w-3 h-3" />
            </button>

            <AnimatePresence>
                {activeDropdown === sol.id && (
                    <>
                        <div className="fixed inset-0 z-[100]" onClick={() => setActiveDropdown(null)} />
                        <motion.div 
                            initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.95 }}
                            className="absolute right-0 mt-2 w-48 bg-white border border-slate-200 rounded shadow-2xl z-[101] overflow-hidden text-left"
                        >
                            <div className="px-3 py-2 bg-slate-50 border-b border-slate-200 font-black text-[8px] text-slate-400 uppercase tracking-widest">Personal Disponible</div>
                            <div className="max-h-48 overflow-y-auto">
                                {satellites.map(sat => (
                                    <button
                                        key={sat.id} onClick={() => handleAssign(sol.id, sat.id)}
                                        className="w-full px-3 py-2.5 hover:bg-blue-50 flex items-center justify-between group transition-colors border-b border-slate-50"
                                    >
                                        <div className="flex flex-col">
                                            <span className="text-[10px] font-black text-slate-800 group-hover:text-blue-800 transition-colors">{sat.nombre}</span>
                                            <span className="text-[8px] font-bold text-slate-400 uppercase">{sat.zona}</span>
                                        </div>
                                        <div className={`w-1.5 h-1.5 rounded-full ${sat.estado === 'Disponible' ? 'bg-emerald-500 shadow-[0_0_5px_#10b981]' : 'bg-slate-200'}`} />
                                    </button>
                                ))}
                            </div>
                        </motion.div>
                    </>
                )}
            </AnimatePresence>
        </td>
    </tr>
));

const SolicitudesManagement = () => {
    const { solicitudes: globalSolicitudes, users: allUsers, loading, refreshData, isRefreshing } = useData();
    const [isCreateModalOpen, setCreateModalOpen] = useState(false);
    const [activeDropdown, setActiveDropdown] = useState(null);
    const [formData, setFormData] = useState({
        sot: '', tecnico: '', motivo: '', material: '', coordenadas: '', zona: 'NORTE',
        nombre_supervisor: authService.getCurrentUser()?.userName || 'ADMIN'
    });
    const [submitting, setSubmitting] = useState(false);
    const [startDate, setStartDate] = useState('');
    const [endDate, setEndDate] = useState('');

    const satellites = allUsers.filter(u => u.rol === 'satelite');

    const [filteredSolicitudes, setFilteredSolicitudes] = useState([]);
    const [isFilterActive, setIsFilterActive] = useState(false);

    useEffect(() => {
        if (startDate && endDate) {
            const fetchFiltered = async () => {
                try {
                    const res = await dataService.getSolicitudes({ start_date: startDate, end_date: endDate });
                    setFilteredSolicitudes(Array.isArray(res) ? res : []);
                    setIsFilterActive(true);
                } catch (e) { console.error(e); }
            };
            fetchFiltered();
        } else {
            setIsFilterActive(false);
        }
    }, [startDate, endDate]);

    const displaySolicitudes = (isFilterActive ? filteredSolicitudes : globalSolicitudes).filter(s => s.tipo !== 'auxiliar');

    const handleSubmit = async (e) => {
        e.preventDefault();
        setSubmitting(true);
        try {
            const res = await dataService.createSolicitud(formData);
            if (res.status === 'success') {
                setCreateModalOpen(false);
                refreshData(); // Global refresh
                setFormData({
                    sot: '', tecnico: '', motivo: '', material: '', coordenadas: '', zona: 'NORTE', 
                    nombre_supervisor: authService.getCurrentUser()?.userName || 'ADMIN'
                });
            } else {
                alert(res.message);
            }
        } catch (err) {
            alert("Error al crear solicitud");
        } finally {
            setSubmitting(false);
        }
    };

    const handleAssign = async (solicitudId, sateliteId) => {
        try {
            const res = await dataService.updateSolicitudStatus(solicitudId, 'aceptada', sateliteId);
            if (res.status === 'success') {
                setActiveDropdown(null);
                refreshData(); // Global refresh
            } else {
                alert(res.message);
            }
        } catch (err) {
            alert("Error al asignar");
        }
    };
    const handleExportExcel = () => {
        const url = dataService.exportSolicitudes(startDate, endDate);
        window.open(url, '_blank');
    };

    const getStatusStyle = (status) => {
        switch (status?.toLowerCase()) {
            case 'pendiente': return 'badge-yellow';
            case 'aceptada': return 'badge-blue';
            case 'finalizada': return 'badge-green';
            case 'en_camino': return 'bg-cyan-100 text-cyan-700 border-cyan-200';
            case 'corregir': return 'bg-amber-100 text-amber-700 border-amber-200';
            case 'rechazada': return 'bg-red-100 text-red-700 border-red-200';
            case 'almorzando': return 'bg-orange-100 text-orange-700 border-orange-200';
            default: return 'bg-slate-100 text-slate-600';
        }
    };

    const formatDate = (dateStr) => {
        if (!dateStr) return '--';
        const date = new Date(dateStr.replace(/-/g, '/'));
        return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    };

    const getPhotoUrl = (path) => {
        if (!path || path === 'null') return null;
        
        const productionDomain = "http://www.cicsamovil.com/";
        
        // Handle tudominio.com replacement (from DB logs)
        if (path.includes("tudominio.com")) {
            return path.replace("https://tudominio.com/", productionDomain)
                       .replace("http://tudominio.com/", productionDomain)
                       .replace("http://ww38.tudominio.com/", productionDomain);
        }

        if (path.startsWith('http')) return path;
        
        const cleanPath = path.startsWith('/') ? path.substring(1) : path;
        return `${productionDomain}${cleanPath}`;
    };

    return (
        <div className="space-y-6">
            {/* Header section responsive */}
            <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4 border-b-2 border-slate-200 pb-5">
                <div>
                    <h2 className="text-xl md:text-2xl font-bold text-slate-800 tracking-tight flex items-center gap-3">
                        <ClipboardList className="w-6 h-6 md:w-7 md:h-7 text-blue-800" />
                        Despacho de Bolsa SOT
                    </h2>
                    <p className="text-[10px] text-slate-500 font-bold uppercase tracking-widest mt-1">Control de Operaciones y Satélites</p>
                </div>
                <div className="flex flex-col sm:flex-row gap-3 w-full lg:w-auto">
                    <div className="flex items-center gap-2 bg-slate-100 p-1.5 rounded border border-slate-200">
                        <div className="flex items-center gap-1.5 px-2">
                            <Calendar className="w-3.5 h-3.5 text-slate-500" />
                            <span className="text-[9px] font-black text-slate-500 uppercase tracking-tighter">Periodo:</span>
                        </div>
                        <input 
                            type="date" 
                            value={startDate} 
                            onChange={(e) => setStartDate(e.target.value)}
                            className="bg-white border border-slate-300 rounded px-2 py-1 text-[10px] font-bold outline-none focus:border-blue-500"
                        />
                        <span className="text-[9px] font-black text-slate-400">AL</span>
                        <input 
                            type="date" 
                            value={endDate} 
                            onChange={(e) => setEndDate(e.target.value)}
                            className="bg-white border border-slate-300 rounded px-2 py-1 text-[10px] font-bold outline-none focus:border-blue-500"
                        />
                        <button 
                            onClick={() => isFilterActive ? null : refreshData()}
                            className="p-1.5 bg-blue-800 text-white rounded hover:bg-black transition-colors"
                            title="Filtrar Vista"
                        >
                            <RefreshCw className={`w-3.5 h-3.5 ${loading || isRefreshing ? 'animate-spin' : ''}`} />
                        </button>
                    </div>

                    <button 
                        onClick={handleExportExcel}
                        className="flex items-center justify-center gap-2 px-4 py-2 bg-emerald-600 text-white rounded text-[10px] font-black uppercase tracking-widest shadow-md hover:bg-emerald-700 transition-all"
                    >
                        <FileSpreadsheet className="w-4 h-4" /> Exportar Excel
                    </button>

                    <button 
                        onClick={() => setCreateModalOpen(true)}
                        className="btn-enterprise flex items-center justify-center gap-2 text-[10px] uppercase tracking-widest py-2.5 shadow-lg shadow-blue-800/10"
                    >
                        <Plus className="w-4 h-4" /> Nueva Orden
                    </button>
                </div>
            </div>

            {/* Responsive List Container */}
            <div className="border border-slate-200 rounded-sm overflow-hidden shadow-sm bg-white min-h-[500px]">
                <div className="overflow-x-auto">
                    <table className="zebra-table min-w-[1000px]">
                        <thead>
                            <tr className="bg-slate-50 border-b border-slate-200">
                                <th className="py-4 px-4 text-[9px] font-black uppercase tracking-widest">Fecha / Registro</th>
                                <th className="py-4 px-4 text-[9px] font-black uppercase tracking-widest">Estado</th>
                                <th className="py-4 px-4 text-[9px] font-black uppercase tracking-widest">Detalle Tarea</th>
                                <th className="py-4 px-4 text-[9px] font-black uppercase tracking-widest">Supervisor</th>
                                <th className="py-4 px-4 text-[9px] font-black uppercase tracking-widest">Responsable</th>
                                <th className="py-4 px-4 text-[9px] font-black uppercase tracking-widest">GPS</th>
                                <th className="py-4 px-4 text-[9px] font-black uppercase tracking-widest">Distrito</th>
                                <th className="py-4 px-4 text-[9px] font-black uppercase tracking-widest">Foto</th>
                                <th className="py-4 px-4 text-right text-[9px] font-black uppercase tracking-widest">Gestión</th>
                            </tr>
                        </thead>
                        <tbody>
                            {!loading && displaySolicitudes.map((sol) => (
                                <SolicitudRow 
                                    key={sol.id} 
                                    sol={sol}
                                    formatDate={formatDate}
                                    getStatusStyle={getStatusStyle}
                                    getPhotoUrl={getPhotoUrl}
                                    setActiveDropdown={setActiveDropdown}
                                    activeDropdown={activeDropdown}
                                    satellites={satellites}
                                    handleAssign={handleAssign}
                                />
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* Modal adaptive */}
            <AnimatePresence>
                {isCreateModalOpen && (
                    <div className="fixed inset-0 z-[1000] flex items-center justify-center p-4 md:p-6 bg-slate-900/40 backdrop-blur-sm">
                        <motion.div 
                            initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.95 }}
                            className="bg-white border border-slate-300 rounded shadow-2xl w-full max-w-2xl overflow-y-auto max-h-[90vh]"
                        >
                            <div className="bg-blue-800 px-6 md:px-10 py-5 flex items-center justify-between text-white sticky top-0 z-10">
                                <div>
                                    <h3 className="font-bold text-sm md:text-lg tracking-tight uppercase leading-none">Emisión SOT Directa</h3>
                                    <p className="text-[9px] opacity-60 font-black uppercase tracking-widest mt-1">Gestión Centralizada de Bolsa</p>
                                </div>
                                <button onClick={() => setCreateModalOpen(false)} className="hover:bg-white/10 p-2 rounded transition-colors">
                                    <X className="w-5 h-5 md:w-6 md:h-6" />
                                </button>
                            </div>

                            <form onSubmit={handleSubmit} className="p-6 md:p-10 space-y-6 md:space-y-8">
                                <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 md:gap-8">
                                    <div className="space-y-1">
                                        <label className="text-[9px] font-black text-slate-500 uppercase tracking-widest ml-1">Num. SOT</label>
                                        <input required className="form-input h-11 text-base font-black" value={formData.sot} onChange={(e) => setFormData({...formData, sot: e.target.value})} />
                                    </div>
                                    <div className="space-y-1">
                                        <label className="text-[9px] font-black text-slate-500 uppercase tracking-widest ml-1">Técnico Operativo</label>
                                        <input required className="form-input h-11" value={formData.tecnico} onChange={(e) => setFormData({...formData, tecnico: e.target.value})} />
                                    </div>
                                </div>

                                <div className="space-y-1">
                                    <label className="text-[9px] font-black text-slate-500 uppercase tracking-widest ml-1">Memorándum / Instrucciones</label>
                                    <textarea required rows="4" className="form-input text-xs italic leading-relaxed" value={formData.motivo} onChange={(e) => setFormData({...formData, motivo: e.target.value})} />
                                </div>

                                <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 md:gap-8">
                                    <div className="space-y-1">
                                        <label className="text-[9px] font-black text-slate-500 uppercase tracking-widest ml-1">Sector Red</label>
                                        <select className="form-input h-11 bg-slate-50 text-xs font-bold" value={formData.zona} onChange={(e) => setFormData({...formData, zona: e.target.value})}>
                                            <option value="NORTE">Centro / Norte</option>
                                            <option value="SUR">Sector Sur</option>
                                            <option value="ESTE">Sector Este</option>
                                            <option value="OESTE">Sector Oeste</option>
                                        </select>
                                    </div>
                                    <div className="space-y-1">
                                        <label className="text-[9px] font-black text-slate-500 uppercase tracking-widest ml-1">Ubicación (Lat, Lng)</label>
                                        <input className="form-input h-11 font-mono text-[10px]" value={formData.coordenadas} onChange={(e) => setFormData({...formData, coordenadas: e.target.value})} />
                                    </div>
                                </div>

                                <button type="submit" disabled={submitting} className="w-full btn-enterprise h-12 flex items-center justify-center gap-3 text-[11px] uppercase tracking-widest shadow-xl shadow-blue-800/10 active:scale-[0.98] transition-transform">
                                    {submitting ? 'Procesando...' : (
                                        <>
                                            <ClipboardCheck className="w-4 h-4" /> Finalizar y Publicar
                                        </>
                                    )}
                                </button>
                            </form>
                        </motion.div>
                    </div>
                )}
            </AnimatePresence>
        </div>
    );
};

export default SolicitudesManagement;
