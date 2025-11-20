# 🏡 **'UniMate'** – 대학생 룸메이트 매칭 플랫폼

대학생들을 위한 **안전하고 간편한 룸메이트 매칭 서비스**

---

## **📜 프로젝트 개요**

Spring Boot 기반의 **UniMate**는  
대학교 재학생을 대상으로 **룸메이트를 매칭**해주는 플랫폼으로,  
학생 인증을 통한 사용자 관리, 프로필 기반 매칭, 실시간 채팅, 리뷰 기능 등을 제공합니다.  

- 🧭 생활 패턴 기반 룸메이트 추천  
- 💬 실시간 채팅 기능 제공  
- 📝 리뷰 및 신고 기능을 통한 신뢰도 관리  
- 🧑‍🤝‍🧑 안전한 학생 인증 기반 매칭 환경 조성

---

## **💁‍♂️ 팀원 소개 / 역할**

| 이위림 | 김채현 | 김홍래 | 백승범 | 안병선 |
| --- | --- | --- | --- | --- |
| <p align="center"><a href="https://github.com/weilim0513-tech"><img src="https://github.com/weilim0513-tech.png" width="100"></a></p> | <p align="center"><a href="https://github.com/Chehyeon-Kim23"><img src="https://github.com/Chehyeon-Kim23.png" width="100"></a></p> | <p align="center"><a href="https://github.com/HongRae-Kim"><img src="https://github.com/HongRae-Kim.png" width="100"></a></p> | <p align="center"><a href="https://github.com/BackSeungBeom"><img src="https://github.com/BackSeungBeom.png" width="100"></a></p> | <p align="center"><a href="https://github.com/rogrhrh"><img src="https://github.com/rogrhrh.png" width="100"></a></p> |
| <p align="center"><b>팀장</b></p> | <p align="center"><b>팀원</b></p> | <p align="center"><b>팀원</b></p> | <p align="center"><b>팀원</b></p> | <p align="center"><b>팀원</b></p> |
| <p align="center">매칭 선호도 등록<br>매칭 상태 취소<br>유사도 계산 로직<br>좋아요 보내기/취소<br>관리자 신고 조회/처리</p> | <p align="center">WebSocket을 적용한<br>실시간 채팅방/알림 구현</p> | <p align="center">자동/사용자 선택 필터<br>응답 처리/상태 결정<br>최종 확정/거절<br>리뷰 기능 구현</p> | <p align="center">JWT 기반 인증 시스템<br>전역 예외 처리<br>사용자 정보 관리</p> | <p align="center">유저 프로필<br>신고 접수 기능 구현<br>Redis적용과 부하테스트<br>GitHub Actions를 이용한 배포 시도</p> |
---

## 📝 유저 스토리
### 👤 고객(사용자)

- **U-1 [이메일 인증]**
 
    **실제 학교 메일 인증**으로 신뢰 확보
    
- **U-2 [생활패턴 등록]**
    
    수면, 청결, 흡연 등 **개인별 생활 습관**을 파악
    
- **U-3 [추천 프로필 리스트]**
    
    나와 **매칭률이 높은** 여러 명의 후보를 확인
    
- **U-4 ['좋아요' 보내기]**
    
    **상호 '좋아요'시** 채팅방 자동 생성

 - **U-5 [룸메이트 확정/취소]**
    
    채팅을 통해 상대방과 **상세 조율**하고 룸메이트 여부 결정

 - **U-6 [신고 및 제재]**
    
    문제되는 사용자를 관리자에게 **신고 및 차단**
    

### 👨‍💻 관리자(Admin)

- **A-1 [신고 관리]**
    
    사용자가 신고한 유저에 대한 제재 및 탈퇴 여부 결정
    


---
## **⭐** 주요 기능
### **👤 사용자 기능**

유저는 학교 이메일 인증을 통해 플랫폼을 이용할 수 있습니다.

- Spring Security + JWT
- 추천 룸메이트 목록 조회
- 룸메이트 좋아요·취소
- 매칭 생성·조회·취소
- 채팅방 생성·조회·메시지 전송
- 알림 조회·읽음 처리
- 프로필 생성·조회·수정·삭제

### 👨‍💻 관리자 기능

관리자는 인증을 거쳐 관리자 페이지에 접근할 수 있습니다.

- Spring Security + JWT
- 신고 조회·처리
 ---

## 🔧기술 스택
<div align=left>
    <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white">
    <img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white">  
    <img src="https://img.shields.io/badge/springboot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white">
    <img src="https://img.shields.io/badge/springsecurity-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white">
    <img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=MySQL&logoColor=white">
    <img src="https://img.shields.io/badge/docker-2496ED?style=for-the-badge&logo=docker&logoColor=white">
    <img src="https://img.shields.io/badge/h2database-09476B?style=for-the-badge&logo=h2database&logoColor=white">
    <img src="https://img.shields.io/badge/git-F05032?style=for-the-badge&logo=git&logoColor=white">
    <img src="https://img.shields.io/badge/github-181717?style=for-the-badge&logo=github&logoColor=white">
    <img src="https://img.shields.io/badge/nextdotjs-000000?style=for-the-badge&logo=nextdotjs&logoColor=white">
    <img src="https://img.shields.io/badge/tailwindcss-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white">
    <img src="https://img.shields.io/badge/websocket-4B32C3?style=for-the-badge&logo=socket.io&logoColor=white">
    <img src="https://img.shields.io/badge/GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white">

</div>

---

## **🔗 ERD (Entity Relationship Diagram)**
<img width="1292" height="1710" alt="Untitled (1)" src="https://github.com/user-attachments/assets/2e4bb761-6b74-4958-a55f-82b0b66263ba" />


---

## 🎞️ 시연 영상
---
## 📃 코딩 컨벤션

### ⚙️ 네이밍 & 작성 규칙

1. **이슈**
    - 제목 규칙 : `[타입] 작업내용`
    - 예시 : `[feat] 로그인 기능 추가`
    - 본문은 템플릿에 맞춰서 작성
2. **PR**
    - 제목 규칙 : `[타입] 작업내용`
    - 예시 : `[feat] 로그인 기능 추가`
    - 본문은 템플릿에 맞춰서 작성
3. **브랜치**
    - 생성 기준 : `develop` 브랜치에서 생성
    - 명명 규칙 : `타입/#이슈번호`
    - 예시: `feat/#1`
    - `main`과 `develop` 브랜치는 브랜치 보호 규칙이 적용되어, 반드시 PR을 통해 최소 2명의 팀원 리뷰 승인 후에만 머지할 수 있다.
4. **Commit Message 규칙**
   - 명명 규칙 : `feat(auth): JWT 기반 인증 구현`
