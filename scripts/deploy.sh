#!/bin/bash

# ============================================
# Unimate 프로덕션 배포 스크립트
# ============================================
# 사용법: ./deploy.sh
# EC2 서버에서 실행하는 스크립트입니다.

set -e  # 오류 발생 시 즉시 종료

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 로그 함수
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 변수 설정
PROJECT_DIR="/home/ubuntu/unimate"  # 프로젝트 디렉토리 (실제 경로로 수정 필요)
COMPOSE_FILE="docker-compose.prod.yml"
BACKUP_DIR="${PROJECT_DIR}/backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# 함수: .env 파일 확인
check_env_file() {
    log_info ".env 파일 확인 중..."
    if [ ! -f "${PROJECT_DIR}/.env" ]; then
        log_error ".env 파일이 없습니다!"
        log_info "env.example을 복사하여 .env 파일을 생성하세요:"
        log_info "cp ${PROJECT_DIR}/env.example ${PROJECT_DIR}/.env"
        exit 1
    fi
    log_info ".env 파일 확인 완료"
}

# 함수: Docker 설치 확인
check_docker() {
    log_info "Docker 설치 확인 중..."
    if ! command -v docker &> /dev/null; then
        log_error "Docker가 설치되어 있지 않습니다!"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        log_error "Docker Compose가 설치되어 있지 않습니다!"
        exit 1
    fi
    log_info "Docker 설치 확인 완료"
}

# 함수: 기존 컨테이너 백업
backup_containers() {
    log_info "기존 컨테이너 상태 백업 중..."
    mkdir -p "${BACKUP_DIR}"
    
    # 실행 중인 컨테이너 목록 저장
    docker ps > "${BACKUP_DIR}/containers_${TIMESTAMP}.txt" 2>/dev/null || true
    log_info "백업 완료: ${BACKUP_DIR}/containers_${TIMESTAMP}.txt"
}

# 함수: Git 최신 코드 가져오기
pull_latest_code() {
    log_info "Git 최신 코드 가져오기 중..."
    cd "${PROJECT_DIR}"
    
    # 현재 브랜치 확인
    CURRENT_BRANCH=$(git branch --show-current)
    log_info "현재 브랜치: ${CURRENT_BRANCH}"
    
    # 변경사항이 있으면 stash
    if ! git diff-index --quiet HEAD --; then
        log_warn "작업 디렉토리에 변경사항이 있습니다. stash 중..."
        git stash save "deploy_${TIMESTAMP}"
    fi
    
    # 최신 코드 pull
    git pull origin "${CURRENT_BRANCH}" || {
        log_error "Git pull 실패!"
        exit 1
    }
    
    log_info "Git pull 완료"
}

# 함수: Docker 이미지 빌드
build_images() {
    log_info "Docker 이미지 빌드 중..."
    cd "${PROJECT_DIR}"
    
    # Backend 이미지 빌드
    log_info "Backend 이미지 빌드 중..."
    docker-compose -f "${COMPOSE_FILE}" build backend || {
        log_error "Backend 이미지 빌드 실패!"
        exit 1
    }
    
    # Frontend 이미지 빌드
    log_info "Frontend 이미지 빌드 중..."
    docker-compose -f "${COMPOSE_FILE}" build frontend || {
        log_error "Frontend 이미지 빌드 실패!"
        exit 1
    }
    
    log_info "모든 이미지 빌드 완료"
}

# 함수: 서비스 시작
start_services() {
    log_info "서비스 시작 중..."
    cd "${PROJECT_DIR}"
    
    # 기존 서비스 중지 (볼륨은 유지)
    log_info "기존 서비스 중지 중..."
    docker-compose -f "${COMPOSE_FILE}" down || true
    
    # 새로운 서비스 시작
    log_info "새로운 서비스 시작 중..."
    docker-compose -f "${COMPOSE_FILE}" up -d || {
        log_error "서비스 시작 실패!"
        exit 1
    }
    
    log_info "서비스 시작 완료"
}

# 함수: 헬스체크
health_check() {
    log_info "헬스체크 수행 중..."
    
    local max_attempts=30
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        log_info "헬스체크 시도 ${attempt}/${max_attempts}..."
        
        # Nginx 헬스체크
        if curl -f http://localhost/health > /dev/null 2>&1; then
            log_info "Nginx 헬스체크 성공"
            
            # Backend 헬스체크 (선택사항)
            if curl -f http://localhost/api/health > /dev/null 2>&1; then
                log_info "Backend 헬스체크 성공"
            else
                log_warn "Backend 헬스체크 실패 (API 엔드포인트 확인 필요)"
            fi
            
            log_info "모든 헬스체크 통과"
            return 0
        fi
        
        sleep 5
        attempt=$((attempt + 1))
    done
    
    log_error "헬스체크 실패! 최대 시도 횟수 초과"
    return 1
}

# 함수: 컨테이너 상태 확인
check_containers() {
    log_info "컨테이너 상태 확인 중..."
    cd "${PROJECT_DIR}"
    
    docker-compose -f "${COMPOSE_FILE}" ps
    
    # 실행 중인 컨테이너 수 확인
    local running=$(docker-compose -f "${COMPOSE_FILE}" ps -q | wc -l)
    log_info "실행 중인 컨테이너 수: ${running}"
}

# 함수: 롤백
rollback() {
    log_warn "롤백 시작..."
    cd "${PROJECT_DIR}"
    
    # 기존 서비스 중지
    docker-compose -f "${COMPOSE_FILE}" down
    
    log_warn "수동 롤백이 필요할 수 있습니다."
    log_info "백업 디렉토리 확인: ${BACKUP_DIR}"
}

# 메인 실행
main() {
    log_info "============================================"
    log_info "Unimate 프로덕션 배포 시작"
    log_info "시간: $(date)"
    log_info "============================================"
    
    # 사전 체크
    check_docker
    check_env_file
    backup_containers
    
    # 배포 실행
    if pull_latest_code && build_images && start_services; then
        # 헬스체크
        if health_check; then
            check_containers
            log_info "============================================"
            log_info "배포 완료!"
            log_info "시간: $(date)"
            log_info "============================================"
        else
            log_error "헬스체크 실패! 롤백 권장"
            rollback
            exit 1
        fi
    else
        log_error "배포 실패! 롤백 중..."
        rollback
        exit 1
    fi
}

# 스크립트 실행
main "$@"

