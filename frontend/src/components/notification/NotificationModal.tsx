'use client'

import { useState, useEffect } from 'react'
import { X, Heart, MessageCircle, Users, Clock } from 'lucide-react'
import { Notification } from '@/types/notification'

interface NotificationModalProps {
  isOpen: boolean
  onClose: () => void
  notifications: Notification[]
  onMarkAsRead: (id: string) => void
  onDeleteNotification: (id: string) => void
  onDeleteAllNotifications: () => void
  onViewProfile: (senderId: number) => void
  onViewChat: (chatroomId: number) => void
}

export default function NotificationModal({
  isOpen,
  onClose,
  notifications,
  onMarkAsRead,
  onDeleteNotification,
  onDeleteAllNotifications,
  onViewProfile,
  onViewChat
}: NotificationModalProps) {
  const [localNotifications, setLocalNotifications] = useState<Notification[]>(notifications)

  useEffect(() => {
    setLocalNotifications(notifications)
  }, [notifications])

  const getNotificationIcon = (type: string) => {
    switch (type) {
      case 'like':
        return (
          <div className="w-10 h-10 bg-pink-200 rounded-full flex items-center justify-center">
            <Heart className="w-5 h-5 text-red-500 fill-red-500" />
          </div>
        )
      case 'chat':
        return (
          <div className="w-12 h-12 bg-gradient-to-br from-blue-400 via-blue-500 to-indigo-500 rounded-full flex items-center justify-center shadow-lg shadow-blue-200">
            <MessageCircle className="w-6 h-6 text-white drop-shadow-sm" />
          </div>
        )
      case 'match':
        return (
          <div className="w-12 h-12 bg-gradient-to-br from-emerald-400 via-green-500 to-teal-500 rounded-full flex items-center justify-center shadow-lg shadow-green-200">
            <Users className="w-6 h-6 text-white drop-shadow-sm" />
          </div>
        )
      default:
        return (
          <div className="w-12 h-12 bg-gradient-to-br from-gray-400 via-gray-500 to-slate-500 rounded-full flex items-center justify-center shadow-lg shadow-gray-200">
            <MessageCircle className="w-6 h-6 text-white drop-shadow-sm" />
          </div>
        )
    }
  }

  const getNotificationAction = (notification: Notification) => {
    switch (notification.type) {
      case 'like':
        return (
          <button
            onClick={() => notification.senderId && onViewProfile(notification.senderId)}
            className="text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300 text-sm font-medium"
          >
            프로필 보기
          </button>
        )
      case 'chat':
        return (
          <button
            onClick={() => notification.chatroomId && onViewChat(notification.chatroomId)}
            className="text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300 text-sm font-medium"
          >
            채팅창으로 이동
          </button>
        )
      case 'match':
        return (
          <div className="flex gap-2">
            <button
              onClick={() => {
                if (notification.chatroomId) {
                  window.location.href = `/chat/${notification.chatroomId}`;
                } else {
                  window.location.href = '/chat';
                }
              }}
              className="text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300 text-sm font-medium"
            >
              채팅창으로 이동
            </button>
          </div>
        )
      default:
        return null
    }
  }

  const formatTime = (timestamp: string | Date) => {
    const now = new Date()
    const date = typeof timestamp === 'string' ? new Date(timestamp) : timestamp
    const diff = now.getTime() - date.getTime()
    const minutes = Math.floor(diff / 60000)
    const hours = Math.floor(diff / 3600000)
    const days = Math.floor(diff / 86400000)

    if (minutes < 1) return '방금 전'
    if (minutes < 60) return `${minutes}분 전`
    if (hours < 24) return `${hours}시간 전`
    if (days < 7) return `${days}일 전`
    
    return date.toLocaleDateString('ko-KR', {
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      hour12: true
    })
  }

  const handleNotificationClick = (notification: Notification) => {
    if (!notification.isRead) {
      onMarkAsRead(notification.id)
    }
  }

  const handleDeleteAll = () => {
    onDeleteAllNotifications()
    // 모달은 닫지 않고 열린 상태로 유지
  }

  if (!isOpen) return null

  return (
    <>
      {/* Backdrop - 클릭 시 닫기 */}
      <div 
        className="fixed inset-0 z-40" 
        onClick={onClose}
      />
      
      {/* Notification Panel */}
      <div className="fixed top-14 right-4 z-50 bg-white dark:bg-gray-800 rounded-xl shadow-xl w-80 max-h-[70vh] overflow-hidden border border-gray-200 dark:border-gray-700 animate-in slide-in-from-top-2 duration-200">
        {/* Header */}
        <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-700">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl font-bold text-gray-900 dark:text-white">알림</h2>
              <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
                오프라인 동안 받은 알림은 로그인 시 표시됩니다.
              </p>
            </div>
            <button
              onClick={() => {
                if (localNotifications.length > 0) {
                  handleDeleteAll()
                } else {
                  onClose()
                }
              }}
              className="p-2 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
              title={localNotifications.length > 0 ? "모든 알림 삭제" : "닫기"}
            >
              <X className="w-5 h-5 text-gray-500 dark:text-gray-400" />
            </button>
          </div>
        </div>

        {/* Notifications List */}
        <div className="overflow-y-auto max-h-80">
          {localNotifications.length === 0 ? (
            <div className="px-6 py-12 text-center">
              <div className="w-16 h-16 bg-gray-100 dark:bg-gray-700 rounded-full flex items-center justify-center mx-auto mb-4">
                <MessageCircle className="w-8 h-8 text-gray-400 dark:text-gray-500" />
              </div>
              <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-2">알림이 없습니다</h3>
              <p className="text-gray-500 dark:text-gray-400">새로운 알림이 오면 여기에 표시됩니다.</p>
            </div>
          ) : (
            <div className="divide-y divide-gray-200 dark:divide-gray-700">
              {localNotifications.map((notification) => (
                <div
                  key={notification.id}
                  className={`px-6 py-4 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors cursor-pointer ${
                    !notification.isRead ? 'bg-blue-50 dark:bg-blue-900/20' : ''
                  }`}
                  onClick={() => handleNotificationClick(notification)}
                >
                  <div className="flex items-start gap-4">
                    {/* Icon */}
                    <div className="flex-shrink-0">
                      {getNotificationIcon(notification.type)}
                    </div>

                    {/* Content */}
                    <div className="flex-1 min-w-0">
                      <p className="text-sm text-gray-900 dark:text-white mb-1">
                        {notification.message}
                      </p>
                      <div className="flex items-center gap-2 text-xs text-gray-500 dark:text-gray-400 mb-2">
                        <Clock className="w-3 h-3" />
                        <span>{formatTime(notification.timestamp)}</span>
                      </div>
                      {getNotificationAction(notification) && (
                        <div className="mt-1">
                          {getNotificationAction(notification)}
                        </div>
                      )}
                    </div>

                    {/* Actions */}
                    <div className="flex-shrink-0 flex items-center gap-2">
                      {!notification.isRead && (
                        <div className="w-2 h-2 bg-red-500 dark:bg-red-400 rounded-full"></div>
                      )}
                      <button
                        onClick={(e) => {
                          e.stopPropagation()
                          onDeleteNotification(notification.id)
                        }}
                        className="p-1 hover:bg-gray-200 dark:hover:bg-gray-600 rounded transition-colors"
                      >
                        <X className="w-4 h-4 text-gray-400 dark:text-gray-500" />
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </>
  )
}
