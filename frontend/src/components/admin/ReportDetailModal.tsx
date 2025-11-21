'use client';

import { useState, useEffect, FC } from 'react';
import { AdminService, ActionType } from '@/lib/services/adminService';
import { ReportDetail } from '@/types/admin';
import { getErrorMessage } from '@/lib/utils/helpers';
import Modal from '@/components/ui/Modal';
import Button from '@/components/ui/Button';
import LoadingSpinner from '@/components/ui/LoadingSpinner';

interface ReportDetailModalProps {
  reportId: number;
  onClose: () => void;
  onActionSuccess: () => void; // 처리 성공 시 목록 새로고침을 위한 콜백
}

const ReportDetailModal: FC<ReportDetailModalProps> = ({ reportId, onClose, onActionSuccess }) => {
  const [report, setReport] = useState<ReportDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isProcessing, setIsProcessing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchDetail = async () => {
      setIsLoading(true);
      setError(null);
      try {
        const response = await AdminService.getReportDetail(reportId);
        setReport((response as any).data || response || null);
      } catch (err) {
        setError(getErrorMessage(err));
      } finally {
        setIsLoading(false);
      }
    };
    if (reportId) {
      fetchDetail();
    }
  }, [reportId]);

  const handleAction = async (action: ActionType) => {
    if (!report || isProcessing) return;

    const confirmAction = window.confirm(
      action === 'DEACTIVATE'
        ? `정말로 '${report.reportedInfo.name}' 사용자를 강제 탈퇴시키겠습니까? 이 작업은 되돌릴 수 없습니다.`
        : `정말로 이 신고를 반려하시겠습니까?`
    );

    if (!confirmAction) return;

    setIsProcessing(true);
    setError(null);
    try {
      await AdminService.handleReportAction(reportId, action);
      onActionSuccess(); // 성공 콜백 호출
      onClose(); // 모달 닫기
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsProcessing(false);
    }
  };

  const isActionDisabled = report?.status === 'RESOLVED' || report?.status === 'REJECTED';

  return (
    <Modal isOpen={true} onClose={onClose} size="lg">
      <div className="p-6">
        <div className="border-b border-gray-200 dark:border-gray-700 pb-4 mb-4">
          <h2 className="text-xl font-bold text-gray-900 dark:text-white">신고 상세 정보</h2>
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">신고 ID: {reportId}</p>
        </div>
        
        {isLoading && <div className="flex justify-center p-8"><LoadingSpinner /></div>}
        
        {error && !isLoading && (
          <div className="text-center p-8">
            <p className="text-red-500 dark:text-red-400 mb-4">오류: {error}</p>
            <Button onClick={() => window.location.reload()}>다시 시도</Button>
          </div>
        )}
        
        {report && !isLoading && !error && (
          <>
            <div className="space-y-4 max-h-[60vh] overflow-y-auto pr-2">
              <InfoSection title="신고 상태" data={report.status} />
              <InfoSection title="신고 유형" data={report.category} />
              <InfoSection title="신고 내용" data={report.content} preWrap />
              <hr className="border-gray-200 dark:border-gray-700"/>
              <UserInfo title="신고자 정보" user={report.reporterInfo} />
              <hr className="border-gray-200 dark:border-gray-700"/>
              <UserInfo title="피신고자 정보" user={report.reportedInfo} />
            </div>

            <div className="mt-6 pt-4 border-t border-gray-200 dark:border-gray-700">
              <div className="bg-yellow-50 dark:bg-yellow-900/20 text-yellow-700 dark:text-yellow-300 text-sm p-3 rounded-md mb-4">
                <p><span className="font-bold">주의:</span> 조치를 취하면 신고가 자동으로 처리 완료됩니다.</p>
              </div>
              
              {error && <p className="text-red-500 dark:text-red-400 text-sm mb-4 text-center">{error}</p>}

              <div className="flex justify-between items-center">
                <Button variant="outline" onClick={onClose} disabled={isProcessing}>닫기</Button>
                <div className="flex space-x-2">
                  <Button
                    variant="secondary"
                    onClick={() => handleAction('REJECT')}
                    disabled={isProcessing || isActionDisabled}
                    loading={isProcessing}
                  >
                    반려
                  </Button>
                  <Button
                    variant="danger"
                    onClick={() => handleAction('DEACTIVATE')}
                    disabled={isProcessing || isActionDisabled}
                    loading={isProcessing}
                  >
                    강제 탈퇴
                  </Button>
                </div>
              </div>
               {isActionDisabled && (
                <p className="text-sm text-gray-500 dark:text-gray-400 mt-2 text-right">
                  이미 처리된 신고입니다.
                </p>
              )}
            </div>
          </>
        )}
      </div>
    </Modal>
  );
};

const InfoSection: FC<{ title: string; data: string; preWrap?: boolean }> = ({ title, data, preWrap = false }) => (
  <div>
    <h3 className="font-semibold text-gray-800 dark:text-gray-200">{title}</h3>
    <p className={`text-gray-600 dark:text-gray-300 mt-1 ${preWrap ? 'whitespace-pre-wrap' : ''}`}>{data}</p>
  </div>
);

const UserInfo: FC<{ title:string; user: ReportDetail['reporterInfo'] }> = ({ title, user }) => (
  <div>
    <h3 className="font-semibold text-gray-800 dark:text-gray-200 mb-2">{title}</h3>
    <div className="text-sm text-gray-600 dark:text-gray-300 space-y-1">
      <p><strong>ID:</strong> {user.userId || 'N/A'}</p>
      <p><strong>이름:</strong> {user.name}</p>
      <p><strong>이메일:</strong> {user.email}</p>
      <p><strong>학교:</strong> {user.university}</p>
    </div>
  </div>
);

export default ReportDetailModal;
