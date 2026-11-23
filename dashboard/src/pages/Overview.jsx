import { 
  Users, Briefcase, Clock, CheckCircle2, 
  TrendingUp, Activity, ArrowUpRight, ArrowDownRight,
  Zap, Shield, Globe, Award, Calendar, ChevronRight
} from 'lucide-react';
import { motion } from 'framer-motion';
import React, { useState, useEffect, useMemo } from 'react';
import { dataService, authService } from '../services/api';
import { useData } from '../services/DataContext';

const Overview = () => {
    const { solicitudes, users, loading, refreshData, isRefreshing } = useData();
    const [admin, setAdmin] = useState(null);

    // Derived Metrics with useMemo
    const stats = useMemo(() => {
        const totalU = users.length;
        const availU = users.filter(u => u.estado === 'Disponible').length;
        const availP = totalU > 0 ? Math.round((availU / totalU) * 100) : 0;
        const actS = solicitudes.filter(s => s.estado === 'aceptada' || s.estado === 'pendiente').length;
        const compS = solicitudes.filter(s => s.estado === 'finalizada').length;

        return {
            total: totalU,
            available: availP,
            activeSots: actS,
            completedSots: compS
        };
    }, [users, solicitudes]);

    const recentActivity = useMemo(() => solicitudes.slice(0, 10), [solicitudes]);

    const regionalStats = useMemo(() => {
        const zones = ['NORTE', 'SUR', 'ESTE', 'OESTE'];
        return zones.map(zona => {
            const zoneSots = solicitudes.filter(s => s.zona === zona);
            const zoneCompleted = zoneSots.filter(s => s.estado === 'finalizada').length;
            const effectiveness = zoneSots.length > 0 ? Math.round((zoneCompleted / zoneSots.length) * 100) : 0;
            return { zona, effectiveness: effectiveness || 0 };
        });
    }, [solicitudes]);

    // For TOPS, we still need them from context or separate state if they are complex
    // But since they are part of users or solicitudes, we can keep them if we add them to context
    // For now, let's assume we might need a separate fetch for tops if they are highly specific
    // However, looking at the code, they come from getTopsPerformance()
    // Let's add tops to context in next step if needed, or keep it local for now if it's less frequent
    // Wait, let's just keep it local for now to minimize changes if strictly necessary, 
    // OR BETTER: Add tops to DataContext as well.
    const [tops, setTops] = useState([]);
    
    const fetchTops = async () => {
        const topsData = await dataService.getTopsPerformance();
        setTops(Array.isArray(topsData) ? topsData : []);
    };

    useEffect(() => {
        setAdmin(authService.getCurrentUser());
        fetchTops();
    }, []);

    const metrics = [
        { label: 'Personal en Red', value: stats.total, icon: Users, color: 'text-blue-600', sub: 'Registrados' },
        { label: 'Estado Disponibilidad', value: `${stats.available}%`, icon: Globe, color: 'text-emerald-600', sub: 'Listos en campo' },
        { label: 'Órdenes en Proceso', value: stats.activeSots, icon: Zap, color: 'text-amber-600', sub: 'Activas / Pendientes' },
        { label: 'Cierres de Servicio', value: stats.completedSots, icon: CheckCircle2, color: 'text-blue-800', sub: 'Total finalizado' },
    ];

    const formatTime = (dateStr) => {
        if (!dateStr) return '--:--';
        try {
            const date = new Date(dateStr.replace(/-/g, '/'));
            return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        } catch (e) {
            return dateStr;
        }
    };

    return (
        <div className="space-y-6 md:space-y-8">
            {/* Header / Title */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div className="text-left">
                    <h2 className="text-xl md:text-2xl font-bold text-slate-800 tracking-tight">Centro de Control</h2>
                    <p className="text-[10px] text-slate-500 font-bold uppercase tracking-widest mt-1">Monitoreo Real-time APPMOVIL</p>
                </div>
                <div className="flex items-center gap-3 bg-white px-3 py-2 rounded border border-slate-200 shadow-sm w-fit">
                    <div className={`w-1.5 h-1.5 rounded-full ${isRefreshing ? 'bg-blue-500 animate-pulse' : 'bg-emerald-500'}`} />
                    <span className="text-[9px] font-black text-slate-400 uppercase tracking-widest leading-none">
                        {isRefreshing ? 'Sincronizando...' : 'Sincronizado'}
                    </span>
                </div>
            </div>

            {/* Metrics Grid */}
            <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4 md:gap-6">
                {metrics.map((m, i) => (
                    <div key={i} className="content-card flex items-center gap-4 py-4 px-5">
                        <div className={`w-10 h-10 md:w-12 md:h-12 rounded border border-slate-100 flex items-center justify-center bg-slate-50 ${m.color}`}>
                            <m.icon className="w-5 h-5 md:w-6 md:h-6" />
                        </div>
                        <div>
                            <p className="text-[9px] md:text-[10px] font-bold text-slate-400 uppercase tracking-widest leading-none">{m.label}</p>
                            <h3 className="text-xl md:text-2xl font-black text-slate-800 mt-1 tracking-tighter">{loading ? '...' : m.value}</h3>
                            <p className="text-[8px] md:text-[9px] text-slate-400 font-black tracking-tighter uppercase mt-0.5">{m.sub}</p>
                        </div>
                    </div>
                ))}
            </div>

            {/* Main Sections */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 md:gap-8 pb-10">
                
                {/* Recent Activity Section */}
                <div className="lg:col-span-2 space-y-4">
                    <div className="flex items-center justify-between border-b-2 border-slate-200 pb-3">
                        <h4 className="font-bold text-xs md:text-sm text-slate-800 flex items-center gap-2 tracking-widest uppercase">
                            <Activity className="w-4 h-4 text-blue-800" />
                            Actividad SOT en Campo
                        </h4>
                        <button onClick={refreshData} disabled={isRefreshing} className="text-[9px] font-black text-blue-800 hover:bg-white hover:shadow-sm px-2 py-1 rounded transition-all uppercase tracking-widest bg-slate-50 border border-slate-200">
                             {isRefreshing ? 'Cargando...' : 'Actualizar'}
                        </button>
                    </div>
                    
                    <div className="border border-slate-200 rounded-sm bg-white overflow-hidden shadow-sm">
                        <div className="overflow-x-auto">
                            <table className="zebra-table min-w-[500px]">
                                <thead>
                                    <tr>
                                        <th className="w-32">ORDEN SOT</th>
                                        <th>CÓDIGO TÉCNICO</th>
                                        <th>ZONA RED</th>
                                        <th className="text-right">HORA EMISIÓN</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {!loading && recentActivity.length > 0 ? recentActivity.map((sol) => (
                                        <tr key={sol.id}>
                                            <td className="font-black text-blue-800">SOT-{sol.sot}</td>
                                            <td className="font-bold text-slate-700 text-xs">{sol.tecnico || 'N/A'}</td>
                                            <td><span className="badge-std badge-blue text-[9px]">S- {sol.zona}</span></td>
                                            <td className="text-right text-slate-500 font-black text-[9px]">{formatTime(sol.fecha_creacion)}</td>
                                        </tr>
                                    )) : !loading && (
                                        <tr>
                                            <td colSpan="4" className="py-20 text-center text-xs font-bold text-slate-300 uppercase italic">Sin registros en bolsa</td>
                                        </tr>
                                    )}
                                </tbody>
                            </table>
                        </div>
                        {loading && (
                            <div className="py-20 flex flex-col items-center justify-center gap-3">
                                <div className="w-5 h-5 border-3 border-blue-800 border-t-transparent rounded-full animate-spin" />
                                <span className="text-[9px] font-black text-slate-400 uppercase tracking-widest">Sincronizando...</span>
                            </div>
                        )}
                    </div>
                </div>

                {/* Side Column: Regional & Tops */}
                <div className="space-y-8">
                    {/* Regional Dispatch */}
                    <div className="space-y-4">
                        <div className="flex items-center justify-between border-b-2 border-slate-200 pb-3">
                            <h4 className="font-bold text-xs md:text-sm text-slate-800 flex items-center gap-2 tracking-widest uppercase">
                                <Briefcase className="w-4 h-4 text-blue-800" />
                                Cumplimiento / Zonas
                            </h4>
                        </div>
                        <div className="content-card space-y-5 py-6">
                            {regionalStats.map((item) => (
                                <div key={item.zona} className="space-y-2">
                                    <div className="flex justify-between items-center text-[9px] font-black uppercase tracking-widest text-slate-400">
                                        <span>SECTOR {item.zona}</span>
                                        <span className="text-slate-900">{item.effectiveness}%</span>
                                    </div>
                                    <div className="h-2 w-full bg-slate-50 border border-slate-200 rounded-sm overflow-hidden">
                                        <motion.div 
                                            initial={{ width: 0 }} animate={{ width: `${item.effectiveness}%` }}
                                            className={`h-full ${item.effectiveness > 70 ? 'bg-emerald-500 shadow-[0_0_8px_#10b98133]' : item.effectiveness > 40 ? 'bg-blue-600' : 'bg-amber-500'}`} 
                                        />
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>

                    {/* Performance Tops Section */}
                    <div className="space-y-4">
                        <div className="flex items-center justify-between border-b-2 border-slate-200 pb-3">
                            <h4 className="font-bold text-xs md:text-sm text-slate-800 flex items-center gap-2 tracking-widest uppercase">
                                <Award className="w-4 h-4 text-blue-800" />
                                TOP Desempeño Satelital
                            </h4>
                        </div>
                        <div className="content-card px-0 py-3 overflow-hidden">
                            {!loading && tops.length > 0 ? (
                                <div className="space-y-1">
                                    {tops.map((sat, index) => (
                                        <div key={sat.id} className="flex items-center gap-3 px-5 py-3 hover:bg-slate-50 transition-colors border-b border-slate-50 last:border-0">
                                            <div className="relative">
                                                <div className={`w-8 h-8 rounded-full flex items-center justify-center font-black text-[10px] border-2 shadow-sm
                                                    ${index === 0 ? 'bg-amber-50 border-amber-400 text-amber-700' : 
                                                      index === 1 ? 'bg-slate-100 border-slate-300 text-slate-600' : 
                                                      index === 2 ? 'bg-orange-50 border-orange-300 text-orange-700' : 
                                                      'bg-white border-slate-100 text-slate-400'}`}>
                                                    {index + 1}
                                                </div>
                                                {index < 3 && (
                                                    <div className="absolute -top-1 -right-1">
                                                        <Zap className={`w-3.5 h-3.5 ${index === 0 ? 'text-amber-400' : 'text-slate-300'}`} fill="currentColor" />
                                                    </div>
                                                )}
                                            </div>
                                            <div className="flex-1">
                                                <div className="flex justify-between items-end mb-1">
                                                    <span className="text-[11px] font-black text-slate-800 uppercase truncate max-w-[120px]">{sat.nombre}</span>
                                                    <span className="text-[10px] font-black text-blue-800">{sat.total} <span className="text-[8px] text-slate-400 uppercase tracking-tighter">SOTs</span></span>
                                                </div>
                                                <div className="h-1.5 w-full bg-slate-50 rounded-full overflow-hidden">
                                                    <motion.div 
                                                        initial={{ width: 0 }} 
                                                        animate={{ width: `${(sat.total / (tops[0]?.total || 1)) * 100}%` }}
                                                        className={`h-full ${index === 0 ? 'bg-blue-800' : 'bg-blue-400'}`}
                                                    />
                                                </div>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            ) : !loading && (
                                <div className="py-20 text-center text-[9px] font-black text-slate-300 uppercase italic tracking-widest">Sin registros históricos</div>
                            )}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Overview;
