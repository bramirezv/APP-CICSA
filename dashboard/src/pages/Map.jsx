import React, { useState, useEffect } from 'react';
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import { 
  Globe, Search, Users, RefreshCw, ChevronRight, X, Phone, Info, CheckCircle2, AlertTriangle, Coffee, Play
} from 'lucide-react';
import { useData } from '../services/DataContext';

const createEnterpriseIcon = (color, name = "") => {
    const initial = name ? name.trim().charAt(0).toUpperCase() : '?';
    // Get first word of the name for a shorter and cleaner tooltip above the marker
    const firstName = name ? name.split(' ')[0].toUpperCase() : '';
    
    return L.divIcon({
        className: 'enterprise-marker',
        html: `
            <div style="position: relative; display: flex; flex-direction: column; align-items: center; justify-content: center; width: 44px; height: 56px;">
                <!-- Tooltip con el nombre -->
                ${firstName ? `
                <div style="
                    position: absolute;
                    top: -6px;
                    background: rgba(15, 23, 42, 0.95);
                    backdrop-filter: blur(4px);
                    color: #ffffff;
                    padding: 2px 8px;
                    border-radius: 9999px;
                    font-size: 9px;
                    font-weight: 950;
                    letter-spacing: 0.05em;
                    box-shadow: 0 4px 10px rgba(0, 0, 0, 0.15);
                    text-transform: uppercase;
                    white-space: nowrap;
                    border: 1px solid rgba(255, 255, 255, 0.1);
                    pointer-events: none;
                    z-index: 10;
                ">
                    ${firstName}
                </div>
                ` : ''}
                <!-- Marcador Circular del Técnico -->
                <div style="
                    width: 36px;
                    height: 36px;
                    border-radius: 50%;
                    background: #ffffff;
                    border: 3.5px solid ${color};
                    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.25);
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-size: 14px;
                    font-weight: 900;
                    color: ${color};
                    font-family: 'Outfit', 'Inter', system-ui, -apple-system, sans-serif;
                    transition: all 0.2s ease-in-out;
                ">
                    ${initial}
                </div>
                <!-- Pequeño triángulo o puntero sutil debajo -->
                <div style="
                    width: 0;
                    height: 0;
                    border-left: 6px solid transparent;
                    border-right: 6px solid transparent;
                    border-top: 6px solid ${color};
                    margin-top: -1px;
                    filter: drop-shadow(0 2px 2px rgba(0,0,0,0.15));
                "></div>
            </div>
        `,
        iconSize: [44, 56],
        iconAnchor: [22, 46]
    });
};

const MapView = () => {
    const { users, loading, refreshData, isRefreshing, lastUpdated } = useData();
    const [searchTerm, setSearchTerm] = useState('');
    const [mapInstance, setMapInstance] = useState(null);
    const [isPanelOpen, setPanelOpen] = useState(false);
    
    const [selectedRole, setSelectedRole] = useState('Todos');
    const [selectedStatus, setSelectedStatus] = useState('Todos');
    const [selectedZone, setSelectedZone] = useState('Todos');
    const [selectedUser, setSelectedUser] = useState(null);
    
    // Progreso del temporizador de actualización visual (15 segundos)
    const [progress, setProgress] = useState(100);

    // Satélites y usuarios con coordenadas válidas de los datos globales
    const satellites = users.filter(s => s.lat && s.lng);

    // Mantener la información del usuario seleccionado actualizada en tiempo real con el polling
    const activeSelectedUser = selectedUser ? satellites.find(u => u.id === selectedUser.id) : null;

    // Efecto para animar el countdown visual de 15 segundos sincronizado con lastUpdated
    useEffect(() => {
        if (!lastUpdated) return;
        
        const interval = setInterval(() => {
            const elapsed = Date.now() - new Date(lastUpdated).getTime();
            const percent = Math.max(0, 100 - (elapsed / 15000) * 100);
            setProgress(percent);
        }, 100);

        return () => clearInterval(interval);
    }, [lastUpdated]);

    const getPhotoUrl = (path) => {
        if (!path || path === 'null') return null;
        const productionDomain = "http://www.cicsamovil.com/";
        
        if (path.includes("tudominio.com")) {
            return path.replace("https://tudominio.com/", productionDomain)
                       .replace("http://tudominio.com/", productionDomain)
                       .replace("http://ww38.tudominio.com/", productionDomain);
        }
        if (path.startsWith('http')) return path;
        
        const cleanPath = path.startsWith('/') ? path.substring(1) : path;
        return `${productionDomain}${cleanPath}`;
    };

    const center = [-12.0464, -77.0428]; // Lima por defecto

    const flyToUser = (lat, lng) => {
        if (mapInstance) {
            mapInstance.flyTo([lat, lng], 17, { duration: 1.5 });
            if (window.innerWidth < 1024) setPanelOpen(false);
        }
    };

    // Obtener lista única de zonas detectadas dinámicamente
    const zones = [...new Set(satellites.map(s => s.zona).filter(Boolean))].sort();

    // Estadísticas rápidas generales (según la lista total de usuarios con ubicación)
    const stats = {
        total: satellites.length,
        disponible: satellites.filter(s => s.estado === 'Disponible').length,
        ocupado: satellites.filter(s => s.estado === 'Ocupado' || s.estado === 'En Servicio').length,
        almuerzo: satellites.filter(s => s.estado === 'Almorzando').length,
        offline: satellites.filter(s => s.estado === 'Fuera de línea' || !s.estado || s.estado === 'No Disponible').length
    };

    // Aplicar filtros en tiempo real
    const filteredSatellites = satellites.filter(s => {
        const matchesSearch = (s.nombre || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
                              (s.zona || '').toLowerCase().includes(searchTerm.toLowerCase());
        
        const matchesRole = selectedRole === 'Todos' || s.rol === selectedRole;
        
        const matchesStatus = selectedStatus === 'Todos' || (
            selectedStatus === 'Disponible' ? s.estado === 'Disponible' :
            selectedStatus === 'Ocupado' ? (s.estado === 'Ocupado' || s.estado === 'En Servicio') :
            selectedStatus === 'Almorzando' ? s.estado === 'Almorzando' :
            selectedStatus === 'Offline' ? (s.estado === 'Fuera de línea' || !s.estado || s.estado === 'No Disponible') : true
        );
        
        const matchesZone = selectedZone === 'Todos' || s.zona === selectedZone;
        
        return matchesSearch && matchesRole && matchesStatus && matchesZone;
    });

    return (
        <div className="space-y-4 md:space-y-6 flex flex-col h-auto lg:h-[580px] min-h-[480px]">
            {/* Header del Dashboard */}
            <div className="flex items-end justify-between border-b-2 border-slate-200 pb-4">
                <div>
                    <h2 className="text-xl md:text-2xl font-bold text-slate-800 tracking-tight flex items-center gap-2">
                        <Globe className="w-5 h-5 md:w-6 md:h-6 text-blue-800" />
                        Monitoreo Satelital
                    </h2>
                    <p className="text-[9px] md:text-[10px] text-slate-400 font-black uppercase tracking-widest mt-1">Geolocalización en Tiempo Real</p>
                </div>
                <div className="flex items-center gap-2">
                    <button 
                        onClick={() => setPanelOpen(!isPanelOpen)}
                        className="lg:hidden p-2 bg-blue-800 text-white rounded shadow-sm hover:bg-blue-700 transition"
                    >
                        {isPanelOpen ? <X className="w-4 h-4" /> : <Users className="w-4 h-4" />}
                    </button>
                    <button 
                        onClick={refreshData} disabled={loading || isRefreshing}
                        className="p-2 bg-slate-100 text-blue-800 border border-slate-300 rounded shadow-sm hover:bg-slate-200 transition"
                    >
                        <RefreshCw className={`w-4 h-4 ${loading || isRefreshing ? 'animate-spin' : ''}`} />
                    </button>
                </div>
            </div>

            {/* Panel de Filtros Superiores */}
            <div className="bg-white/90 backdrop-blur-md border border-slate-100 rounded-2xl p-3 flex flex-wrap gap-4 items-center shadow-md">
                <div className="flex items-center gap-1.5 text-slate-500 font-black text-[10px] tracking-wider uppercase">
                    <Info className="w-4 h-4 text-blue-800" />
                    <span>Filtros de Búsqueda:</span>
                </div>
                
                <div className="flex-1 flex flex-wrap gap-3">
                    <select
                        value={selectedRole}
                        onChange={(e) => setSelectedRole(e.target.value)}
                        className="form-select bg-slate-50 hover:bg-slate-100 border border-slate-200 text-xs rounded-xl h-9 px-3 py-0 text-slate-700 font-extrabold transition cursor-pointer focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    >
                        <option value="Todos">Rol: Todos</option>
                        <option value="satelite">Satélites</option>
                        <option value="supervisor">Supervisores</option>
                        <option value="admin">Administradores</option>
                    </select>

                    <select
                        value={selectedStatus}
                        onChange={(e) => setSelectedStatus(e.target.value)}
                        className="form-select bg-slate-50 hover:bg-slate-100 border border-slate-200 text-xs rounded-xl h-9 px-3 py-0 text-slate-700 font-extrabold transition cursor-pointer focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    >
                        <option value="Todos">Estado: Todos</option>
                        <option value="Disponible">Disponibles</option>
                        <option value="Ocupado">En Servicio (Ocupado)</option>
                        <option value="Almorzando">En Almuerzo</option>
                        <option value="Offline">Fuera de Línea</option>
                    </select>

                    <select
                        value={selectedZone}
                        onChange={(e) => setSelectedZone(e.target.value)}
                        className="form-select bg-slate-50 hover:bg-slate-100 border border-slate-200 text-xs rounded-xl h-9 px-3 py-0 text-slate-700 font-extrabold transition cursor-pointer focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    >
                        <option value="Todos">Zona: Todas</option>
                        {zones.map(z => (
                            <option key={z} value={z}>{z}</option>
                        ))}
                    </select>
                </div>
                
                {(selectedRole !== 'Todos' || selectedStatus !== 'Todos' || selectedZone !== 'Todos' || searchTerm) && (
                    <button
                        onClick={() => {
                            setSelectedRole('Todos');
                            setSelectedStatus('Todos');
                            setSelectedZone('Todos');
                            setSearchTerm('');
                            setSelectedUser(null);
                        }}
                        className="text-[10px] font-black uppercase text-blue-800 hover:underline"
                    >
                        Limpiar filtros
                    </button>
                )}
            </div>

            {/* Contenedor del Mapa e Indicadores */}
            <div className="flex-1 flex flex-col lg:flex-row gap-6 h-full w-full items-stretch relative">
                
                {/* Barra Lateral de Telemetría */}
                <div className={`
                    w-full lg:w-80 flex flex-col border border-slate-200/80 rounded-2xl shadow-lg overflow-hidden h-full z-[1001] lg:z-10 bg-white
                    fixed lg:relative inset-x-0 bottom-0 lg:inset-auto transition-transform duration-300 ease-in-out
                    ${isPanelOpen ? 'translate-y-0' : 'translate-y-full lg:translate-y-0'}
                    max-h-[70vh] lg:max-h-full
                `}>
                    {/* Resumen de Estados (Contadores rápidos) */}
                    <div className="p-2.5 bg-slate-50 border-b border-slate-200 grid grid-cols-4 gap-1.5 text-center">
                        <div className="bg-white rounded border border-slate-200 p-1">
                            <p className="text-[12px] font-black text-slate-800 leading-none">{stats.total}</p>
                            <p className="text-[7px] font-extrabold text-slate-400 uppercase tracking-wider mt-1.5">Total</p>
                        </div>
                        <div className="bg-emerald-50 rounded border border-emerald-100 p-1">
                            <p className="text-[12px] font-black text-emerald-700 leading-none">{stats.disponible}</p>
                            <p className="text-[7px] font-extrabold text-emerald-500 uppercase tracking-wider mt-1.5">Disp</p>
                        </div>
                        <div className="bg-blue-50 rounded border border-blue-100 p-1">
                            <p className="text-[12px] font-black text-blue-700 leading-none">{stats.ocupado}</p>
                            <p className="text-[7px] font-extrabold text-blue-500 uppercase tracking-wider mt-1.5">Serv</p>
                        </div>
                        <div className="bg-amber-50 rounded border border-amber-100 p-1">
                            <p className="text-[12px] font-black text-amber-700 leading-none">{stats.almuerzo}</p>
                            <p className="text-[7px] font-extrabold text-amber-500 uppercase tracking-wider mt-1.5">Alm</p>
                        </div>
                    </div>

                    <div className="p-3 bg-white border-b border-slate-100">
                        <div className="relative">
                            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-300" />
                            <input 
                                type="text" placeholder="Buscar personal..." value={searchTerm}
                                onChange={(e) => setSearchTerm(e.target.value)}
                                className="w-full form-input pl-9 h-9 text-xs font-bold text-slate-700"
                            />
                        </div>
                    </div>

                    {/* Lista de Unidades */}
                    <div className="flex-1 overflow-y-auto divide-y divide-slate-50">
                        {filteredSatellites.length > 0 ? filteredSatellites.map(sat => {
                            const isAvailable = sat.estado === 'Disponible';
                            const isLunch = sat.estado === 'Almorzando';
                            const isOffline = sat.estado === 'Fuera de línea' || sat.estado === 'No Disponible' || !sat.estado;
                            
                            const badgeBorder = isAvailable ? 'bg-emerald-500' : isLunch ? 'bg-amber-500' : isOffline ? 'bg-slate-400' : 'bg-blue-600';
                            
                            return (
                                <button 
                                    key={sat.id} onClick={() => {
                                        setSelectedUser(sat);
                                        flyToUser(sat.lat, sat.lng);
                                    }}
                                    className={`w-full p-3.5 flex items-center justify-between transition-all text-left border-l-2 hover:bg-slate-50 ${
                                        selectedUser?.id === sat.id ? 'bg-blue-50/50 border-blue-600' : 'border-transparent'
                                    }`}
                                >
                                    <div className="flex items-center gap-3">
                                        <div className={`w-1.5 h-8 rounded-sm ${badgeBorder}`} />
                                        <div>
                                            <p className="text-[11px] font-bold text-slate-900 leading-none">{sat.nombre.toUpperCase()}</p>
                                            <div className="flex items-center gap-2 mt-1.5">
                                                <span className="text-[8px] font-black text-slate-400 uppercase tracking-widest">
                                                    {sat.rol.toUpperCase()}
                                                </span>
                                                <span className="text-[8px] font-bold text-blue-800 bg-blue-50 px-1.5 py-0.5 rounded text-center">
                                                    {sat.zona}
                                                </span>
                                            </div>
                                        </div>
                                    </div>
                                    <ChevronRight className="w-3.5 h-3.5 text-slate-200" />
                                </button>
                            );
                        }) : <p className="p-10 text-center text-[10px] font-black text-slate-300 uppercase italic">Sin unidades activas</p>}
                    </div>
                    
                    <div className="p-2.5 bg-slate-50 border-t border-slate-100 text-center font-black text-[9px] text-slate-400 uppercase tracking-wider">
                        {filteredSatellites.length} UNIDADES EN PANTALLA
                    </div>
                </div>

                {/* Área de Visualización del Mapa */}
                <div className="flex-1 h-[350px] lg:h-full rounded-2xl border border-slate-200/80 shadow-lg overflow-hidden relative z-10 w-full flex flex-col bg-slate-50">
                    
                    {/* Leyenda Absoluta sobre el mapa */}
                    <div className="absolute top-4 right-4 z-[1000] bg-white/95 backdrop-blur-sm border border-slate-200 rounded-xl p-3 shadow-lg space-y-2 hidden sm:block">
                         <p className="text-[8px] font-black text-slate-400 uppercase tracking-widest border-b pb-1 mb-1.5">Leyenda de Estados</p>
                         <div className="flex items-center gap-2">
                            <div className="w-2.5 h-2.5 rounded-full bg-emerald-500 border border-white shadow-sm" />
                            <span className="text-[9px] font-extrabold text-slate-600 uppercase tracking-wider">DISPONIBLE</span>
                         </div>
                         <div className="flex items-center gap-2">
                            <div className="w-2.5 h-2.5 rounded-full bg-blue-700 border border-white shadow-sm" />
                            <span className="text-[9px] font-extrabold text-slate-600 uppercase tracking-wider">EN SERVICIO</span>
                         </div>
                         <div className="flex items-center gap-2">
                            <div className="w-2.5 h-2.5 rounded-full bg-amber-500 border border-white shadow-sm" />
                            <span className="text-[9px] font-extrabold text-slate-600 uppercase tracking-wider">ALMUERZO</span>
                         </div>
                         <div className="flex items-center gap-2">
                            <div className="w-2.5 h-2.5 rounded-full bg-slate-500 border border-white shadow-sm" />
                            <span className="text-[9px] font-extrabold text-slate-600 uppercase tracking-wider">FUERA DE LÍNEA</span>
                         </div>
                    </div>
                    
                    {/* Barra de progreso de auto-actualización de 15 segundos */}
                    <div className="w-full bg-slate-100 h-1 overflow-hidden relative z-20">
                        <div 
                            className="bg-blue-600 h-full transition-all duration-100 ease-linear shadow"
                            style={{ width: `${progress}%` }}
                        />
                    </div>

                    <div className="flex-1 w-full h-full">
                        <MapContainer 
                            center={center} zoom={13} zoomControl={false} ref={setMapInstance}
                            style={{ height: '100%', width: '100%' }}
                        >
                            <TileLayer
                                url="https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png"
                                attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                            />
                            {filteredSatellites.map(sat => {
                                // Color del marcador según estado y rol
                                let markerColor = '#3b82f6'; // Azul Ocupado
                                if (sat.estado === 'Disponible') {
                                    markerColor = '#10b981'; // Verde Esmeralda
                                } else if (sat.estado === 'Almorzando') {
                                    markerColor = '#f59e0b'; // Ámbar
                                } else if (sat.estado === 'Fuera de línea' || !sat.estado || sat.estado === 'No Disponible') {
                                    markerColor = '#64748b'; // Gris
                                }

                                return (
                                    <Marker 
                                        key={sat.id} position={[sat.lat, sat.lng]} 
                                        icon={createEnterpriseIcon(markerColor, sat.nombre)}
                                        eventHandlers={{
                                            click: () => {
                                                setSelectedUser(sat);
                                                flyToUser(sat.lat, sat.lng);
                                            }
                                        }}
                                    >
                                        <Popup closeButton={false} className="enterprise-popup">
                                            <div className="p-4 min-w-[210px]">
                                                {/* Header del Popup */}
                                                <div className="flex items-center gap-3 mb-3 pb-3 border-b border-slate-100">
                                                    <div className="w-8 h-8 rounded flex items-center justify-center font-black text-xs border bg-slate-50 text-blue-900 border-slate-200">
                                                        {sat.nombre.charAt(0).toUpperCase()}
                                                    </div>
                                                    <div>
                                                        <h4 className="font-bold text-[12px] text-slate-900 leading-none">{sat.nombre.toUpperCase()}</h4>
                                                        <div className="flex items-center gap-2 mt-1.5">
                                                            <span className="text-[8px] font-black text-slate-400 uppercase tracking-wider">{sat.rol}</span>
                                                            <span className="text-[8px] font-extrabold text-blue-800 bg-blue-50 px-1 rounded">{sat.zona}</span>
                                                        </div>
                                                    </div>
                                                </div>

                                                {/* Telemetría del Popup */}
                                                <div className="bg-slate-50 p-2.5 rounded text-[10px] space-y-2">
                                                    <div className="flex justify-between font-bold">
                                                        <span className="text-slate-400 uppercase">Estado</span>
                                                        <span style={{ color: markerColor }} className="font-extrabold">
                                                            {(sat.estado || 'Fuera de línea').toUpperCase()}
                                                        </span>
                                                    </div>
                                                    {sat.telefono && (
                                                        <div className="flex justify-between font-bold items-center">
                                                            <span className="text-slate-400 uppercase">Teléfono</span>
                                                            <a href={`tel:${sat.telefono}`} className="text-blue-700 hover:underline font-extrabold flex items-center gap-1">
                                                                <Phone className="w-3 h-3 text-blue-600" />
                                                                {sat.telefono}
                                                            </a>
                                                        </div>
                                                    )}
                                                    <div className="flex justify-between font-bold">
                                                        <span className="text-slate-400 uppercase">Último Reporte</span>
                                                        <span className="text-slate-600 font-extrabold">
                                                            {sat.ultima_conexion ? sat.ultima_conexion.split(' ')[1] || sat.ultima_conexion : '---'}
                                                        </span>
                                                    </div>
                                                </div>

                                                {/* Evidencia fotográfica si estuviera presente */}
                                                {sat.foto_evidencia && (
                                                    <div className="mt-3 overflow-hidden rounded border border-slate-200 shadow-sm">
                                                        <img 
                                                            src={getPhotoUrl(sat.foto_evidencia)} 
                                                            alt="Evidencia" 
                                                            className="w-full h-24 object-cover hover:scale-105 transition-transform duration-200"
                                                        />
                                                    </div>
                                                )}
                                            </div>
                                        </Popup>
                                    </Marker>
                                );
                            })}
                        </MapContainer>

                        {/* Tarjeta Flotante de Telemetría (Estilo Móvil) */}
                        {activeSelectedUser && (
                            <div className="absolute bottom-4 right-4 left-4 md:left-auto z-[1002] md:w-[360px] bg-white/95 backdrop-blur-md rounded-2xl shadow-2xl border border-slate-100 p-5 transition-all duration-300 transform translate-y-0 flex flex-col">
                                {/* Botón de cerrar */}
                                <button 
                                    onClick={() => setSelectedUser(null)}
                                    className="absolute top-3 right-3 p-1.5 rounded-full hover:bg-slate-105 text-slate-400 hover:text-slate-600 transition"
                                >
                                    <X className="w-4 h-4" />
                                </button>

                                {/* Encabezado: Avatar e Info */}
                                <div className="flex items-center gap-4">
                                    {/* Avatar circular con anillo de estado */}
                                    <div 
                                        className="w-12 h-12 rounded-full bg-white flex items-center justify-center font-black text-base shadow-sm border-4 transition-all duration-300"
                                        style={{ borderColor: 
                                            activeSelectedUser.estado === 'Disponible' ? '#10b981' :
                                            activeSelectedUser.estado === 'Almorzando' ? '#f59e0b' :
                                            (activeSelectedUser.estado === 'Fuera de línea' || !activeSelectedUser.estado || activeSelectedUser.estado === 'No Disponible') ? '#64748b' : '#3b82f6'
                                        }}
                                    >
                                        <span style={{ color: 
                                            activeSelectedUser.estado === 'Disponible' ? '#10b981' :
                                            activeSelectedUser.estado === 'Almorzando' ? '#f59e0b' :
                                            (activeSelectedUser.estado === 'Fuera de línea' || !activeSelectedUser.estado || activeSelectedUser.estado === 'No Disponible') ? '#64748b' : '#3b82f6'
                                        }}>
                                            {activeSelectedUser.nombre.charAt(0).toUpperCase()}
                                        </span>
                                    </div>

                                    <div className="flex-1 min-w-0">
                                        <h4 className="font-black text-[13px] text-slate-800 uppercase tracking-tight truncate leading-none">
                                            {activeSelectedUser.nombre}
                                        </h4>
                                        <div className="flex items-center gap-2 mt-1.5">
                                            <span className="text-[8px] font-black bg-blue-50 text-blue-700 px-1.5 py-0.5 rounded uppercase tracking-wider">
                                                {activeSelectedUser.rol}
                                            </span>
                                            <span className="text-[8px] font-black text-slate-400 uppercase tracking-wider">
                                                Zona: <span className="text-slate-600 font-extrabold">{activeSelectedUser.zona || '---'}</span>
                                            </span>
                                        </div>
                                    </div>
                                </div>

                                {/* Separador */}
                                <div className="border-b border-slate-100 my-3" />

                                {/* Rejilla de Telemetría */}
                                <div className="grid grid-cols-3 gap-2 text-center">
                                    {/* Estado */}
                                    <div className="flex flex-col items-center">
                                        <span className="text-[7px] font-black text-slate-400 uppercase tracking-wider">Estado Actual</span>
                                        <div className="flex items-center gap-1 mt-1.5">
                                            <span className="relative flex h-2 w-2">
                                                <span className={`animate-ping absolute inline-flex h-full w-full rounded-full opacity-75 ${
                                                    activeSelectedUser.estado === 'Disponible' ? 'bg-emerald-450 bg-emerald-400' :
                                                    activeSelectedUser.estado === 'Almorzando' ? 'bg-amber-450 bg-amber-400' :
                                                    (activeSelectedUser.estado === 'Fuera de línea' || !activeSelectedUser.estado || activeSelectedUser.estado === 'No Disponible') ? 'bg-slate-400' : 'bg-blue-400'
                                                }`}></span>
                                                <span className={`relative inline-flex rounded-full h-2 w-2 ${
                                                    activeSelectedUser.estado === 'Disponible' ? 'bg-emerald-500' :
                                                    activeSelectedUser.estado === 'Almorzando' ? 'bg-amber-500' :
                                                    (activeSelectedUser.estado === 'Fuera de línea' || !activeSelectedUser.estado || activeSelectedUser.estado === 'No Disponible') ? 'bg-slate-500' : 'bg-blue-500'
                                                }`}></span>
                                            </span>
                                            <span className="text-[9px] font-black text-slate-700 uppercase leading-none">
                                                {activeSelectedUser.estado === 'Disponible' ? 'Disponible' :
                                                 activeSelectedUser.estado === 'Almorzando' ? 'Almuerzo' :
                                                 (activeSelectedUser.estado === 'Fuera de línea' || !activeSelectedUser.estado || activeSelectedUser.estado === 'No Disponible') ? 'Offline' : 'En Servicio'}
                                            </span>
                                        </div>
                                    </div>

                                    {/* Ruta / Plan */}
                                    <div className="flex flex-col items-center border-x border-slate-100 px-1">
                                        <span className="text-[7px] font-black text-slate-400 uppercase tracking-wider">Ruta / Plan</span>
                                        <span className={`text-[9px] font-black mt-1.5 uppercase leading-none ${activeSelectedUser.ruta_activa === 1 ? 'text-emerald-600' : 'text-slate-400'}`}>
                                            {activeSelectedUser.ruta_activa === 1 ? 'Activa' : 'Inactiva / Fin'}
                                        </span>
                                    </div>

                                    {/* Último Reporte */}
                                    <div className="flex flex-col items-center">
                                        <span className="text-[7px] font-black text-slate-400 uppercase tracking-wider">Último Reporte</span>
                                        <span className="text-[9px] font-bold text-slate-600 mt-1.5 leading-none">
                                            {activeSelectedUser.ultima_conexion ? activeSelectedUser.ultima_conexion.split(' ')[1] || activeSelectedUser.ultima_conexion : '---'}
                                        </span>
                                    </div>
                                </div>

                                {/* Separador */}
                                <div className="border-b border-slate-100 my-3" />

                                {/* Botones de Acción */}
                                <div className="grid grid-cols-2 gap-2">
                                    {/* Llamar */}
                                    {activeSelectedUser.telefono ? (
                                        <a 
                                            href={`tel:${activeSelectedUser.telefono}`}
                                            className="flex items-center justify-center gap-1.5 bg-blue-600 hover:bg-blue-700 text-white text-[10px] font-black uppercase tracking-wider py-2.5 px-3 rounded-xl shadow-md transition-all active:scale-[0.98] cursor-pointer text-center"
                                        >
                                            <Phone className="w-3 h-3" />
                                            Llamar
                                        </a>
                                    ) : (
                                        <button 
                                            disabled
                                            className="flex items-center justify-center gap-1.5 bg-slate-100 text-slate-400 text-[10px] font-black uppercase tracking-wider py-2.5 px-3 rounded-xl cursor-not-allowed text-center"
                                        >
                                            <Phone className="w-3 h-3" />
                                            Sin Teléfono
                                        </button>
                                    )}

                                    {/* Cómo Llegar */}
                                    <a 
                                        href={`https://www.google.com/maps/dir/?api=1&destination=${activeSelectedUser.lat},${activeSelectedUser.lng}`}
                                        target="_blank"
                                        rel="noopener noreferrer"
                                        className="flex items-center justify-center gap-1.5 bg-[#0f172a] hover:bg-[#1e293b] text-white text-[10px] font-black uppercase tracking-wider py-2.5 px-3 rounded-xl shadow-md transition-all active:scale-[0.98] text-center"
                                    >
                                        <Play className="w-3 h-3 rotate-[-90deg] fill-white" />
                                        Cómo Llegar
                                    </a>
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            </div>
            
            <style>{`
                .enterprise-popup .leaflet-popup-content-wrapper { border-radius: 8px; padding: 0; box-shadow: 0 10px 25px -5px rgba(0,0,0,0.1), 0 8px 10px -6px rgba(0,0,0,0.1); border: 1px solid #e2e8f0; }
                .enterprise-popup .leaflet-popup-content { margin: 0; }
                .enterprise-popup .leaflet-popup-tip { background: white; border: 1px solid #cbd5e1; }
            `}</style>
        </div>
    );
};

export default MapView;
