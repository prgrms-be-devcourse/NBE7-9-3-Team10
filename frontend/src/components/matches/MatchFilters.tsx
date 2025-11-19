'use client';

import Button from '../ui/Button';
import Select from '../ui/Select';
import Input from '../ui/Input';
import type { MatchFilters as MatchFiltersType } from '../../types/match';

interface MatchFiltersProps {
  filters: MatchFiltersType;
  onFilterChange: (filters: MatchFiltersType) => void;
  onApplyFilters: () => void;
  isLoading?: boolean;
}

// 백엔드 SimilarityCalculator와 동일한 가중치
const FILTER_WEIGHTS = {
  SMOKING: 0.20,      // 흡연 (20%)
  SLEEP: 0.20,        // 수면 (20%)
  CLEANLINESS: 0.20,  // 청결 (20%)
  AGE: 0.10,          // 나이 (10%)
  NOISE: 0.10,        // 소음 (10%)
  PET: 0.10,          // 반려동물 (10%)
  LIFESTYLE: 0.10     // 생활방식 (10%)
} as const;

export const MatchFilters = ({ 
  filters, 
  onFilterChange, 
  onApplyFilters, 
  isLoading 
}: MatchFiltersProps) => {
  const handleFilterChange = (key: keyof MatchFiltersType, value: string) => {
    onFilterChange({
      ...filters,
      [key]: value || undefined
    });
  };

  return (
    <>
      {/* 수면 시간대 */}
      <div>
        <label className="block text-sm font-medium text-gray-900 dark:text-white mb-2">
           수면 시간대
        </label>
        <Select
          value={filters.sleepPattern || ''}
          onChange={(e) => handleFilterChange('sleepPattern', e.target.value)}
          className="w-full px-2.5 py-1.5 text-sm border border-gray-200 dark:border-gray-600 rounded focus:outline-none focus:ring-1 focus:ring-blue-600 dark:focus:ring-blue-400 bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
          options={[
            { value: '', label: '전체' },
            { value: 'very_early', label: '22시 이전' },
            { value: 'early', label: '22시~00시' },
            { value: 'normal', label: '00시~02시' },
            { value: 'late', label: '02시~04시' },
            { value: 'very_late', label: '04시 이후' }
          ]}
        />
      </div>

      {/* 청소 빈도 */}
      <div>
        <label className="block text-sm font-medium text-gray-900 dark:text-white mb-2">
          청소 빈도
        </label>
        <Select
          value={filters.cleaningFrequency || ''}
          onChange={(e) => handleFilterChange('cleaningFrequency', e.target.value)}
          className="w-full px-2.5 py-1.5 text-sm border border-gray-200 dark:border-gray-600 rounded focus:outline-none focus:ring-1 focus:ring-blue-600 dark:focus:ring-blue-400 bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
          options={[
            { value: '', label: '전체' },
            { value: 'daily', label: '매일' },
            { value: 'several_times_weekly', label: '주 2~3회' },
            { value: 'weekly', label: '주 1회' },
            { value: 'monthly', label: '월 1~2회' },
            { value: 'rarely', label: '거의 안 함' }
          ]}
        />
      </div>

      {/* 나이대 */}
      <div>
        <label className="block text-sm font-medium text-gray-900 dark:text-white mb-2">
           나이대
        </label>
        <Select
          value={filters.ageRange || ''}
          onChange={(e) => handleFilterChange('ageRange', e.target.value)}
          className="w-full px-2.5 py-1.5 text-sm border border-gray-200 dark:border-gray-600 rounded focus:outline-none focus:ring-1 focus:ring-blue-600 dark:focus:ring-blue-400 bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
          options={[
            { value: '', label: '전체' },
            { value: '20-22', label: '20-22세' },
            { value: '23-25', label: '23-25세' },
            { value: '26-28', label: '26-28세' },
            { value: '29-30', label: '29-30세' },
            { value: '31+', label: '31세 이상' }
          ]}
        />
      </div>

      {/* 필터 적용 버튼 */}
      <Button
        onClick={onApplyFilters}
        disabled={isLoading}
        className="w-full bg-blue-600 dark:bg-blue-500 hover:bg-blue-700 dark:hover:bg-blue-600 text-white font-medium py-2 text-sm rounded transition-colors"
      >
        필터 적용
      </Button>
    </>
  );
};
