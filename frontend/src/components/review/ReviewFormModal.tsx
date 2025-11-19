"use client";

import { Dialog, Transition } from "@headlessui/react";
import { Fragment } from "react";
import { ReviewForm } from "./ReviewForm";
import type { ReviewResponse } from "@/types/review";

interface ReviewFormModalProps {
    open: boolean;
    onClose: () => void;
    matchId: number;
    targetUserId: number;
    revieweeName: string;
    onSuccess?: () => void;
    review?: ReviewResponse | null; 
    isEditMode?: boolean; 
}

export function ReviewFormModal({
    open,
    onClose,
    matchId,
    targetUserId,
    revieweeName,
    onSuccess,
    review,
    isEditMode = false,
}: ReviewFormModalProps) {
    const handleSuccess = () => {
        if (onSuccess) {
            onSuccess();
        }
        onClose(); 
    };

    return (
        <Transition appear show={open} as={Fragment}>
            <Dialog as="div" className="relative z-50" onClose={onClose}>
                <Transition.Child
                    as={Fragment}
                    enter="ease-out duration-200"
                    enterFrom="opacity-0"
                    enterTo="opacity-100"
                    leave="ease-in duration-150"
                    leaveFrom="opacity-100"
                    leaveTo="opacity-0"
                >
                    {/* 배경 블러 처리 적용 */}
                    <div className="fixed inset-0 bg-black/20 backdrop-blur-sm" />
                </Transition.Child>

                <div className="fixed inset-0 overflow-y-auto">
                    <div className="flex min-h-full items-center justify-center p-4">
                        <Transition.Child
                            as={Fragment}
                            enter="ease-out duration-200"
                            enterFrom="opacity-0 scale-95 translate-y-4"
                            enterTo="opacity-100 scale-100 translate-y-0"
                            leave="ease-in duration-150"
                            leaveFrom="opacity-100 scale-100 translate-y-0"
                            leaveTo="opacity-0 scale-95 translate-y-4"
                        >
                            <Dialog.Panel className="w-full max-w-lg rounded-lg bg-white dark:bg-gray-800 p-6 shadow-xl">
                                <Dialog.Title className="text-xl font-semibold mb-4 text-gray-900 dark:text-white">
                                    {isEditMode ? `${revieweeName} 리뷰 수정` : `${revieweeName} 리뷰 작성`}
                                </Dialog.Title>

                                <ReviewForm 
                                    matchId={matchId} 
                                    targetUserId={targetUserId}
                                    onSuccess={handleSuccess}
                                    review={review}
                                    isEditMode={isEditMode}
                                />
                            </Dialog.Panel>
                        </Transition.Child>
                    </div>
                </div>
            </Dialog>
        </Transition>
    );
}