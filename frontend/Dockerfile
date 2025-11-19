# Stage 1: Dependencies
FROM node:20-alpine AS deps

WORKDIR /app

# package 파일들 복사
COPY package.json package-lock.json ./

# 의존성 설치
RUN npm ci --only=production

# Stage 2: Build
FROM node:20-alpine AS builder

WORKDIR /app

# 의존성 복사 (빌드용)
COPY package.json package-lock.json ./
RUN npm ci

# 소스 코드 복사
COPY . .

# Next.js 빌드
RUN npm run build

# Stage 3: Runtime
FROM node:20-alpine AS runner

WORKDIR /app

ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1

# 사용자 생성 (root 권한 회피)
RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nextjs

# .next 폴더와 public 폴더 복사
COPY --from=builder /app/public ./public
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs

# 포트 노출
EXPOSE 3000

ENV PORT=3000
ENV HOSTNAME="0.0.0.0"

# Next.js 서버 실행
CMD ["node", "server.js"]

