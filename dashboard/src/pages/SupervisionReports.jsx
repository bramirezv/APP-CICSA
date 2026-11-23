import React, { useState, useEffect } from 'react';
import { 
  ShieldCheck, Search, Calendar, Filter, 
  MapPin, User, Eye, Download, FileSpreadsheet,
  Star, ClipboardCheck, Clock, ExternalLink,
  ChevronRight, ArrowUpRight, Camera, Info,
  MoreVertical, X, AlertCircle, RefreshCw, TrendingUp
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { dataService } from '../services/api';
 
const QUESTIONS_MAP = {
    "calidad_1": "El técnico es autosuficiente, tiene un trato adecuado, se encuentra correctamente uniformado y mejora la experiencia del cliente",
    "calidad_2": "El técnico brinda información de los servicios de valor agregado y soluciones únicas de nuestro operador",
    "seguridad_1": "El técnico cuenta con sus EPP y EPE completos y los usa de forma adecuada",
    "seguridad_2": "El técnico cuida su integridad física y respeta las distancias mínimas de seguridad eléctrica -DMS?",
    "evaluacion_1": "El técnico llega a la hora programada con el equipo necesario",
    "evaluacion_2": "El técnico se encuentra capacitado cumple con las normas y procedimientos de claro?",
    "evaluacion_3": "El técnico usa los aplicativos de claro, brinda soluciones y acepta con buena actitud las recomendaciones del supervisor?",
    "config_1": "Se configura de forma correcta los equipos instalados y se explica al cliente el uso del servicio (IPTV, MESH, WIFI, CONTROL REMOTO, APP DEL CLIENTE Y GRILLA DE CANALES)?",
    "config_2": "Se verifica parámetros adecuados en el ONT/EMTA dentro de rangos establecidos (NIVELES, PING, TEST DE VELOCIDAD Y TEST DE ESFUERZO)?",
    "config_3": "Se realiza la configuración correcta y explicación de servicios de valor agregado para clientes claro negocio?",
    "ejecucion_1": "El técnico instala respetando las normativas técnicas la acometida (loop, chapa q, chapa p)?",
    "ejecucion_2": "El técnico deja limpia su zona de trabajo?",
    "ejecucion_3": "El técnico cumple con la normativa de instalación de FTTH O HFC?",
    "ejecucion_4": "El técnico deja el/los equipos en un lugar adecuado y correctamente cableado para el uso del servicio?",
    "ejecucion_5": "El técnico conectoriza de forma adecuada los conectores SC/APC, RG6, RJ45, ROSETA Y RJ11?",
    "tnps_1": "El cliente quedó satisfecho con la atención?"
};

const SupervisionReports = () => {
    const [supervisiones, setSupervisiones] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [selectedReport, setSelectedReport] = useState(null);
    const [searchTerm, setSearchTerm] = useState('');
    const [filterTecnico, setFilterTecnico] = useState('');
    const [startDate, setStartDate] = useState('');
    const [endDate, setEndDate] = useState('');
    const [prodStats, setProdStats] = useState([]);
    const [isRefreshingProd, setIsRefreshingProd] = useState(false);

    useEffect(() => {
        fetchProdStats();
    }, []);

    useEffect(() => {
        if ((startDate && endDate) || (!startDate && !endDate)) {
            fetchData();
            fetchProdStats();
        }
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [startDate, endDate]);

    const fetchProdStats = async () => {
        setIsRefreshingProd(true);
        try {
            const params = {};
            if (startDate && endDate) {
                params.start_date = startDate;
                params.end_date = endDate;
            }
            const res = await dataService.getSupervisorProductivity(params);
            if (res.status === 'success') {
                setProdStats(res.data || []);
            }
        } catch (error) {
            console.error("Error fetching productivity:", error);
        } finally {
            setIsRefreshingProd(false);
        }
    };

    const fetchData = async () => {
        setLoading(true);
        setError(null);
        try {
            const params = {};
            if (startDate && endDate) {
                params.start_date = startDate;
                params.end_date = endDate;
            }
            const res = await dataService.getSupervisiones(params);
            if (res.status === 'success') {
                setSupervisiones(res.data || []);
            } else {
                setError(res.message || "Error al obtener datos");
            }
        } catch (err) {
            console.error("Error loading supervisiones", err);
            setError("No se pudo conectar con el servidor. Verifica tu conexión.");
        } finally {
            setLoading(false);
        }
    };

    const formatDate = (dateStr) => {
        if (!dateStr) return '--';
        const date = new Date(dateStr.replace(/-/g, '/'));
        return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    };

    const getScoreColor = (score) => {
        if (score >= 4) return 'text-emerald-600 bg-emerald-50 border-emerald-100';
        if (score >= 3) return 'text-amber-600 bg-amber-50 border-amber-100';
        return 'text-red-600 bg-red-50 border-red-100';
    };

    const filteredReports = supervisiones.filter(report => {
        const matchesSearch = report.sot?.toLowerCase().includes(searchTerm.toLowerCase()) || 
                             report.nombre_tecnico?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                             report.nombre_supervisor?.toLowerCase().includes(searchTerm.toLowerCase());
        const matchesTecnico = filterTecnico === '' || report.nombre_tecnico === filterTecnico;
        
        let matchesDate = true;
        if (startDate && endDate && report.fecha_creacion) {
            const reportDate = new Date(report.fecha_creacion.replace(/-/g, '/'));
            const start = new Date(startDate.replace(/-/g, '/') + ' 00:00:00');
            const end = new Date(endDate.replace(/-/g, '/') + ' 23:59:59');
            matchesDate = reportDate >= start && reportDate <= end;
        }

        return matchesSearch && matchesTecnico && matchesDate;
    });

    const uniqueTecnicos = [...new Set(supervisiones.map(s => s.nombre_tecnico))].filter(Boolean);

    // Métricas
    const totalSupervisiones = supervisiones.length;
    const avgScore = supervisiones.length > 0 
        ? (supervisiones.reduce((acc, curr) => acc + (curr.puntuacion || 0), 0) / supervisiones.length).toFixed(1)
        : 0;
    const conformes = supervisiones.filter(s => s.puntuacion >= 4).length;

    return (
        <div className="space-y-6 pb-12">
            {/* Header */}
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b-2 border-slate-200 pb-5">
                <div>
                    <h2 className="text-xl md:text-2xl font-bold text-slate-800 tracking-tight flex items-center gap-3">
                        <ShieldCheck className="w-6 h-6 md:w-7 md:h-7 text-blue-800" />
                        Módulo de Supervisión de Campo
                    </h2>
                    <p className="text-[10px] text-slate-500 font-bold uppercase tracking-widest mt-1">Auditoría Técnica y Cumplimiento Normativo</p>
                </div>
                <div className="flex flex-col sm:flex-row gap-3 w-full md:w-auto">
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
                            onClick={fetchData}
                            className="p-1.5 bg-blue-800 text-white rounded hover:bg-black transition-colors"
                            title="Filtrar Vista"
                        >
                            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
                        </button>
                    </div>
                    <button 
                        onClick={() => window.open(dataService.exportSupervisiones(startDate, endDate), '_blank')}
                        className="flex items-center justify-center gap-2 px-4 py-2.5 bg-blue-800 text-white rounded text-[10px] font-black uppercase tracking-widest shadow-lg shadow-blue-800/20"
                    >
                        <Download className="w-4 h-4" /> Exportar Auditoría
                    </button>
                </div>
            </div>

            {/* Metrics Grid */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div className="content-card p-5 border-l-4 border-l-blue-600">
                    <p className="text-[9px] font-black text-slate-400 uppercase tracking-widest">Total Supervisadas</p>
                    <div className="flex items-end justify-between mt-2">
                        <h4 className="text-2xl font-black text-slate-800">{totalSupervisiones}</h4>
                        <div className="p-2 bg-blue-50 rounded text-blue-600">
                            <ClipboardCheck className="w-5 h-5" />
                        </div>
                    </div>
                </div>
                <div className="content-card p-5 border-l-4 border-l-emerald-600">
                    <p className="text-[9px] font-black text-slate-400 uppercase tracking-widest">Índice de Conformidad</p>
                    <div className="flex items-end justify-between mt-2">
                        <h4 className="text-2xl font-black text-slate-800">{conformes} <span className="text-xs font-bold text-slate-400">APTOS</span></h4>
                        <div className="p-2 bg-emerald-50 rounded text-emerald-600">
                            <Star className="w-5 h-5" />
                        </div>
                    </div>
                </div>
                <div className="content-card p-5 border-l-4 border-l-amber-600">
                    <p className="text-[9px] font-black text-slate-400 uppercase tracking-widest">SOTs Auditadas</p>
                    <div className="flex items-end justify-between mt-2">
                        <h4 className="text-2xl font-black text-slate-800">{filteredReports.length}</h4>
                        <div className="p-2 bg-amber-50 rounded text-amber-600">
                            <TrendingUp className="w-5 h-5" />
                        </div>
                    </div>
                </div>
            </div>

            {/* Productivity Dashboard (NEW) */}
            <div className="space-y-4">
                <div className="flex items-center justify-between border-b border-slate-200 pb-2">
                    <h4 className="text-[10px] font-black text-slate-500 uppercase tracking-widest flex items-center gap-2">
                        <TrendingUp className="w-4 h-4 text-blue-600" />
                        Control de Productividad Supervisores {(startDate && endDate) ? `(${startDate} AL ${endDate})` : '(HOY)'}
                    </h4>
                </div>
                
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                    {isRefreshingProd && prodStats.length === 0 ? (
                        <div className="col-span-full py-8 text-center bg-white rounded-lg border border-slate-200">
                             <RefreshCw className="w-5 h-5 animate-spin mx-auto text-blue-600" />
                        </div>
                    ) : prodStats.map((item) => (
                        <div key={item.supervisor_id} className="content-card p-4 hover:shadow-md transition-all group border-t-4 border-t-blue-800">
                            <div className="flex justify-between items-start">
                                <div className="flex-1 min-w-0">
                                    <h5 className="font-black text-slate-800 text-[11px] uppercase truncate">{item.supervisor_nombre}</h5>
                                    <p className="text-[8px] font-bold text-slate-400 mt-0.5">ID: {item.supervisor_id}</p>
                                </div>
                                <div className="bg-blue-50 text-blue-800 px-2 py-1 rounded font-black text-lg leading-none">
                                    {item.total_supervisiones}
                                </div>
                            </div>
                            <div className="mt-3">
                                <p className="text-[8px] font-black text-slate-400 uppercase tracking-tighter mb-1">Técnicos auditados:</p>
                                <div className="flex flex-wrap gap-1 max-h-12 overflow-y-auto custom-scrollbar">
                                    {item.tecnicos_supervisados ? item.tecnicos_supervisados.split(', ').map((tec, i) => (
                                        <span key={i} className="px-1.5 py-0.5 bg-slate-50 border border-slate-100 rounded text-[9px] font-bold text-slate-500">
                                            {tec}
                                        </span>
                                    )) : <span className="text-[9px] text-slate-300 italic font-bold">Sin actividad</span>}
                                </div>
                            </div>
                        </div>
                    ))}
                    {prodStats.length === 0 && !isRefreshingProd && (
                         <div className="col-span-full py-8 text-center bg-white rounded-lg border border-slate-100 italic text-[10px] text-slate-400 font-bold uppercase tracking-widest">
                            No se registran supervisiones en el sistema {(startDate && endDate) ? 'en el periodo seleccionado' : 'el día de hoy'}
                        </div>
                    )}
                </div>
            </div>

            {/* Filters & Table */}
            <div className="content-card overflow-hidden border-slate-200">
                <div className="p-4 md:p-6 border-b border-slate-100 flex flex-col md:flex-row gap-4 bg-slate-50/50">
                    <div className="relative flex-1">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
                        <input 
                            type="text" 
                            placeholder="Buscar por SOT, Técnico o Supervisor..." 
                            className="form-input pl-10 h-11 text-xs"
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                    </div>
                    <select 
                        className="form-input h-11 text-xs md:w-64 font-bold"
                        value={filterTecnico}
                        onChange={(e) => setFilterTecnico(e.target.value)}
                    >
                        <option value="">TODOS LOS TÉCNICOS</option>
                        {uniqueTecnicos.map(t => <option key={t} value={t}>{t.toUpperCase()}</option>)}
                    </select>
                </div>

                <div className="overflow-x-auto">
                    <table className="zebra-table min-w-[1000px]">
                        <thead>
                            <tr>
                                <th className="py-4 px-6 text-[9px] font-black uppercase tracking-widest">Fecha / Protocolo</th>
                                <th className="py-4 px-6 text-[9px] font-black uppercase tracking-widest">Técnico Auditado</th>
                                <th className="py-4 px-6 text-[9px] font-black uppercase tracking-widest">SOT Ref.</th>
                                <th className="py-4 px-6 text-[9px] font-black uppercase tracking-widest">Supervisor</th>
                                <th className="py-4 px-6 text-[9px] font-black uppercase tracking-widest">Estado</th>
                                <th className="py-4 px-6 text-[9px] font-black uppercase tracking-widest">Evidencias</th>
                                <th className="py-4 px-6 text-right text-[9px] font-black uppercase tracking-widest">Acciones</th>
                            </tr>
                        </thead>
                        <tbody>
                            {loading ? (
                                <tr>
                                    <td colSpan="7" className="py-20 text-center">
                                        <RefreshCw className="w-8 h-8 animate-spin mx-auto text-slate-300" />
                                        <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest mt-4">Sincronizando con base de datos...</p>
                                    </td>
                                </tr>
                            ) : error ? (
                                <tr>
                                    <td colSpan="7" className="py-20 text-center">
                                        <AlertCircle className="w-8 h-8 mx-auto text-red-400" />
                                        <p className="text-[10px] font-black text-red-500 uppercase tracking-widest mt-4">{error}</p>
                                        <button onClick={fetchData} className="mt-4 text-[9px] font-black bg-slate-800 text-white px-4 py-2 rounded uppercase tracking-widest">Reintentar</button>
                                    </td>
                                </tr>
                            ) : filteredReports.length === 0 ? (
                                <tr>
                                    <td colSpan="7" className="py-20 text-center text-slate-400 font-bold italic">
                                        No se encontraron registros de supervisión.
                                    </td>
                                </tr>
                            ) : filteredReports.map((report) => (
                                <tr key={report.id} className="group hover:bg-blue-50/30 transition-colors">
                                    <td className="py-4 px-6">
                                        <div className="flex flex-col">
                                            <span className="text-[11px] font-black text-slate-800">{formatDate(report.fecha_creacion)}</span>
                                            <span className="text-[9px] font-bold text-slate-400 uppercase tracking-tighter">ID: SUP-{report.id.toString().padStart(5, '0')}</span>
                                        </div>
                                    </td>
                                    <td className="py-4 px-6">
                                        <div className="flex items-center gap-2">
                                            <div className="w-7 h-7 rounded-full bg-slate-100 flex items-center justify-center text-[10px] font-black text-slate-500 border border-slate-200">
                                                {report.nombre_tecnico?.charAt(0)}
                                            </div>
                                            <span className="text-[11px] font-black text-slate-700 uppercase">{report.nombre_tecnico}</span>
                                        </div>
                                    </td>
                                    <td className="py-4 px-6">
                                        <span className="badge-std bg-slate-100 text-slate-600 font-mono text-[10px]">{report.sot || 'N/A'}</span>
                                    </td>
                                    <td className="py-4 px-6 text-[10px] font-bold text-slate-500 uppercase">
                                        {report.nombre_supervisor}
                                    </td>
                                    <td className="py-4 px-6">
                                        <span className="badge-std bg-emerald-100 text-emerald-700 text-[9px]">COMPLETADO</span>
                                    </td>
                                    <td className="py-4 px-6">
                                        <div className="flex gap-1">
                                            {report.fotos_urls?.slice(0, 3).map((url, i) => (
                                                <div key={i} className="w-8 h-8 rounded border border-slate-200 overflow-hidden bg-slate-100 group-hover:border-blue-300">
                                                    <img src={url} alt="" className="w-full h-full object-cover opacity-80 group-hover:opacity-100" />
                                                </div>
                                            ))}
                                            {report.fotos_urls?.length > 3 && (
                                                <div className="w-8 h-8 rounded border border-slate-200 flex items-center justify-center bg-slate-50 text-[9px] font-black text-slate-400">
                                                    +{report.fotos_urls.length - 3}
                                                </div>
                                            )}
                                        </div>
                                    </td>
                                    <td className="py-4 px-6 text-right">
                                        <button 
                                            onClick={() => setSelectedReport(report)}
                                            className="p-2 hover:bg-blue-100 rounded-full text-blue-600 transition-colors"
                                        >
                                            <Eye className="w-5 h-5" />
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* Detail Modal */}
            <AnimatePresence>
                {selectedReport && (
                    <div className="fixed inset-0 z-[1000] flex items-center justify-center p-4 md:p-6 bg-slate-900/60 backdrop-blur-md">
                        <motion.div 
                            initial={{ opacity: 0, y: 50 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: 50 }}
                            className="bg-white border border-slate-200 rounded-lg shadow-2xl w-full max-w-4xl overflow-hidden max-h-[90vh] flex flex-col"
                        >
                            {/* Modal Header */}
                            <div className="bg-slate-900 px-6 py-5 flex items-center justify-between text-white sticky top-0 z-10">
                                <div className="flex items-center gap-4">
                                    <div className="p-2 rounded-lg bg-blue-500/20">
                                        <ShieldCheck className="w-6 h-6 text-blue-400" />
                                    </div>
                                    <div>
                                        <h3 className="font-black text-sm md:text-base tracking-tight uppercase leading-none">REPORTE DE AUDITORÍA - ACTUALIZADO</h3>
                                        <p className="text-[9px] opacity-60 font-black uppercase tracking-widest mt-1">ID Transacción: {selectedReport.id}</p>
                                    </div>
                                </div>
                                <button onClick={() => setSelectedReport(null)} className="hover:bg-white/10 p-2 rounded-full transition-colors">
                                    <X className="w-6 h-6" />
                                </button>
                            </div>

                            {/* Modal Content */}
                            <div className="overflow-y-auto p-6 md:p-8 space-y-8">
                                <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                                    {/* Info Panel */}
                                    <div className="space-y-6">
                                        <div className="grid grid-cols-2 gap-4">
                                            <div className="bg-slate-50 p-4 rounded border border-slate-100">
                                                <p className="text-[9px] font-black text-slate-400 uppercase tracking-widest">Técnico Responsable</p>
                                                <p className="text-xs font-black text-slate-800 mt-1">{selectedReport.nombre_tecnico}</p>
                                            </div>
                                            <div className="bg-slate-50 p-4 rounded border border-slate-100">
                                                <p className="text-[9px] font-black text-slate-400 uppercase tracking-widest">Supervisor Auditor</p>
                                                <p className="text-xs font-black text-slate-800 mt-1">{selectedReport.nombre_supervisor}</p>
                                            </div>
                                        </div>

                                        {/* --- NUEVA ESTRUCTURA DE CHECKLIST CORREGIDA --- */}
                                        <div className="space-y-6">
                                            {/* Datos de la SOT */}
                                            {selectedReport.checklist?.datos_sot && (
                                                <div className="space-y-3">
                                                    <h5 className="text-[10px] font-black text-slate-400 uppercase tracking-widest flex items-center gap-2">
                                                        <Info className="w-4 h-4 text-blue-600" />
                                                        Información de Actividad
                                                    </h5>
                                                    <div className="bg-white border border-slate-200 rounded divide-y divide-slate-100 overflow-hidden">
                                                        {Object.entries(selectedReport.checklist.datos_sot).map(([key, val]) => (
                                                            <div key={key} className="flex items-center justify-between p-3 hover:bg-slate-50 transition-colors">
                                                                <span className="text-[10px] font-bold text-slate-500 uppercase">{key.replace(/_/g, ' ')}</span>
                                                                <span className="text-[10px] font-black text-slate-800 uppercase text-right pl-4">{val}</span>
                                                            </div>
                                                        ))}
                                                        {selectedReport.checklist.duracion_segundos && (
                                                            <div className="flex items-center justify-between p-3 bg-slate-50">
                                                                <span className="text-[10px] font-bold text-slate-500 uppercase">Duración de Supervisión</span>
                                                                <span className="text-[10px] font-black text-blue-700">{selectedReport.checklist.duracion_segundos} Segundos</span>
                                                            </div>
                                                        )}
                                                    </div>
                                                </div>
                                            )}

                                            {/* Respuestas del Checklist */}
                                            {selectedReport.checklist?.respuestas && (
                                                <div className="space-y-3">
                                                    <h5 className="text-[10px] font-black text-slate-400 uppercase tracking-widest flex items-center gap-2">
                                                        <ClipboardCheck className="w-4 h-4 text-emerald-600" />
                                                        Resultados de Evaluación
                                                    </h5>
                                                    <div className="bg-white border border-slate-200 rounded divide-y divide-slate-100 max-h-[400px] overflow-y-auto custom-scrollbar">
                                                        {Object.entries(selectedReport.checklist.respuestas).map(([key, val]) => {
                                                            const questionText = QUESTIONS_MAP[key.toLowerCase()] || key.replace(/_/g, ' ').toUpperCase();
                                                            return (
                                                                <div key={key} className="flex flex-col p-4 hover:bg-slate-50 transition-colors gap-2">
                                                                    <span className="text-[10px] font-medium text-slate-600 leading-relaxed uppercase">
                                                                        {questionText}
                                                                    </span>
                                                                    <div className="flex justify-end">
                                                                        <span className={`text-[9px] font-black px-3 py-1 rounded-full uppercase min-w-[80px] text-center ${
                                                                            val === 'si' ? 'bg-emerald-100 text-emerald-700 border border-emerald-200' : 
                                                                            val === 'no' ? 'bg-red-100 text-red-700 border border-red-200' : 'bg-slate-100 text-slate-600 border border-slate-200'
                                                                        }`}>
                                                                            {val}
                                                                        </span>
                                                                    </div>
                                                                </div>
                                                            );
                                                        })}
                                                    </div>
                                                </div>
                                            )}

                                            {/* Penalidades */}
                                            {(selectedReport.checklist?.penalidades?.length > 0 || selectedReport.checklist?.penalidad?.length > 0) && (
                                                <div className="space-y-3">
                                                    <h5 className="text-[10px] font-black text-red-500 uppercase tracking-widest flex items-center gap-2">
                                                        <AlertCircle className="w-4 h-4" />
                                                        Penalidades Detectadas
                                                    </h5>
                                                    <div className="bg-red-50 border border-red-100 rounded p-3 space-y-2">
                                                        {(selectedReport.checklist.penalidades || selectedReport.checklist.penalidad).map((p, i) => (
                                                            <div key={i} className="flex gap-2 text-[10px] text-red-700 font-bold uppercase leading-tight">
                                                                <span className="flex-shrink-0">•</span>
                                                                <span>{p}</span>
                                                            </div>
                                                        ))}
                                                    </div>
                                                </div>
                                            )}
                                        </div>

                                        <div className="space-y-3">
                                            <h5 className="text-[10px] font-black text-slate-400 uppercase tracking-widest flex items-center gap-2">
                                                <AlertCircle className="w-4 h-4 text-amber-600" />
                                                Observaciones del Supervisor
                                            </h5>
                                            <div className="bg-amber-50/50 border border-amber-100 p-4 rounded text-xs italic text-slate-700 leading-relaxed min-h-[80px]">
                                                {selectedReport.comentarios || "Sin observaciones adicionales registradas."}
                                            </div>
                                        </div>
                                        
                                        <div className="space-y-3">
                                            {selectedReport.latitud && (
                                                <>
                                                    <h5 className="text-[10px] font-black text-slate-400 uppercase tracking-widest flex items-center gap-2">
                                                        <MapPin className="w-4 h-4 text-red-500" />
                                                        Ubicación GPS
                                                    </h5>
                                                    <div className="w-full h-48 rounded-lg overflow-hidden border border-slate-200 shadow-sm">
                                                        <iframe 
                                                            src={`https://maps.google.com/maps?q=${selectedReport.latitud},${selectedReport.longitud}&hl=es&z=16&output=embed`}
                                                            width="100%" 
                                                            height="100%" 
                                                            frameBorder="0" 
                                                            style={{ border: 0 }} 
                                                            allowFullScreen="" 
                                                            aria-hidden="false" 
                                                            tabIndex="0"
                                                            title="Ubicación de Supervisión"
                                                        />
                                                    </div>
                                                </>
                                            )}
                                        </div>
                                    </div>

                                    {/* Photo Gallery */}
                                    <div className="space-y-4">
                                        <h5 className="text-[10px] font-black text-slate-400 uppercase tracking-widest flex items-center gap-2">
                                            <Camera className="w-4 h-4 text-emerald-600" />
                                            Evidencias Fotográficas ({selectedReport.fotos_urls?.length || 0})
                                        </h5>
                                        <div className="grid grid-cols-2 gap-3">
                                            {selectedReport.fotos_urls?.map((url, i) => (
                                                <a 
                                                    key={i} href={url} target="_blank" rel="noreferrer"
                                                    className="aspect-square rounded-lg border border-slate-200 overflow-hidden bg-slate-50 relative group"
                                                >
                                                    <img src={url} alt="" className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110" />
                                                    <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                                                        <ExternalLink className="w-6 h-6 text-white" />
                                                    </div>
                                                </a>
                                            ))}
                                            {(!selectedReport.fotos_urls || selectedReport.fotos_urls.length === 0) && (
                                                <div className="col-span-2 py-12 bg-slate-50 border-2 border-dashed border-slate-200 rounded-lg flex flex-col items-center justify-center text-slate-400">
                                                    <Camera className="w-10 h-10 mb-2 opacity-20" />
                                                    <p className="text-[10px] font-bold uppercase italic">Sin evidencias capturadas</p>
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                </div>
                            </div>

                            {/* Modal Footer */}
                            <div className="bg-slate-50 px-8 py-5 border-t border-slate-200 flex justify-end">
                                <button 
                                    onClick={() => setSelectedReport(null)}
                                    className="px-8 py-2.5 bg-slate-800 text-white rounded text-[10px] font-black uppercase tracking-widest hover:bg-black transition-all"
                                >
                                    Cerrar Vista
                                </button>
                            </div>
                        </motion.div>
                    </div>
                )}
            </AnimatePresence>
        </div>
    );
};

export default SupervisionReports;
