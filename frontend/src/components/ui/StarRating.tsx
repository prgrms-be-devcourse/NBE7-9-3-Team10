'use client';

import { Star } from "lucide-react";

interface StarRatingProps {
    rating: number;
    onRatingChange: (value: number) => void;
    onClickStar?: (value: number) => void;
}

export function StarRating({ rating, onRatingChange, onClickStar }: StarRatingProps) {
    const stars = [1, 2, 3, 4, 5];

    const handleClick = (value: number) => {
        onRatingChange(value);
        if (onClickStar) {
            onClickStar(value);
        }
    };

    return (
        <div className="flex gap-1">
            {stars.map((value) => (
                <button
                    key={value}
                    type="button"
                    onClick={() => handleClick(value)}
                    className="p-1 hover:bg-gray-100 rounded-lg transition-colors focus:outline-none"
                    aria-label={`${value}점 선택`}
                >
                    <Star
                        size={28}
                        fill={value <= rating ? "currentColor" : "none"}
                        className={
                            value <= rating
                                ? "text-yellow-400"
                                : "text-gray-300"
                        }
                        // 타입 에러 방지를 위한 타입 단언 (필요시)
                        {...({} as any)}
                    />
                </button>
            ))}
        </div>
    );
}