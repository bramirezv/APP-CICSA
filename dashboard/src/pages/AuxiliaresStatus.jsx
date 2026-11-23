import React, { useState, useEffect } from 'react';
import { 
    Activity, Clock, MapPin, TrendingUp, Zap, RefreshCw, AlertTriangle, Box
} from 'lucide-react';
import { motion } from 'framer-motion';
import { useData } from '../services/DataContext';

const AuxiliaresStatus = () => {
    const { solicitudes, users, loading, refreshData, isRefreshing } = useData();

    // Priority sorting: Ocupado > Almorzando > Disponible > Fuera de línea
    // Filtramos para obtener a los auxiliares
    const auxiliares = users.filter(u => u.rol === 'auxiliar' || u.rol === 'usuario').sort((a, b) => {
        const priority = { 'Ocupado': 1, 'Almorzando': 2, 'Disponible': 3, 'Fuera de línea': 4 };
        const pA = priority[a.estado] || 5;
        const pB = priority[b.estado] || 5;
        return pA - pB;
    });

    return (
        <div className="space-y-8">
            {/* Header Section */}
            <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4 border-b-2 border-slate-200 pb-5">
                <div>
                    <h2 className="text-xl md:text-2xl font-bold text-slate-800 tracking-tight flex items-center gap-3">
                        <Box className="w-6 h-6 md:w-7 md:h-7 text-purple-800" />
                        Monitoreo de Estado de Auxiliares
                    </h2>
                    <p className="text-[10px] text-slate-500 font-bold uppercase tracking-widest mt-1">Control de Disponibilidad y Desempeño Operativo</p>
                </div>
                
                <div className="flex items-center gap-3">
                     <div className="flex items-center gap-2 px-4 py-2 bg-emerald-50 text-emerald-600 rounded border border-emerald-100 shadow-sm">
                         <div className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
                         <span className="text-[10px] font-black uppercase tracking-wider">Disponibles: {auxiliares.filter(a => a.estado === 'Disponible').length}</span>
                     </div>
                     <button 
                        onClick={refreshData}
                        className="p-2.5 bg-white border border-slate-200 rounded text-slate-600 hover:text-purple-800 hover:border-purple-200 transition-all shadow-sm"
                        title="Refrescar Datos"
                     >
                        <RefreshCw className={`w-4 h-4 ${loading || isRefreshing ? 'animate-spin' : ''}`} />
                     </button>
                </div>
            </div>

            {/* Auxiliares Grid */}
            {loading && auxiliares.length === 0 ? (
                <div className="py-40 flex flex-col items-center justify-center gap-3 text-slate-400">
                    <RefreshCw className="w-8 h-8 animate-spin opacity-20" />
                    <span className="text-xs font-black uppercase tracking-[0.3em] opacity-40">Sincronizando Auxiliares...</span>
                </div>
            ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6 pb-20">
                    {auxiliares.map(aux => {
                        // En la lógica de la BD, si el auxiliar crea una plantilla, su nombre va a nombre_satelite
                        // Pero para ver lo que está haciendo AHORA, buscamos en las solicitudes 
                        // donde él sea el creador o donde él sea el asignado.
                        // Para simplificar, usamos su ID si fuera asignado, o su nombre si es el auxiliar creador.
                        const userSolicitudes = solicitudes.filter(s => s.tipo === 'auxiliar' && (s.id_satelite === aux.id || s.nombre_satelite === aux.nombre));
                        const completedCount = userSolicitudes.filter(s => s.estado?.toLowerCase() === 'finalizada' || s.estado?.toLowerCase() === 'completada').length;
                        const totalCount = userSolicitudes.length;
                        const activeSot = userSolicitudes.find(s => 
                            ['aceptada', 'en_camino', 'llegado', 'corregir'].includes(s.estado?.toLowerCase())
                        );

                        return (
                            <motion.div 
                                key={aux.id}
                                layout
                                initial={{ opacity: 0, scale: 0.95 }}
                                animate={{ opacity: 1, scale: 1 }}
                                className="bg-white border-2 border-slate-100 rounded-lg p-6 shadow-sm hover:shadow-xl hover:border-purple-100 transition-all group relative overflow-hidden"
                            >
                                {/* Background Accent Icon */}
                                <Box className="absolute -right-4 -bottom-4 w-24 h-24 text-slate-50/50 group-hover:text-purple-50/50 transition-colors" />

                                <div className="flex items-start justify-between mb-5 relative z-10">
                                    <div className="flex items-center gap-4">
                                        <div className="w-12 h-12 rounded bg-slate-50 flex items-center justify-center text-slate-400 font-black text-lg border border-slate-100 shadow-inner group-hover:bg-purple-800 group-hover:text-white group-hover:border-purple-800 transition-all duration-300">
                                            {(aux.nombre || "A").charAt(0).toUpperCase()}
                                        </div>
                                        <div>
                                            <h5 className="text-[13px] font-black text-slate-800 uppercase leading-none tracking-tight group-hover:text-purple-900">{aux.nombre || 'Sin Nombre'}</h5>
                                            <div className="flex items-center gap-1.5 mt-2 font-bold text-[10px] text-slate-400 uppercase tracking-widest">
                                                <MapPin className="w-3 h-3 text-red-500" />
                                                <span>{aux.zona || 'SIN ZONA'}</span>
                                            </div>
                                        </div>
                                    </div>
                                    <div className="flex flex-col items-end gap-2">
                                        <span className={`px-2.5 py-1 rounded text-[9px] font-black uppercase tracking-tighter border-2 shadow-sm
                                            ${aux.estado === 'Disponible' ? 'bg-emerald-50 text-emerald-600 border-emerald-100/50' : 
                                              aux.estado === 'Ocupado' ? 'bg-blue-50 text-blue-800 border-blue-100/50' : 
                                              aux.estado === 'Almorzando' ? 'bg-orange-50 text-orange-700 border-orange-100/50' : 
                                              'bg-slate-50 text-slate-400 border-slate-100'}`}>
                                            {aux.estado}
                                        </span>
                                    </div>
                                </div>

                                <div className="space-y-5 relative z-10">
                                    {activeSot ? (
                                        <motion.div 
                                            initial={{ x: -10, opacity: 0 }} animate={{ x: 0, opacity: 1 }}
                                            className="border-l-[3px] border-purple-600 bg-slate-50/80 p-3.5 rounded-r-md space-y-2 shadow-sm border border-slate-200"
                                        >
                                            <div className="flex items-center justify-between mb-1.5 border-b border-slate-100 pb-1.5">
                                                <div className="flex items-center gap-1.5">
                                                    <Zap className="w-3 h-3 text-purple-600" />
                                                    <span className="text-[9px] font-black text-slate-400 uppercase tracking-widest">En Operación</span>
                                                </div>
                                                <span className="text-[9px] font-black text-purple-800">SOT-{activeSot.sot}</span>
                                            </div>
                                            
                                            <div className="space-y-1.5">
                                                <p className="text-[10px] leading-tight text-slate-600">
                                                    <span className="font-black text-slate-800 uppercase text-[8px]">Motivo:</span> {activeSot.motivo}
                                                </p>
                                                <p className="text-[10px] leading-tight text-slate-600">
                                                    <span className="font-black text-slate-800 uppercase text-[8px]">Técnico Asistido:</span> {activeSot.tecnico || 'N/A'}
                                                </p>
                                                <p className="text-[10px] leading-tight text-slate-600">
                                                    <span className="font-black text-slate-800 uppercase text-[8px]">Supervisor:</span> {activeSot.nombre_supervisor || 'N/A'}
                                                </p>
                                            </div>

                                            <div className="pt-2 flex items-center justify-between">
                                                <span className="text-[9px] font-black text-purple-600 uppercase tracking-tighter bg-purple-50 px-2 py-0.5 rounded border border-purple-100">
                                                    {activeSot.estado?.replace('_', ' ')}
                                                </span>
                                                <div className="w-1.5 h-1.5 rounded-full bg-purple-500 animate-pulse shadow-[0_0_5px_rgba(168,85,247,0.5)]" />
                                            </div>
                                        </motion.div>
                                    ) : (
                                        <div className="bg-slate-50/80 rounded-md p-5 border border-dashed border-slate-200 flex flex-col items-center justify-center py-10 group-hover:bg-slate-100 transition-colors">
                                            <div className="w-12 h-12 rounded-full bg-white flex items-center justify-center border border-slate-100 mb-4 shadow-sm group-hover:scale-110 transition-transform">
                                                <Clock className="w-6 h-6 text-slate-200" />
                                            </div>
                                            <span className="text-[10px] font-black text-slate-300 uppercase tracking-[0.2em] italic">A la espera de Tareas</span>
                                        </div>
                                    )}

                                    <div className="flex items-center justify-between text-[10px] font-black uppercase tracking-widest pt-3 border-t border-slate-100">
                                        <div className="flex items-center gap-2 text-slate-400">
                                            <TrendingUp className="w-3.5 h-3.5 text-emerald-500" />
                                            <span className="text-slate-700">{completedCount} <span className="opacity-40">/ {totalCount}</span></span>
                                        </div>
                                        <div className="text-slate-400 flex items-center gap-2">
                                            <RefreshCw className="w-3.5 h-3.5 opacity-50" />
                                            <span>{(aux.ultima_conexion && aux.ultima_conexion !== 'Nunca') ? aux.ultima_conexion.split(' ')[1].substring(0, 5) : '--:--'}</span>
                                        </div>
                                    </div>
                                </div>

                                {aux.estado === 'Fuera de línea' && (
                                    <div className="absolute inset-0 bg-white/40 backdrop-blur-[1px] flex items-center justify-center z-20">
                                         <div className="bg-white/90 px-4 py-2 rounded-full border border-slate-200 shadow-sm flex items-center gap-2">
                                             <AlertTriangle className="w-3 h-3 text-amber-500" />
                                             <span className="text-[9px] font-black text-slate-400 uppercase tracking-widest">Inactivo</span>
                                         </div>
                                    </div>
                                )}
                            </motion.div>
                        );
                    })}
                </div>
            )}
        </div>
    );
};

export default AuxiliaresStatus;
