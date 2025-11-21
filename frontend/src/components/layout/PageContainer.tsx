import React from 'react';
import { cn } from '@/lib/utils/helpers';

interface PageContainerProps {
    children: React.ReactNode;
    maxWidth?: 'sm' | 'md' | 'lg' | 'xl' | '2xl' | '6xl' | '7xl' | 'full';
    className?: string;
}

const maxWidthClasses = {
    sm: 'max-w-sm',
    md: 'max-w-md',
    lg: 'max-w-lg',
    xl: 'max-w-xl',
    '2xl': 'max-w-2xl',
    '6xl': 'max-w-6xl',
    '7xl': 'max-w-7xl',
    full: 'max-w-full',
};

export default function PageContainer({
    children,
    maxWidth = '7xl',
    className
}: PageContainerProps) {
    return (
        <div className={cn(
            'min-h-screen bg-gray-50 dark:bg-gray-900',
            className
        )}>
            <div className={cn(
                maxWidthClasses[maxWidth],
                'mx-auto px-4 py-8'
            )}>
                {children}
            </div>
        </div>
    );
}
