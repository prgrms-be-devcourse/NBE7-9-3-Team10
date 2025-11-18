# CI/CD 설정 가이드

## 🎯 현재 구성

### 1단계: 기본 CI 파이프라인 ✅

생성된 워크플로우 파일들:
- `.github/workflows/ci-backend.yml` - 백엔드 빌드 & 테스트
- `.github/workflows/ci-frontend.yml` - 프론트엔드 린트 & 빌드
- `.github/workflows/ci-combined.yml` - 통합 CI (병렬 실행)

**동작 방식:**
- `main`, `develop` 브랜치에 push 또는 PR 생성 시 자동 실행
- 백엔드와 프론트엔드가 병렬로 실행되어 시간 단축
- 변경된 파일 경로(`paths`)에 따라 필요한 워크플로우만 실행

---

## 📋 구성 내용

### Backend CI (ci-backend.yml)
- **환경**: Java 21, Gradle
- **서비스**: Redis 7-alpine (테스트용)
- **작업**:
  1. Gradle 빌드 (테스트 제외)
  2. 테스트 실행
  3. 테스트 결과 및 리포트 업로드

### Frontend CI (ci-frontend.yml)
- **환경**: Node.js 20, npm
- **작업**:
  1. 의존성 설치 (`npm ci`)
  2. ESLint 실행 (경고만, 실패 시 계속 진행)
  3. TypeScript 타입 체크
  4. Next.js 빌드
  5. 빌드 아티팩트 업로드

### Combined CI (ci-combined.yml)
- 백엔드와 프론트엔드 작업을 병렬로 실행
- 모든 변경사항에 대해 통합 검증 수행

---

## 🚀 즉시 사용 가능

현재 설정으로 다음이 자동 실행됩니다:

1. **코드 푸시 시:**
   - 백엔드: Gradle 빌드 → 테스트 실행
   - 프론트엔드: 의존성 설치 → 린트 → 타입체크 → 빌드

2. **PR 생성 시:**
   - 위와 동일한 검증 수행
   - PR 머지 전 코드 품질 확인

---

## 📝 다음 단계 (선택사항)

### 2단계: Docker 이미지 빌드
- `backend/Dockerfile` 작성
- `frontend/Dockerfile` 작성
- Docker 이미지 빌드 및 푸시 워크플로우 추가

### 3단계: 환경별 설정 분리
**GitHub Secrets 설정:**
```
DATABASE_URL=...
REDIS_URL=...
JWT_SECRET=...
MAIL_USERNAME=...
MAIL_PASSWORD=...
NEXT_PUBLIC_API_URL=http://localhost:8080
```

### 4단계: CD 파이프라인 (배포 자동화)
- **프론트엔드**: Vercel 자동 배포 (Next.js 최적화)
- **백엔드**: AWS/GCP/Azure 또는 Docker Compose 배포
- 브랜치별 배포 전략 설정 (`develop` → 개발, `main` → 프로덕션)

---

## 🔧 사용법

### 워크플로우 실행 확인
1. GitHub 저장소 > Actions 탭
2. 각 워크플로우 실행 상태 확인
3. 실패 시 로그 확인하여 문제 해결

### 로컬에서 테스트
```bash
# 백엔드
cd backend/unimate
./gradlew test

# 프론트엔드
cd frontend
npm run lint
npm run build
```

---

## ⚠️ 주의사항

1. **Redis 서비스**: 백엔드 테스트 시 Redis가 자동으로 실행됩니다.
2. **환경 변수**: 프론트엔드 빌드 시 `NEXT_PUBLIC_API_URL`이 필요하면 GitHub Secrets에 설정하세요.
3. **ESLint**: 현재 설정은 `continue-on-error: true`로 되어 있어 실패해도 빌드는 계속됩니다.

---

## 🐛 문제 해결

### 백엔드 테스트 실패
- Redis 서비스가 제대로 실행되는지 확인
- 테스트 프로파일 설정 확인 (`application-test.yml`)

### 프론트엔드 빌드 실패
- 환경 변수 설정 확인 (`NEXT_PUBLIC_API_URL`)
- 의존성 버전 충돌 확인 (`package-lock.json`)

### 워크플로우가 실행되지 않음
- `paths` 필터가 올바른지 확인
- 브랜치 이름이 `main` 또는 `develop`인지 확인

---

## ✨ 향후 개선사항

1. **코드 커버리지 리포트**
   - JaCoCo (백엔드)
   - Codecov 연동

2. **자동화된 릴리즈**
   - 버전 태그 자동 생성
   - 릴리즈 노트 자동 생성

3. **성능 테스트**
   - 부하 테스트 통합
   - 성능 메트릭 수집

