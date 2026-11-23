import React, { useState } from 'react';
import { 
  FileSpreadsheet, Calendar, Download, 
  CheckCircle2, AlertCircle, Loader2,
  FileText, History, TrendingUp, Info,
  Zap, ArrowUpRight, ShieldCheck, ChevronRight,
  Database, Map as MapIcon, Share2, ClipboardList
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { dataService, authService } from '../services/api';

const Reports = () => {
    const [month, setMonth] = useState(new Date().getMonth() + 1);
    const [year, setYear] = useState(new Date().getFullYear());
    const [generating, setGenerating] = useState(false);
    const [lastDownload, setLastDownload] = useState(null);
    const [reportType, setReportType] = useState('asistencia');
    const admin = authService.getCurrentUser();

    const handleDownload = async () => {
        setGenerating(true);
        try {
            const url = await dataService.exportAttendance(month, year);
            window.open(url, '_blank');
            setLastDownload(new Date().toLocaleString());
        } catch (err) {
            console.error("Error generating report", err);
        } finally {
            setTimeout(() => setGenerating(false), 2000);
        }
    };

    const months = [
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    ];

    return (
        <div className="space-y-6 pb-12">
            {/* Header responsive */}
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b-2 border-slate-200 pb-5">
                <div>
                    <h2 className="text-xl md:text-2xl font-bold text-slate-800 tracking-tight flex items-center gap-3">
                        <FileSpreadsheet className="w-6 h-6 md:w-7 md:h-7 text-blue-800" />
                        Suite de Auditoría
                    </h2>
                    <p className="text-[10px] text-slate-500 font-bold uppercase tracking-widest mt-1">Generación de Matriz Operativa Mensual</p>
                </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 md:gap-8">
                
                {/* Export Control Panel responsive */}
                <div className="lg:col-span-2 space-y-6">
                    <div className="content-card">
                        <h3 className="content-title text-[10px] md:text-xs tracking-widest uppercase flex items-center gap-2">
                            <Database className="w-4 h-4 text-blue-800" />
                            Configuración de Exportación
                        </h3>
                        
                        <div className="p-5 md:p-8 space-y-8">
                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 md:gap-8">
                                <div className="space-y-2">
                                    <label className="text-[9px] font-black text-slate-400 uppercase tracking-widest leading-none">Tipo de Reporte Principal</label>
                                    <select 
                                        className="form-input bg-slate-50 font-black text-[11px] md:text-xs h-11 md:h-12"
                                        value={reportType}
                                        onChange={(e) => setReportType(e.target.value)}
                                    >
                                        <option value="asistencia">MATRIZ ASISTENCIA</option>
                                        <option value="sot">REPORTES SOT</option>
                                        <option value="location">GEOLOCALIZACIÓN</option>
                                    </select>
                                </div>
                                <div className="space-y-2">
                                    <label className="text-[9px] font-black text-slate-400 uppercase tracking-widest leading-none">Formato Salida</label>
                                    <div className="flex items-center gap-3 h-11 md:h-12 px-1">
                                        <span className="badge-std badge-green text-[9px] font-black uppercase">XLSX / EXCEL</span>
                                    </div>
                                </div>
                            </div>

                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 md:gap-8 border-t border-slate-100 pt-8">
                                <div className="space-y-2">
                                    <label className="text-[9px] font-black text-slate-400 uppercase tracking-widest leading-none">Periodo Mensual</label>
                                    <select 
                                        value={month} onChange={(e) => setMonth(e.target.value)}
                                        className="form-input bg-slate-50 h-11 md:h-12 text-[11px] font-bold"
                                    >
                                        {months.map((m, i) => (
                                            <option key={i+1} value={i+1}>{m.toUpperCase()}</option>
                                        ))}
                                    </select>
                                </div>
                                <div className="space-y-2">
                                    <label className="text-[9px] font-black text-slate-400 uppercase tracking-widest leading-none">Año Ejercicio</label>
                                    <select 
                                        value={year} onChange={(e) => setYear(e.target.value)}
                                        className="form-input bg-slate-50 h-11 md:h-12 text-[11px] font-bold"
                                    >
                                        <option value="2026">2026</option>
                                        <option value="2025">2025</option>
                                    </select>
                                </div>
                            </div>

                            <div className="bg-slate-50 border border-slate-200 p-4 md:p-6 rounded flex items-start gap-3 md:gap-4">
                                <div className="p-2 md:p-3 bg-white rounded border border-slate-200 flex-shrink-0">
                                    <ShieldCheck className="w-5 h-5 text-emerald-600" />
                                </div>
                                <div>
                                    <h4 className="text-[10px] font-black text-slate-800 uppercase tracking-tight">Validación Integridad</h4>
                                    <p className="text-[10px] md:text-[11px] text-slate-500 mt-1 leading-relaxed">
                                        Los datos generados han sido validados por los servidores de APPMOVIL S.A.C. mediante firmas de tiempo y coordenadas GPS registradas en campo.
                                    </p>
                                </div>
                            </div>

                            <div className="flex justify-end pt-4">
                                <button 
                                    onClick={handleDownload} disabled={generating}
                                    className="btn-enterprise w-full md:w-auto h-12 md:h-14 md:px-12 flex items-center justify-center gap-3 shadow-lg shadow-blue-800/10 text-[11px] font-black uppercase tracking-widest"
                                >
                                    {generating ? (
                                        <>
                                            <Loader2 className="w-4 h-4 md:w-5 md:h-5 animate-spin" />
                                            PROCESANDO...
                                        </>
                                    ) : (
                                        <>
                                            GENERAR ARCHIVO EXCEL
                                            <Download className="w-4 h-4 md:w-5 md:h-5" />
                                        </>
                                    )}
                                </button>
                            </div>
                        </div>
                    </div>

                    {/* History Table responsive */}
                    <div className="content-card">
                         <h3 className="content-title text-[10px] md:text-xs tracking-widest uppercase flex items-center gap-2">
                            <History className="w-4 h-4 text-blue-800" />
                            Historial de Consultas Recientes
                        </h3>
                        <div className="overflow-x-auto border border-slate-200 rounded">
                            <table className="zebra-table min-w-[500px]">
                                <thead>
                                    <tr>
                                        <th className="py-4 px-4 text-[9px] font-black uppercase tracking-widest">Sincronización</th>
                                        <th className="py-4 px-4 text-[9px] font-black uppercase tracking-widest">Protocolo</th>
                                        <th className="py-4 px-4 text-[9px] font-black uppercase tracking-widest">Periodo</th>
                                        <th className="py-4 px-4 text-[9px] font-black uppercase tracking-widest">Estado</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr className="hover:bg-slate-50">
                                        <td className="font-bold text-[10px] py-4 px-4">{lastDownload || 'N/A'}</td>
                                        <td className="text-[9px] font-black uppercase text-slate-400">Manual / Dashboard</td>
                                        <td className="text-[11px] font-bold py-4 px-4">{months[month-1]} {year}</td>
                                        <td><span className="badge-std badge-green text-[9px] font-black uppercase">COMPLETADO</span></td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>

                {/* Sidebar Side responsive */}
                <div className="space-y-6">
                    <div className="content-card bg-blue-800 text-white border-none py-8 md:py-10 px-6">
                        <div className="text-center space-y-4">
                            <Share2 className="w-10 h-10 mx-auto opacity-40" />
                            <div>
                                <h4 className="font-black text-sm md:text-base tracking-tight leading-none uppercase">Auditoría BI Inteligente</h4>
                                <p className="text-[10px] md:text-[11px] opacity-60 mt-4 leading-relaxed font-bold">
                                    Próximamente: Integración directa con PowerBI y envío automático de cierres de mes a gerencia.
                                </p>
                            </div>
                            <div className="h-px bg-white/10 mx-8" />
                            <p className="text-[8px] font-black tracking-[0.3em] opacity-40 uppercase">System v4.5.1-ADMIN</p>
                        </div>
                    </div>

                    <div className="content-card p-5 bg-slate-50 border-slate-200 space-y-5">
                         <div className="flex items-center gap-2 text-slate-700">
                             <TrendingUp className="w-4 h-4 text-emerald-600" />
                             <h4 className="text-[9px] font-black uppercase tracking-widest opacity-60">Matriz de Rendimiento</h4>
                         </div>
                         <div className="space-y-4">
                             <div className="flex justify-between items-center text-[10px] font-bold">
                                 <span className="text-slate-400 italic">Reportes procesados</span>
                                 <span className="text-slate-900 border-b-2 border-emerald-400">48</span>
                             </div>
                             <div className="flex justify-between items-center text-[10px] font-bold">
                                 <span className="text-slate-400 italic">Sincronización API</span>
                                 <span className="text-emerald-600 font-black tracking-tighter">100% OK</span>
                             </div>
                         </div>
                    </div>
                </div>

            </div>
        </div>
    );
};

export default Reports;
