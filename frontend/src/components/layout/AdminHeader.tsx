'use client';

import { useEffect, useState } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import AdminAuthService from '@/lib/services/AdminAuthService';
import Link from 'next/link';
import { Shield, LayoutDashboard } from 'lucide-react';

export default function AdminHeader() {
  const router = useRouter();
  const pathname = usePathname();
  const [adminEmail, setAdminEmail] = useState('');

  useEffect(() => {
    const email = localStorage.getItem('adminEmail') || '';
    setAdminEmail(email);
  }, []);

  const handleLogout = async () => {
    await AdminAuthService.logout();
    router.push('/admin/login');
  };

  const navigationItems = [
    { key: '/admin/dashboard', label: '대시보드', icon: LayoutDashboard },
    { key: '/admin/reports', label: '신고 관리', icon: Shield },
  ];

  return (
    <header className="bg-white dark:bg-gray-800 shadow-md sticky top-0 z-20 border-b border-gray-200 dark:border-gray-700">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-3 flex justify-between items-center">
        {/* Left side: Title */}
        <Link href="/admin/dashboard" className="flex items-center gap-2">
          <div className="w-8 h-8 bg-slate-800 dark:bg-slate-700 rounded-lg flex items-center justify-center">
            <Shield className="w-5 h-5 text-white" />
          </div>
          <span className="text-xl font-bold text-slate-800 dark:text-white">Admin</span>
        </Link>

        {/* Right side: Nav, User Info, Logout */}
        <div className="flex items-center gap-6">
          <nav className="flex items-center gap-4">
            {navigationItems.map((item) => {
              const Icon = item.icon;
              const isActive = pathname === item.key;
              return (
                <Link
                  key={item.key}
                  href={item.key}
                  className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg transition-colors text-sm ${
                    isActive
                      ? 'bg-slate-100 dark:bg-slate-700 text-slate-900 dark:text-white font-semibold'
                      : 'text-gray-600 dark:text-gray-400 hover:bg-slate-50 dark:hover:bg-slate-700'
                  }`}
                >
                  <Icon className="w-4 h-4" />
                  <span>{item.label}</span>
                </Link>
              );
            })}
          </nav>
          
          <div className="flex items-center gap-3 border-l border-gray-200 dark:border-gray-700 pl-6">
            <span className="text-sm text-gray-600 dark:text-gray-400 hidden sm:block">{adminEmail}</span>
            <button
              onClick={handleLogout}
              className="px-3 py-1.5 bg-red-600 dark:bg-red-500 text-white text-sm rounded-lg hover:bg-red-700 dark:hover:bg-red-600 transition-colors"
            >
              로그아웃
            </button>
          </div>
        </div>
      </div>
    </header>
  );
}
