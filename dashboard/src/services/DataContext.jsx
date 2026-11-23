import React, { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';
import { dataService } from './api';

const DataContext = createContext();

// Simple deep comparison for arrays of objects
const areExacltyEqual = (arr1, arr2) => {
    if (arr1.length !== arr2.length) return false;
    return JSON.stringify(arr1) === JSON.stringify(arr2);
};

export const useData = () => {
    const context = useContext(DataContext);
    if (!context) {
        throw new Error('useData must be used within a DataProvider');
    }
    return context;
};

export const DataProvider = ({ children }) => {
    // Initial state from localStorage to ensure instant UI
    const [solicitudes, setSolicitudes] = useState(() => {
        const cached = localStorage.getItem('cache_solicitudes');
        return cached ? JSON.parse(cached) : [];
    });
    const [users, setUsers] = useState(() => {
        const cached = localStorage.getItem('cache_users');
        return cached ? JSON.parse(cached) : [];
    });
    
    const [loading, setLoading] = useState(solicitudes.length === 0);
    const [lastUpdated, setLastUpdated] = useState(null);
    const [isRefreshing, setIsRefreshing] = useState(false);
    
    const isFirstLoad = useRef(true);

    const fetchData = useCallback(async (silent = false) => {
        if (!silent && solicitudes.length === 0) setLoading(true);
        setIsRefreshing(true);
        
        const fetchSolicitudes = async () => {
            try {
                const solData = await dataService.getSolicitudes();
                if (Array.isArray(solData)) {
                    setSolicitudes(prev => {
                        if (areExacltyEqual(prev, solData)) return prev;
                        localStorage.setItem('cache_solicitudes', JSON.stringify(solData));
                        return solData;
                    });
                }
            } catch (e) { console.error('Error fetching solicitudes:', e); }
        };

        const fetchUsers = async () => {
            try {
                const usersData = await dataService.getUsersStatus();
                if (Array.isArray(usersData)) {
                    setUsers(prev => {
                        if (areExacltyEqual(prev, usersData)) return prev;
                        localStorage.setItem('cache_users', JSON.stringify(usersData));
                        return usersData;
                    });
                }
            } catch (e) { console.error('Error fetching users:', e); }
        };

        try {
            await Promise.all([fetchSolicitudes(), fetchUsers()]);
            setLastUpdated(new Date());
        } catch (error) {
            console.error('Error in parallel fetching:', error);
        } finally {
            setLoading(false);
            setIsRefreshing(false);
            isFirstLoad.current = false;
        }
    }, [solicitudes.length]);

    useEffect(() => {
        fetchData();
        const interval = setInterval(() => {
            fetchData(true);
        }, 15000);
        return () => clearInterval(interval);
    }, [fetchData]);

    const value = {
        solicitudes,
        users,
        loading,
        isRefreshing,
        lastUpdated,
        refreshData: () => fetchData(false)
    };

    return (
        <DataContext.Provider value={value}>
            {children}
        </DataContext.Provider>
    );
};
