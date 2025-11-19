'use client';

import { FC, useState, ChangeEvent } from 'react';
import Input from '@/components/ui/Input';
import Select from '@/components/ui/Select';
import Button from '@/components/ui/Button';

interface ReportFiltersProps {
  onFilterChange: (filters: { status?: string | null; keyword?: string | null }) => void;
  initialFilters: { status: string | null; keyword: string | null };
}

const ReportFilters: FC<ReportFiltersProps> = ({ onFilterChange, initialFilters }) => {
  const [keyword, setKeyword] = useState(initialFilters.keyword || '');
  const [status, setStatus] = useState(initialFilters.status || '');

  const handleSearch = () => {
    onFilterChange({ status: status || null, keyword: keyword || null });
  };

  return (
    <div className="p-4 bg-gray-50 dark:bg-gray-800 rounded-lg mb-6 flex flex-col sm:flex-row items-stretch sm:items-center gap-4">
      <Select
        value={status}
        onChange={(e: ChangeEvent<HTMLSelectElement>) => setStatus(e.target.value)}
        className="w-full sm:w-48"
        options={[
          { value: '', label: '전체 상태' },
          { value: 'RECEIVED', label: '접수' },
          { value: 'IN_PROGRESS', label: '처리 중' },
          { value: 'RESOLVED', label: '처리 완료' },
          { value: 'REJECTED', label: '반려' },
        ]}
      />
      <Input
        type="text"
        placeholder="신고자 또는 피신고자 이름 검색"
        value={keyword}
        onChange={(e: ChangeEvent<HTMLInputElement>) => setKeyword(e.target.value)}
        className="flex-grow"
      />
      <Button onClick={handleSearch} className="w-full sm:w-auto">검색</Button>
    </div>
  );
};

export default ReportFilters;
