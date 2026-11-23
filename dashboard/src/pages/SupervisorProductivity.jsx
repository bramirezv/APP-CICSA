import React, { useState, useEffect } from 'react';
import { 
  TrendingUp, Users, RefreshCw, ChevronRight, 
  Search, Shield, Activity, Calendar
} from 'lucide-react';
import { motion } from 'framer-motion';
import { dataService } from '../services/api';

const SupervisorProductivity = () => {
    const [stats, setStats] = useState([]);
    const [loading, setLoading] = useState(true);
    const [isRefreshing, setIsRefreshing] = useState(false);
    const [searchTerm, setSearchTerm] = useState('');

    const fetchStats = async () => {
        setIsRefreshing(true);
        try {
            const res = await dataService.getSupervisorProductivity();
            if (res.status === 'success') {
                setStats(res.data || []);
            }
        } catch (error) {
            console.error("Error fetching productivity:", error);
        } finally {
            setLoading(false);
            setIsRefreshing(false);
        }
    };

    useEffect(() => {
        fetchStats();
    }, []);

    const filteredStats = stats.filter(s => 
        s.supervisor_nombre.toLowerCase().includes(searchTerm.toLowerCase())
    );

    return (
        <div className="space-y-6 md:space-y-8">
            {/* Header */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div className="text-left">
                    <h2 className="text-xl md:text-2xl font-bold text-slate-800 tracking-tight">Productividad de Supervisores</h2>
                    <p className="text-[10px] text-slate-500 font-bold uppercase tracking-widest mt-1">Control de supervisiones diarias en tiempo real</p>
                </div>
                
                <div className="flex items-center gap-2">
                    <div className="bg-white border border-slate-200 rounded-lg px-3 py-1.5 flex items-center gap-2 shadow-sm">
                        <Calendar className="w-4 h-4 text-blue-800" />
                        <span className="text-xs font-bold text-slate-700">{new Date().toLocaleDateString('es-PE', { day: 'numeric', month: 'long', year: 'numeric' })}</span>
                    </div>
                    <button 
                        onClick={fetchStats}
                        disabled={isRefreshing}
                        className={`p-2 bg-white border border-slate-200 rounded-lg shadow-sm hover:bg-slate-50 transition-all ${isRefreshing ? 'animate-spin' : ''}`}
                    >
                        <RefreshCw className="w-4 h-4 text-blue-800" />
                    </button>
                </div>
            </div>

            {/* Top Cards Summary */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div className="content-card flex items-center gap-4 py-4 px-6 border-l-4 border-blue-600">
                    <div className="w-12 h-12 rounded bg-blue-50 flex items-center justify-center text-blue-600">
                        <Shield className="w-6 h-6" />
                    </div>
                    <div>
                        <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Supervisores Activos</p>
                        <h3 className="text-2xl font-black text-slate-800 tracking-tighter">{stats.length}</h3>
                    </div>
                </div>
                
                <div className="content-card flex items-center gap-4 py-4 px-6 border-l-4 border-emerald-500">
                    <div className="w-12 h-12 rounded bg-emerald-50 flex items-center justify-center text-emerald-600">
                        <TrendingUp className="w-6 h-6" />
                    </div>
                    <div>
                        <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Total Supervisiones Hoy</p>
                        <h3 className="text-2xl font-black text-slate-800 tracking-tighter">
                            {stats.reduce((acc, curr) => acc + curr.total_supervisiones, 0)}
                        </h3>
                    </div>
                </div>

                <div className="content-card flex items-center gap-4 py-4 px-6 border-l-4 border-amber-500">
                    <div className="w-12 h-12 rounded bg-amber-50 flex items-center justify-center text-amber-600">
                        <Activity className="w-6 h-6" />
                    </div>
                    <div>
                        <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Promedio por Sup.</p>
                        <h3 className="text-2xl font-black text-slate-800 tracking-tighter">
                            {stats.length > 0 ? (stats.reduce((acc, curr) => acc + curr.total_supervisiones, 0) / stats.length).toFixed(1) : 0}
                        </h3>
                    </div>
                </div>
            </div>

            {/* List and Search */}
            <div className="space-y-4">
                <div className="flex items-center justify-between border-b-2 border-slate-200 pb-3">
                    <h4 className="font-bold text-xs md:text-sm text-slate-800 flex items-center gap-2 tracking-widest uppercase">
                        <Users className="w-4 h-4 text-blue-800" />
                        Desempeño Individual
                    </h4>
                    <div className="relative">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
                        <input 
                            type="text" 
                            placeholder="Buscar supervisor..." 
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            className="pl-9 pr-4 py-1.5 bg-white border border-slate-200 rounded-lg text-xs outline-none focus:ring-2 focus:ring-blue-500/20 w-48 md:w-64"
                        />
                    </div>
                </div>

                {loading ? (
                    <div className="py-20 flex flex-col items-center justify-center gap-3">
                        <div className="w-8 h-8 border-4 border-blue-800 border-t-transparent rounded-full animate-spin" />
                        <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest">Cargando productividad...</span>
                    </div>
                ) : filteredStats.length > 0 ? (
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        {filteredStats.map((item, index) => (
                            <motion.div 
                                key={item.supervisor_id}
                                initial={{ opacity: 0, y: 10 }}
                                animate={{ opacity: 1, y: 0 }}
                                transition={{ delay: index * 0.05 }}
                                className="content-card overflow-hidden group hover:shadow-md transition-all"
                            >
                                <div className="p-5 flex items-start gap-4">
                                    <div className="w-12 h-12 rounded-full bg-slate-100 flex items-center justify-center text-slate-500 font-bold border-2 border-white shadow-sm shrink-0">
                                        {item.supervisor_nombre.charAt(0).toUpperCase()}
                                    </div>
                                    <div className="flex-1 min-w-0">
                                        <div className="flex justify-between items-start">
                                            <div>
                                                <h5 className="font-black text-slate-800 text-sm uppercase truncate">{item.supervisor_nombre}</h5>
                                                <p className="text-[10px] font-bold text-slate-400">ID: {item.supervisor_id}</p>
                                            </div>
                                            <div className="text-right">
                                                <span className="text-2xl font-black text-blue-800 leading-none">{item.total_supervisiones}</span>
                                                <p className="text-[8px] font-black text-slate-400 uppercase tracking-tighter">Supervisiones</p>
                                            </div>
                                        </div>
                                        
                                        <div className="mt-4 pt-4 border-t border-slate-100">
                                            <p className="text-[9px] font-black text-slate-500 uppercase tracking-widest mb-2 flex items-center gap-1">
                                                <ChevronRight className="w-3 h-3 text-blue-600" />
                                                Técnicos supervisados hoy:
                                            </p>
                                            <div className="flex flex-wrap gap-1.5">
                                                {item.tecnicos_supervisados ? item.tecnicos_supervisados.split(', ').map((tec, i) => (
                                                    <span key={i} className="px-2 py-0.5 bg-slate-50 border border-slate-200 rounded text-[10px] font-bold text-slate-600">
                                                        {tec}
                                                    </span>
                                                )) : (
                                                    <span className="text-[10px] font-bold text-slate-300 italic">Ninguno todavía</span>
                                                )}
                                            </div>
                                        </div>
                                    </div>
                                </div>
                                <div className="h-1 w-full bg-slate-50">
                                    <motion.div 
                                        initial={{ width: 0 }}
                                        animate={{ width: `${(item.total_supervisiones / (stats[0]?.total_supervisiones || 1)) * 100}%` }}
                                        className="h-full bg-blue-600"
                                    />
                                </div>
                            </motion.div>
                        ))}
                    </div>
                ) : (
                    <div className="py-20 text-center border-2 border-dashed border-slate-200 rounded-xl bg-white">
                        <div className="flex flex-col items-center gap-3">
                            <Users className="w-10 h-10 text-slate-200" />
                            <p className="text-[10px] font-black text-slate-300 uppercase tracking-widest">No se encontraron registros para el filtro aplicado</p>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};

export default SupervisorProductivity;
