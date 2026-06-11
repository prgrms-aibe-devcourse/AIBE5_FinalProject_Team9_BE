<h1 align="center">GrimGate</h1>

<p align="center">문 너머 공포가 당신을 기다립니다.</p>
<p align="center">공포 방탈출 테마 예약·커뮤니티 웹 플랫폼</p>


<hr>

## 목차
- [서비스 목적](#서비스-목적)
- [핵심 차별점](#핵심-차별점)
- [주요 기능](#주요-기능)
- [기술 스택](#기술-스택)
- [시스템 아키텍처](#시스템-아키텍처)
- [ERD](#erd)
- [API 명세](#api-명세)
- [팀원 역할](#팀원-역할)
- [트러블슈팅](#트러블슈팅)
- [향후 개선사항](#향후-개선사항)
- [UI Design](#ui-design)

## 서비스 목적
GrimGate는 공포 방탈출을 즐기는 사용자가 온라인에서 테마를 탐색하고, 예약·결제까지 한 번에 완료할 수 있는 통합 플랫폼입니다. 단순 예약을 넘어 후기 작성·메이트 모집 등 커뮤니티 기능을 통해 공포 방탈출 허브 역할을 목표로 합니다.
<hr>

## 타겟 사용자
- 방탈출 입문자 ~ 마니아층 (주 10~30대)
- 1인 ~ 최대 인원 그룹으로 방탈출을 즐기는 사용자
- 메이트 모집이 필요한 솔로 플레이어
- 성공률·공포지수·클리어 기록에 관심 있는 방탈출 마니아
<hr>

## 핵심 차별점
- 공포지수·성공률·최단 클리어 기록 관리
- 업적 시스템 (쫄보 / 일반인 / 강심장 / 오컬트 동호회장 / 퇴마사)
- 메이트 모집 커뮤니티 게시판
- AI 기반 테마 추천 챗봇
<hr>

##  주요 기능

| 기능 | 설명 |
|------|------|
| 테마 탐색 · 예약 | 지역/지점/날짜/시간 선택 후 결제까지 한 번에 |
| 빠른 예약 | 당일~모레 기준 잔여 시간 즉시 예약 |
| AI 테마 추천 | Gemini API 기반 조건 맞춤 챗봇 추천 |
| 후기 시스템 | 별점·공포도·태그·스포일러 자동 가림 처리 |
| 메이트 모집 | 커뮤니티 게시판에서 팀원 모집·참여 |
| 업적·칭호 시스템 | 성공률 기반 자동 칭호 부여 + 9종 업적 |
| 마이페이지 | 예약 관리·후기·활동 내역·프로필 통합 관리 |
| 미니게임 | 공포 분위기 클릭 기반 체험 콘텐츠 |
| 알림 | 예약 임박·업적 달성·커뮤니티 실시간 알림 |

<hr>

## 기술 스택
| **영역** | **기술** |
| --- | --- |
| Frontend | React + Next.js + TypeScript + TailwindCSS |
| Backend | Java 17 + Spring Boot + Gradle + Spring Security + JPA |
| Database | MySQL + Redis |
| 인증 | JWT + Google OAuth |
| 스토리지 | AWS S3 |
| AI 추천 | Gemini API |
| 결제 | Toss Payments 테스트 결제 |
| 인프라 | Docker + AWS EC2 + GitHub Actions |
| 문서화 | Swagger UI |

<hr>

## 시스템 아키텍처
<img width="1900" height="1500" alt="system-architecture-v2 drawio" src="https://github.com/user-attachments/assets/9e5064bf-19e5-494b-bbf6-4b1d4efe801b" />
<hr>

## ERD
🔗[ERD보기](https://dbdiagram.io/d/6a0349de7a923b947291bd60)

## API 명세
🔗[API 명세서 보기](https://docs.google.com/spreadsheets/d/1k34WC1Kcj4GGapumP9v9f53It2kO6vYguhrMVqFbh6A/edit?gid=248867899#gid=248867899)

<hr>

## 팀원 역할
<div align="center">

| [![이건희](https://github.com/Lee1sd.png)](https://github.com/Lee1sd) | [![노윤희](https://github.com/sdg3729.png)](https://github.com/sdg3729) | [![박수빈](https://github.com/SooBin111.png)](https://github.com/SooBin111) | [![전큰별](https://github.com/Keunbyeol931.png)](https://github.com/Keunbyeol931) | [![유상진](https://github.com/sangjin025.png)](https://github.com/sangjin025) |
|:---:|:---:|:---:|:---:|:---:|
| **이건희**<br>후기 및 전체테마, AI 추천 | **노윤희**<br>결제, 예약 | **박수빈**<br>로그인/회원가입, 마이페이지 | **전큰별**<br>메이트모집, 게임 구현 | **유상진**<br>프론트엔드 |

</div>

<hr>

## 트러블슈팅
## 향후 개선사항
- 공포 방탈출 외 일반·SF·판타지 등 다양한 장르 테마 확장
- 실시간 채팅 기반 메이트 모집 고도화
  
<hr>

## UI Design

| 메인 | 마이페이지 | 메이트 모집 |
|------|-----------|------------|
| <img width="100%" alt="메인" src="https://github.com/user-attachments/assets/36ccf87e-9264-49c5-ac45-cffb03a0311c" /> | <img width="100%" alt="마이페이지" src="https://github.com/user-attachments/assets/172ad449-9a69-4b6b-b9da-e581c46aedd9"/> | <img width="100%" alt="메이트모집" src="https://github.com/user-attachments/assets/06e282c4-72d9-435e-b8be-37de349b6d2f" />


[피그마 디자인](https://www.figma.com/design/xzQFuhB1Lqyvu3IDCC1RCu/%EB%B0%A9%ED%83%88%EC%B6%9C?node-id=2789-14&t=YV5BxZBIGPqRu0Cc-1)
