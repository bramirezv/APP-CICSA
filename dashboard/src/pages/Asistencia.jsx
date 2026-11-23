import React, { useState, useEffect, useMemo } from 'react';
import { 
  Clock, Calendar, Download, RefreshCw, 
  LogIn, LogOut, Users, Activity,
  ChevronRight, Loader2, AlertCircle,
  CheckCircle2, Timer, ArrowUpRight
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { dataService } from '../services/api';

const Asistencia = () => {
  const [attendance, setAttendance] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split('T')[0]);
  const [exportStart, setExportStart] = useState(new Date().toISOString().split('T')[0]);
  const [exportEnd, setExportEnd] = useState(new Date().toISOString().split('T')[0]);
  const [exporting, setExporting] = useState(false);

  const fetchAttendance = async (silent = false) => {
    if (!silent) setLoading(true);
    else setRefreshing(true);
    try {
      const data = await dataService.getAttendance(selectedDate);
      setAttendance(data);
    } catch (err) {
      console.error('Error fetching attendance:', err);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    fetchAttendance();
  }, [selectedDate]);

  // Auto-refresh every 30s
  useEffect(() => {
    const interval = setInterval(() => fetchAttendance(true), 30000);
    return () => clearInterval(interval);
  }, [selectedDate]);

  // Group by user
  const summaries = useMemo(() => {
    const grouped = {};
    attendance.forEach(item => {
      const uid = item.userId;
      if (!grouped[uid]) {
        grouped[uid] = { userId: uid, nombre: item.nombre, ingresos: [], salidas: [] };
      }
      if (item.evento === 'INGRESO') grouped[uid].ingresos.push(item.hora);
      else grouped[uid].salidas.push(item.hora);
    });

    return Object.values(grouped).map(g => ({
      ...g,
      horaIngreso: g.ingresos.length > 0 ? g.ingresos.sort()[0] : null,
      horaSalida: g.salidas.length > 0 ? g.salidas.sort().reverse()[0] : null,
      totalEventos: g.ingresos.length + g.salidas.length
    })).sort((a, b) => (a.horaIngreso || '99:99').localeCompare(b.horaIngreso || '99:99'));
  }, [attendance]);

  const stats = useMemo(() => ({
    total: new Set(attendance.map(a => a.userId)).size,
    ingresos: attendance.filter(a => a.evento === 'INGRESO').length,
    salidas: attendance.filter(a => a.evento === 'SALIDA').length,
    enRuta: summaries.filter(s => s.horaIngreso && !s.horaSalida).length,
    rutaCerrada: summaries.filter(s => s.horaIngreso && s.horaSalida).length,
  }), [attendance, summaries]);

  const handleExportExcel = () => {
    setExporting(true);
    try {
      const url = dataService.exportHorariosRuta(exportStart, exportEnd);
      window.open(url, '_blank');
    } catch (err) {
      console.error('Export error:', err);
    } finally {
      setTimeout(() => setExporting(false), 2000);
    }
  };

  const formatDateLabel = (dateStr) => {
    const d = new Date(dateStr + 'T12:00:00');
    const days = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];
    const months = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'];
    return `${days[d.getDay()]}, ${d.getDate()} de ${months[d.getMonth()]} ${d.getFullYear()}`;
  };

  const getStatusBadge = (summary) => {
    if (summary.horaIngreso && summary.horaSalida) {
      return <span className="badge-std" style={{ background: '#fee2e2', color: '#dc2626', borderColor: '#fca5a5' }}>RUTA CERRADA</span>;
    }
    if (summary.horaIngreso) {
      return <span className="badge-std" style={{ background: '#dcfce7', color: '#15803d', borderColor: '#86efac' }}>EN RUTA</span>;
    }
    return <span className="badge-std" style={{ background: '#f1f5f9', color: '#94a3b8', borderColor: '#e2e8f0' }}>SIN ACTIVIDAD</span>;
  };

  return (
    <div className="space-y-6 pb-12">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b-2 border-slate-200 pb-5">
        <div>
          <h2 className="text-xl md:text-2xl font-bold text-slate-800 tracking-tight flex items-center gap-3">
            <Clock className="w-6 h-6 md:w-7 md:h-7 text-blue-800" />
            Control de Asistencia
          </h2>
          <p className="text-[10px] text-slate-500 font-bold uppercase tracking-widest mt-1">
            Horarios de Activación y Cierre de Ruta
          </p>
        </div>
        <div className="flex items-center gap-3">
          <input 
            type="date" 
            value={selectedDate} 
            onChange={e => setSelectedDate(e.target.value)}
            className="form-input h-10 text-[11px] font-bold w-36 md:w-44"
          />
          <button 
            onClick={() => fetchAttendance()} 
            className="btn-enterprise h-10 px-4 flex items-center gap-2 text-[10px]"
            disabled={refreshing}
          >
            <RefreshCw className={`w-4 h-4 ${refreshing ? 'animate-spin' : ''}`} />
            <span className="hidden sm:inline">ACTUALIZAR</span>
          </button>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3 md:gap-4">
        <StatCard icon={Users} label="PERSONAL" value={stats.total} color="#1e40af" bg="#dbeafe" />
        <StatCard icon={LogIn} label="INGRESOS" value={stats.ingresos} color="#15803d" bg="#dcfce7" />
        <StatCard icon={LogOut} label="SALIDAS" value={stats.salidas} color="#dc2626" bg="#fee2e2" />
        <StatCard icon={Activity} label="EN RUTA" value={stats.enRuta} color="#a16207" bg="#fef9c3" />
        <StatCard icon={CheckCircle2} label="CERRADAS" value={stats.rutaCerrada} color="#7c3aed" bg="#f3e8ff" />
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-4 gap-6">
        
        {/* Main Table */}
        <div className="xl:col-span-3 space-y-4">
          <div className="content-card">
            <h3 className="content-title text-[10px] md:text-xs tracking-widest uppercase flex items-center gap-2">
              <Timer className="w-4 h-4 text-blue-800" />
              Resumen del día — {formatDateLabel(selectedDate)}
            </h3>

            {loading ? (
              <div className="flex items-center justify-center py-20">
                <Loader2 className="w-8 h-8 animate-spin text-blue-800" />
              </div>
            ) : summaries.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-20 text-slate-400">
                <AlertCircle className="w-12 h-12 mb-3 opacity-40" />
                <p className="text-sm font-bold">No hay registros de asistencia</p>
                <p className="text-[10px] mt-1">No se encontraron eventos para esta fecha</p>
              </div>
            ) : (
              <div className="overflow-x-auto border border-slate-200 rounded">
                <table className="zebra-table min-w-[600px]">
                  <thead>
                    <tr>
                      <th className="py-3 px-4 text-[9px] font-black uppercase tracking-widest w-10">N°</th>
                      <th className="py-3 px-4 text-[9px] font-black uppercase tracking-widest text-left">SATÉLITE</th>
                      <th className="py-3 px-4 text-[9px] font-black uppercase tracking-widest text-center">
                        <span className="inline-flex items-center gap-1">
                          <LogIn className="w-3 h-3 text-emerald-600" />
                          ACTIVÓ RUTA
                        </span>
                      </th>
                      <th className="py-3 px-4 text-[9px] font-black uppercase tracking-widest text-center">
                        <span className="inline-flex items-center gap-1">
                          <LogOut className="w-3 h-3 text-red-600" />
                          CERRÓ RUTA
                        </span>
                      </th>
                      <th className="py-3 px-4 text-[9px] font-black uppercase tracking-widest text-center">EVENTOS</th>
                      <th className="py-3 px-4 text-[9px] font-black uppercase tracking-widest text-center">ESTADO</th>
                    </tr>
                  </thead>
                  <tbody>
                    {summaries.map((s, idx) => (
                      <motion.tr 
                        key={s.userId}
                        initial={{ opacity: 0, y: 5 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ delay: idx * 0.03 }}
                        className="hover:bg-blue-50/50"
                      >
                        <td className="py-3 px-4 text-center font-bold text-[11px] text-slate-400">{idx + 1}</td>
                        <td className="py-3 px-4">
                          <div className="flex items-center gap-3">
                            <div 
                              className="w-8 h-8 rounded-full flex items-center justify-center text-white font-black text-[10px] flex-shrink-0"
                              style={{ backgroundColor: '#1e40af' }}
                            >
                              {s.nombre.charAt(0).toUpperCase()}
                            </div>
                            <div>
                              <p className="font-bold text-[11px] text-slate-800 uppercase leading-none">{s.nombre}</p>
                              <p className="text-[9px] text-slate-400 mt-0.5">ID: {s.userId}</p>
                            </div>
                          </div>
                        </td>
                        <td className="py-3 px-4 text-center">
                          {s.horaIngreso ? (
                            <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded bg-emerald-50 border border-emerald-200">
                              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
                              <span className="font-black text-emerald-700 text-sm tracking-tight">{s.horaIngreso}</span>
                            </span>
                          ) : (
                            <span className="text-slate-300 text-xs font-bold">— — —</span>
                          )}
                        </td>
                        <td className="py-3 px-4 text-center">
                          {s.horaSalida ? (
                            <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded bg-red-50 border border-red-200">
                              <span className="w-1.5 h-1.5 rounded-full bg-red-500"></span>
                              <span className="font-black text-red-700 text-sm tracking-tight">{s.horaSalida}</span>
                            </span>
                          ) : (
                            <span className="text-slate-300 text-xs font-bold">— — —</span>
                          )}
                        </td>
                        <td className="py-3 px-4 text-center">
                          <span className="font-black text-xs text-slate-600">{s.totalEventos}</span>
                        </td>
                        <td className="py-3 px-4 text-center">
                          {getStatusBadge(s)}
                        </td>
                      </motion.tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* Event log */}
          {attendance.length > 0 && (
            <div className="content-card">
              <h3 className="content-title text-[10px] md:text-xs tracking-widest uppercase flex items-center gap-2">
                <Activity className="w-4 h-4 text-blue-800" />
                Historial de Eventos Detallado
              </h3>
              <div className="overflow-x-auto border border-slate-200 rounded">
                <table className="zebra-table min-w-[500px]">
                  <thead>
                    <tr>
                      <th className="py-2.5 px-4 text-[9px] font-black uppercase tracking-widest text-left">SATÉLITE</th>
                      <th className="py-2.5 px-4 text-[9px] font-black uppercase tracking-widest text-center">EVENTO</th>
                      <th className="py-2.5 px-4 text-[9px] font-black uppercase tracking-widest text-center">HORA</th>
                      <th className="py-2.5 px-4 text-[9px] font-black uppercase tracking-widest text-center">FECHA</th>
                    </tr>
                  </thead>
                  <tbody>
                    {[...attendance].sort((a, b) => b.hora.localeCompare(a.hora)).map((item, idx) => (
                      <tr key={item.id || idx}>
                        <td className="py-2.5 px-4 font-bold text-[11px] text-slate-800 uppercase">{item.nombre}</td>
                        <td className="py-2.5 px-4 text-center">
                          {item.evento === 'INGRESO' ? (
                            <span className="inline-flex items-center gap-1 text-[10px] font-bold text-emerald-700">
                              <LogIn className="w-3 h-3" /> Inició Ruta
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 text-[10px] font-bold text-red-600">
                              <LogOut className="w-3 h-3" /> Finalizó Ruta
                            </span>
                          )}
                        </td>
                        <td className="py-2.5 px-4 text-center font-black text-sm text-slate-800">{item.hora}</td>
                        <td className="py-2.5 px-4 text-center text-[10px] text-slate-400 font-bold">{item.fecha}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>

        {/* Sidebar: Export Panel */}
        <div className="space-y-4">
          <div className="content-card">
            <h3 className="content-title text-[10px] md:text-xs tracking-widest uppercase flex items-center gap-2">
              <Download className="w-4 h-4 text-blue-800" />
              Exportar Horarios
            </h3>
            <div className="p-4 space-y-5">
              <p className="text-[10px] text-slate-500 leading-relaxed">
                Genera un archivo Excel con los horarios de activación y cierre de ruta de todos los satélites para el rango de fechas seleccionado.
              </p>

              <div className="space-y-3">
                <div className="space-y-1.5">
                  <label className="text-[9px] font-black text-slate-400 uppercase tracking-widest">Fecha Inicio</label>
                  <input 
                    type="date" 
                    value={exportStart} 
                    onChange={e => setExportStart(e.target.value)}
                    className="form-input h-10 text-[11px] font-bold"
                  />
                </div>
                <div className="space-y-1.5">
                  <label className="text-[9px] font-black text-slate-400 uppercase tracking-widest">Fecha Fin</label>
                  <input 
                    type="date" 
                    value={exportEnd} 
                    onChange={e => setExportEnd(e.target.value)}
                    className="form-input h-10 text-[11px] font-bold"
                  />
                </div>
              </div>

              <button 
                onClick={handleExportExcel}
                disabled={exporting}
                className="btn-enterprise w-full h-12 flex items-center justify-center gap-2 text-[10px] font-black uppercase tracking-widest shadow-lg shadow-blue-800/10"
              >
                {exporting ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    GENERANDO...
                  </>
                ) : (
                  <>
                    <Download className="w-4 h-4" />
                    DESCARGAR EXCEL
                  </>
                )}
              </button>
            </div>
          </div>

          {/* Quick Summary Card */}
          <div className="content-card bg-blue-800 text-white border-none py-6 px-5">
            <div className="text-center space-y-3">
              <Clock className="w-8 h-8 mx-auto opacity-40" />
              <div>
                <h4 className="font-black text-sm tracking-tight uppercase">Control en Tiempo Real</h4>
                <p className="text-[10px] opacity-60 mt-2 leading-relaxed font-bold">
                  Los datos se actualizan automáticamente cada 30 segundos. Las horas registradas corresponden al momento exacto de activación y cierre de ruta.
                </p>
              </div>
              <div className="h-px bg-white/10 mx-6" />
              <p className="text-[8px] font-black tracking-[0.3em] opacity-40 uppercase">LIVE MONITORING v1.0</p>
            </div>
          </div>

          {/* Quick Export Today */}
          <div className="content-card p-4 bg-slate-50 border-slate-200">
            <button 
              onClick={() => {
                const today = new Date().toISOString().split('T')[0];
                const url = dataService.exportHorariosRuta(today, today);
                window.open(url, '_blank');
              }}
              className="w-full flex items-center justify-between gap-2 text-[10px] font-bold text-blue-800 hover:text-blue-600 transition-colors py-1"
            >
              <span className="flex items-center gap-2">
                <ArrowUpRight className="w-4 h-4" />
                EXPORTAR SOLO HOY
              </span>
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

const StatCard = ({ icon: Icon, label, value, color, bg }) => (
  <motion.div 
    initial={{ opacity: 0, y: 8 }}
    animate={{ opacity: 1, y: 0 }}
    className="content-card py-4 px-4 flex items-center gap-3"
  >
    <div 
      className="w-10 h-10 rounded flex items-center justify-center flex-shrink-0"
      style={{ backgroundColor: bg }}
    >
      <Icon className="w-5 h-5" style={{ color }} />
    </div>
    <div>
      <p className="font-black text-xl leading-none" style={{ color }}>{value}</p>
      <p className="text-[8px] font-black text-slate-400 uppercase tracking-widest mt-1">{label}</p>
    </div>
  </motion.div>
);

export default Asistencia;
