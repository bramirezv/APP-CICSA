import React, { useState, useEffect } from 'react';
import { 
  Camera, Search, Calendar, Filter, 
  User, Download, Image as ImageIcon,
  Clock, RefreshCw, Star, MapPin, ExternalLink
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { dataService } from '../services/api';

const TNPSMonitoring = () => {
    const [evidencias, setEvidencias] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [searchTerm, setSearchTerm] = useState('');
    const [selectedImage, setSelectedImage] = useState(null);

    useEffect(() => {
        fetchData();
    }, []);

    const fetchData = async () => {
        setLoading(true);
        setError(null);
        try {
            const res = await dataService.getTNPS();
            if (res.status === 'success') {
                setEvidencias(res.data || []);
            } else {
                setError(res.message || "Error al obtener datos");
            }
        } catch (err) {
            console.error("Error loading TNPS", err);
            setError("No se pudo conectar con el servidor.");
        } finally {
            setLoading(false);
        }
    };

    const formatDate = (dateStr) => {
        if (!dateStr) return '--';
        const date = new Date(dateStr.replace(/-/g, '/'));
        return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    };

    const filteredEvidencias = evidencias.filter(item => {
        const matchesSearch = item.sot?.toLowerCase().includes(searchTerm.toLowerCase()) || 
                             item.nombre_tecnico?.toLowerCase().includes(searchTerm.toLowerCase());
        return matchesSearch;
    });

    const getFullImageUrl = (url) => {
        if (url && url.startsWith('http')) return url;
        return `https://www.cicsamovil.com/api/uploads/tnps/${url}`;
    };

    return (
        <div className="space-y-6 pb-12">
            {/* Header */}
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b-2 border-slate-200 pb-5">
                <div>
                    <h2 className="text-xl md:text-2xl font-bold text-slate-800 tracking-tight flex items-center gap-3">
                        <Camera className="w-6 h-6 md:w-7 md:h-7 text-indigo-600" />
                        Monitoreo de Evidencias TNPS
                    </h2>
                    <p className="text-[10px] text-slate-500 font-bold uppercase tracking-widest mt-1">Verificación de Encuestas de Satisfacción en Campo</p>
                </div>
                <div className="flex gap-2">
                    <button 
                        onClick={fetchData}
                        className="p-2.5 bg-white border border-slate-200 rounded text-slate-600 hover:bg-slate-50 transition-all shadow-sm"
                    >
                        <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
                    </button>
                </div>
            </div>

            {/* Metrics */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                <div className="content-card p-5 border-l-4 border-l-indigo-600">
                    <p className="text-[9px] font-black text-slate-400 uppercase tracking-widest">Total Evidencias</p>
                    <div className="flex items-end justify-between mt-2">
                        <h4 className="text-2xl font-black text-slate-800">{evidencias.length}</h4>
                        <div className="p-2 bg-indigo-50 rounded text-indigo-600">
                            <ImageIcon className="w-5 h-5" />
                        </div>
                    </div>
                </div>
                <div className="content-card p-5 border-l-4 border-l-amber-500">
                    <p className="text-[9px] font-black text-slate-400 uppercase tracking-widest">Encuestas Hoy</p>
                    <div className="flex items-end justify-between mt-2">
                        <h4 className="text-2xl font-black text-slate-800">
                            {evidencias.filter(e => new Date(e.fecha_registro).toDateString() === new Date().toDateString()).length}
                        </h4>
                        <div className="p-2 bg-amber-50 rounded text-amber-600">
                            <Clock className="w-5 h-5" />
                        </div>
                    </div>
                </div>
            </div>

            {/* Filters */}
            <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm flex flex-col md:flex-row gap-4">
                <div className="flex-1 relative">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
                    <input 
                        type="text" 
                        placeholder="Buscar por SOT o Técnico..." 
                        className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded text-sm focus:ring-2 focus:ring-indigo-500 transition-all"
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                    />
                </div>
            </div>

            {/* Grid */}
            {loading ? (
                <div className="flex flex-col items-center justify-center py-20 bg-white rounded-2xl border border-slate-200 border-dashed">
                    <RefreshCw className="w-10 h-10 animate-spin text-indigo-500 mb-4" />
                    <p className="text-slate-500 font-bold uppercase text-[10px] tracking-widest">Cargando evidencias...</p>
                </div>
            ) : error ? (
                <div className="p-8 bg-red-50 rounded-2xl border border-red-100 text-center text-red-600 font-bold">
                    {error}
                </div>
            ) : filteredEvidencias.length === 0 ? (
                <div className="flex flex-col items-center justify-center py-20 bg-white rounded-2xl border border-slate-200 border-dashed">
                    <ImageIcon className="w-12 h-12 text-slate-300 mb-4" />
                    <p className="text-slate-500 font-bold uppercase text-[10px] tracking-widest">No se encontraron evidencias</p>
                </div>
            ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
                    {filteredEvidencias.map((item) => (
                        <motion.div 
                            layout
                            initial={{ opacity: 0, scale: 0.9 }}
                            animate={{ opacity: 1, scale: 1 }}
                            key={item.id}
                            className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-sm hover:shadow-xl transition-all group"
                        >
                            <div className="relative aspect-video overflow-hidden bg-slate-100 cursor-pointer" onClick={() => setSelectedImage(getFullImageUrl(item.foto_url))}>
                                <img 
                                    src={getFullImageUrl(item.foto_url)} 
                                    alt="Evidencia TNPS" 
                                    className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110"
                                />
                                <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                                    <ExternalLink className="text-white w-8 h-8" />
                                </div>
                                <div className="absolute top-2 left-2 px-2 py-1 bg-white/90 backdrop-blur rounded-lg text-[9px] font-black text-indigo-700 shadow-sm">
                                    SOT: {item.sot || 'SIN SOT'}
                                </div>
                            </div>
                            
                            <div className="p-4 space-y-3">
                                <div className="flex items-start gap-3">
                                    <div className="p-2 bg-slate-50 rounded-lg text-slate-500">
                                        <User className="w-4 h-4" />
                                    </div>
                                    <div>
                                        <p className="text-[8px] font-black text-slate-400 uppercase tracking-widest">Técnico</p>
                                        <p className="text-xs font-bold text-slate-700">{item.nombre_tecnico}</p>
                                    </div>
                                </div>

                                <div className="flex items-center gap-3">
                                    <div className="p-2 bg-slate-50 rounded-lg text-slate-500">
                                        <Clock className="w-4 h-4" />
                                    </div>
                                    <div>
                                        <p className="text-[8px] font-black text-slate-400 uppercase tracking-widest">Fecha Registro</p>
                                        <p className="text-[10px] font-bold text-slate-600">{formatDate(item.fecha_registro)}</p>
                                    </div>
                                </div>

                                {item.latitud && (
                                    <div className="pt-2 flex items-center gap-2">
                                        <a 
                                            href={`https://www.google.com/maps?q=${item.latitud},${item.longitud}`}
                                            target="_blank"
                                            rel="noopener noreferrer"
                                            className="flex items-center gap-1.5 text-[9px] font-black text-indigo-600 hover:text-indigo-800 transition-colors uppercase tracking-widest"
                                        >
                                            <MapPin className="w-3 h-3" /> Ver Ubicación
                                        </a>
                                    </div>
                                )}
                            </div>
                        </motion.div>
                    ))}
                </div>
            )}

            {/* Modal for full image */}
            <AnimatePresence>
                {selectedImage && (
                    <motion.div 
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        className="fixed inset-0 z-[200] flex items-center justify-center p-4 bg-slate-900/90 backdrop-blur-sm"
                        onClick={() => setSelectedImage(null)}
                    >
                        <motion.div 
                            initial={{ scale: 0.9, y: 20 }}
                            animate={{ scale: 1, y: 0 }}
                            className="relative max-w-4xl w-full"
                            onClick={(e) => e.stopPropagation()}
                        >
                            <button 
                                onClick={() => setSelectedImage(null)}
                                className="absolute -top-12 right-0 p-2 text-white hover:bg-white/10 rounded-full transition-all"
                            >
                                <RefreshCw className="w-6 h-6" />
                            </button>
                            <img 
                                src={selectedImage} 
                                alt="Full Evidencia" 
                                className="w-full h-auto max-h-[80vh] object-contain rounded-xl shadow-2xl border-4 border-white/10"
                            />
                        </motion.div>
                    </motion.div>
                )}
            </AnimatePresence>
        </div>
    );
};

export default TNPSMonitoring;
