'use client';

import React, { useState, useEffect } from 'react';
import { ProfileResponse } from '@/types/profile';
import { ProfileService } from '@/lib/services/profileService';
import { useAuth } from '@/contexts/AuthContext';
import { getErrorMessage } from '@/lib/utils/helpers';
import LoadingSpinner from '@/components/ui/LoadingSpinner';
import Button from '@/components/ui/Button';
import { Card, CardContent } from '@/components/ui/Card';
import ProfileCard from './ProfileCard';
import ProfileEmpty from './ProfileEmpty';
import type { User } from '@/types/user';

interface ProfileViewProps {
  onEdit?: () => void;
  onCreate?: () => void;
}

const ProfileView: React.FC<ProfileViewProps> = ({ onEdit, onCreate }) => {
  const { user, updateUser } = useAuth();
  const [profile, setProfile] = useState<ProfileResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string>('');
  const [hasProfile, setHasProfile] = useState<boolean | null>(null);

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const profileData = await ProfileService.getMyProfile();
        setProfile(profileData);
        setHasProfile(true);
      } catch (err: any) {
        const errorMessage = getErrorMessage(err);
        if (err?.status === 404 || errorMessage.includes('404') || errorMessage.includes('not found')) {
          setHasProfile(false);
        } else {
          setError(errorMessage);
        }
      } finally {
        setIsLoading(false);
      }
    };

    fetchProfile();
  }, []);

  const toggleMatchingStatus = async () => {
    if (!profile) return;
    
    try {
      const updatedProfile = await ProfileService.updateMatchingStatus(!profile.matchingEnabled);
      setProfile(updatedProfile);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  };

  const handleCreateProfile = () => {
    if (onCreate) {
      onCreate();
    }
  };

  const handleUserUpdate = (updatedUser: User) => {
    if (updateUser) {
      updateUser(updatedUser as any);
    }
  };

  if (isLoading) {
    return <LoadingSpinner message="프로필을 불러오는 중..." />;
  }

  if (error) {
    return (
      <div className="max-w-2xl mx-auto p-6">
        <Card>
          <CardContent className="py-8">
            <div className="text-center">
              <div className="mb-4">
                <svg
                  className="mx-auto h-12 w-12 text-red-400"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z"
                  />
                </svg>
              </div>
              <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-2">
                프로필을 불러올 수 없습니다
              </h3>
              <p className="text-red-600 dark:text-red-400 mb-6">{error}</p>
              <div className="space-x-4">
                <Button onClick={() => window.location.reload()}>
                  다시 시도
                </Button>
                <Button variant="outline" onClick={handleCreateProfile}>
                  새 프로필 만들기
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (hasProfile === false) {
    return <ProfileEmpty onCreate={handleCreateProfile} />;
  }

  if (!profile) {
    return <LoadingSpinner message="프로필을 불러오는 중..." />;
  }

  if (!user) {
    return <LoadingSpinner message="사용자 정보를 불러오는 중..." />;
  }

  return (
    <div className="max-w-2xl mx-auto p-6">
      <ProfileCard
        user={user}
        profile={profile}
        onEdit={onEdit || (() => window.location.href = '/profile/edit')}
        onToggleMatching={toggleMatchingStatus}
        onUserUpdate={handleUserUpdate}
      />
    </div>
  );
};

export default ProfileView;