# ============================================
# Unimate 프로덕션 배포 스크립트 (Windows용)
# ============================================
# 사용법: .\deploy.ps1
# Windows 환경에서 로컬 테스트용 (EC2는 Linux이므로 deploy.sh 사용)

param(
    [string]$ProjectDir = ".",
    [string]$ComposeFile = "docker-compose.prod.yml"
)

$ErrorActionPreference = "Stop"

function Write-Info {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor Green
}

function Write-Warn {
    param([string]$Message)
    Write-Host "[WARN] $Message" -ForegroundColor Yellow
}

function Write-Error {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor Red
}

# 변수 설정
$BackupDir = Join-Path $ProjectDir "backups"
$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"

# .env 파일 확인
function Test-EnvFile {
    Write-Info ".env 파일 확인 중..."
    $envPath = Join-Path $ProjectDir ".env"
    if (-not (Test-Path $envPath)) {
        Write-Error ".env 파일이 없습니다!"
        Write-Info "env.example을 복사하여 .env 파일을 생성하세요"
        exit 1
    }
    Write-Info ".env 파일 확인 완료"
}

# Docker 설치 확인
function Test-Docker {
    Write-Info "Docker 설치 확인 중..."
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        Write-Error "Docker가 설치되어 있지 않습니다!"
        exit 1
    }
    Write-Info "Docker 설치 확인 완료"
}

# 기존 컨테이너 백업
function Backup-Containers {
    Write-Info "기존 컨테이너 상태 백업 중..."
    New-Item -ItemType Directory -Path $BackupDir -Force | Out-Null
    docker ps > (Join-Path $BackupDir "containers_$Timestamp.txt") 2>$null
    Write-Info "백업 완료"
}

# Docker 이미지 빌드
function Build-Images {
    Write-Info "Docker 이미지 빌드 중..."
    Push-Location $ProjectDir
    
    Write-Info "Backend 이미지 빌드 중..."
    docker-compose -f $ComposeFile build backend
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Backend 이미지 빌드 실패!"
        exit 1
    }
    
    Write-Info "Frontend 이미지 빌드 중..."
    docker-compose -f $ComposeFile build frontend
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Frontend 이미지 빌드 실패!"
        exit 1
    }
    
    Pop-Location
    Write-Info "모든 이미지 빌드 완료"
}

# 서비스 시작
function Start-Services {
    Write-Info "서비스 시작 중..."
    Push-Location $ProjectDir
    
    Write-Info "기존 서비스 중지 중..."
    docker-compose -f $ComposeFile down
    
    Write-Info "새로운 서비스 시작 중..."
    docker-compose -f $ComposeFile up -d
    if ($LASTEXITCODE -ne 0) {
        Write-Error "서비스 시작 실패!"
        exit 1
    }
    
    Pop-Location
    Write-Info "서비스 시작 완료"
}

# 헬스체크
function Test-Health {
    Write-Info "헬스체크 수행 중..."
    
    $maxAttempts = 30
    $attempt = 1
    
    while ($attempt -le $maxAttempts) {
        Write-Info "헬스체크 시도 $attempt/$maxAttempts..."
        
        try {
            $response = Invoke-WebRequest -Uri "http://localhost/health" -UseBasicParsing -TimeoutSec 5
            if ($response.StatusCode -eq 200) {
                Write-Info "Nginx 헬스체크 성공"
                Write-Info "모든 헬스체크 통과"
                return $true
            }
        }
        catch {
            # 계속 시도
        }
        
        Start-Sleep -Seconds 5
        $attempt++
    }
    
    Write-Error "헬스체크 실패! 최대 시도 횟수 초과"
    return $false
}

# 컨테이너 상태 확인
function Show-Containers {
    Write-Info "컨테이너 상태 확인 중..."
    Push-Location $ProjectDir
    docker-compose -f $ComposeFile ps
    Pop-Location
}

# 메인 실행
function Main {
    Write-Info "============================================"
    Write-Info "Unimate 프로덕션 배포 시작"
    Write-Info "시간: $(Get-Date)"
    Write-Info "============================================"
    
    Test-Docker
    Test-EnvFile
    Backup-Containers
    Build-Images
    Start-Services
    
    if (Test-Health) {
        Show-Containers
        Write-Info "============================================"
        Write-Info "배포 완료!"
        Write-Info "시간: $(Get-Date)"
        Write-Info "============================================"
    }
    else {
        Write-Error "헬스체크 실패!"
        exit 1
    }
}

Main

