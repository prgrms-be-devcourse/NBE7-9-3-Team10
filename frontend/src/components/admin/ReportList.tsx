'use client';

import { useState, FC, useCallback } from 'react';
import { useReports } from '@/hooks/useReports';
import ReportCard from './ReportCard';
import ReportFilters from './ReportFilters';
import ReportDetailModal from './ReportDetailModal';
import Button from '@/components/ui/Button';
import LoadingSpinner from '@/components/ui/LoadingSpinner';

const ReportList: FC = () => {
  const {
    reports,
    isLoading,
    error,
    page,
    totalPages,
    handlePageChange,
    handleFilterChange,
    filters,
    fetchReports, // 새로고침 함수 가져오기
  } = useReports();

  const [selectedReportId, setSelectedReportId] = useState<number | null>(null);

  const handleCardClick = (reportId: number) => {
    setSelectedReportId(reportId);
  };

  const handleCloseModal = useCallback(() => {
    setSelectedReportId(null);
  }, []);

  const handleActionSuccess = useCallback(() => {
    fetchReports(); // 액션 성공 시 목록 새로고침
  }, [fetchReports]);

  if (isLoading) {
    return <div className="flex justify-center p-10"><LoadingSpinner /></div>;
  }

  if (error) {
    return <div className="text-center p-10 text-red-500 dark:text-red-400">오류: {error}</div>;
  }

  return (
    <>
      <div>
        <ReportFilters onFilterChange={handleFilterChange} initialFilters={filters} />
        
        <div className="space-y-4">
          {reports.length > 0 ? (
            reports.map(report => (
              <ReportCard
                key={report.reportId}
                report={report}
                onClick={() => handleCardClick(report.reportId)}
              />
            ))
          ) : (
            <div className="text-center py-12 bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700">
              <p className="text-gray-500 dark:text-gray-400">해당 조건의 신고 내역이 없습니다.</p>
            </div>
          )}
        </div>

        {totalPages > 1 && (
          <div className="flex justify-center items-center mt-8 space-x-2">
            {Array.from({ length: totalPages }, (_, i) => (
              <Button
                key={i}
                onClick={() => handlePageChange(i)}
                variant={page === i ? 'primary' : 'outline'}
              >
                {i + 1}
              </Button>
            ))}
          </div>
        )}
      </div>

      {selectedReportId && (
        <ReportDetailModal
          reportId={selectedReportId}
          onClose={handleCloseModal}
          onActionSuccess={handleActionSuccess}
        />
      )}
    </>
  );
};

export default ReportList;
