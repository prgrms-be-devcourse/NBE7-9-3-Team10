'use client';

import { FC } from 'react';
import { ReportSummary } from '@/types/admin';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/Card';

interface ReportCardProps {
  report: ReportSummary;
  onClick: () => void;
}

const getStatusStyle = (status: ReportSummary['status']) => {
  switch (status) {
    case 'RECEIVED': return 'bg-blue-100 dark:bg-blue-900/30 text-blue-800 dark:text-blue-300';
    case 'IN_PROGRESS': return 'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-800 dark:text-yellow-300';
    case 'RESOLVED': return 'bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-300';
    case 'REJECTED': return 'bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-300';
    default: return 'bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-300';
  }
};

const getStatusText = (status: ReportSummary['status']) => {
  switch (status) {
    case 'RECEIVED': return '접수';
    case 'IN_PROGRESS': return '처리 중';
    case 'RESOLVED': return '처리 완료';
    case 'REJECTED': return '반려';
    default: return status;
  }
};

const ReportCard: FC<ReportCardProps> = ({ report, onClick }) => {
  return (
    <Card onClick={onClick} className="cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors">
      <CardHeader>
        <div className="flex justify-between items-center">
          <CardTitle className="text-lg">신고 ID: {report.reportId}</CardTitle>
          <span className={`px-2 py-1 text-xs font-semibold rounded-full ${getStatusStyle(report.status)}`}>
            {getStatusText(report.status)}
          </span>
        </div>
        <CardDescription>
          {new Date(report.createdAt).toLocaleString('ko-KR')}
        </CardDescription>
      </CardHeader>
      <CardContent>
        <div className="grid grid-cols-2 gap-4 text-sm">
          <div>
            <p className="font-semibold text-gray-900 dark:text-white">신고자</p>
            <p className="text-gray-700 dark:text-gray-300">{report.reporterName}</p>
          </div>
          <div>
            <p className="font-semibold text-gray-900 dark:text-white">피신고자</p>
            <p className="text-gray-700 dark:text-gray-300">{report.reportedName}</p>
          </div>
        </div>
      </CardContent>
    </Card>
  );
};

export default ReportCard;
