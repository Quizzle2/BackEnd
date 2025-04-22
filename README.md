# 🧠 Quizzle - AI 기반 실시간 온라인 퀴즈 플랫폼

> **AI 자동 퀴즈 생성 + 실시간 멀티플레이**가 결합된 인터랙티브 학습 게임 플랫폼

---

## 🚀 배포 링크
- 👉 [Quizzle](https://secret-fe.vercel.app)

---

## 🏁 프로젝트 개요

- **Quizzle**은 GPT API를 활용한 **자동 퀴즈 생성**과  
  WebSocket + Redis Pub/Sub을 통한 **실시간 멀티플레이** 게임 환경을 제공합니다.
- 사용자 간 **정답 경쟁**, **포인트 보상**, **아바타 커스터마이징**을 통해  
  학습과 재미를 동시에 잡는 플랫폼입니다.

---

## 🔧 기술 스택

| 분류 | 기술 |
|------|------|
| Frontend | Next.js, Vercel |
| Backend | Spring Boot 3, Java 21, Gradle |
| AI 연동 | OpenAI GPT API |
| Auth | OAuth2, JWT, Kakao/Google Login |
| 실시간 통신 | WebSocket (STOMP), Redis Pub/Sub |
| 인프라 | Docker, AWS EC2, S3, GitHub Actions |
| DB | MySQL (prod), H2 (dev/test) |

---

## 🔑 주요 기능

- ✅ **소셜 로그인** (Google, Kakao)
- ✅ **GPT 기반 퀴즈 생성** (카테고리, 난이도 선택)
- ✅ **실시간 게임 진행** (WebSocket + Redis)
- ✅ **포인트 시스템 및 아바타 구매**
- ✅ **친구 추가**
- ✅ **게임 결과 리포트**
- ✅ **경험치/레벨 시스템**

---

## 📌 ERD
![ERD](https://github.com/user-attachments/assets/a6c4d070-3617-4332-8912-5c680f767c7a)

## 📌 시스템 아키텍처
![아키텍처 다이어그램](https://github.com/user-attachments/assets/c81cb60c-c027-4db7-8834-5ce44fffc331)

---

## 💾 문서
- 📽️ [발표 영상](https://youtu.be/VlVQMjY-zoY)
- ▶️ [시연 영상](https://youtu.be/PAbJh_QYOyo)
- 📕 [발표 PPT](https://github.com/user-attachments/files/19770113/_10.AI.Quizzle.pdf)
- 📘 [팀 Notion](https://www.notion.so/Team10-1bb4873f28dd8015a0f8e7a26451ddfe?pvs=4)

---

## 👥 팀 소개

| 이름 | 역할 | 담당 업무 | GitHub |
|------|------|-----------|--------|
| 권기용 | PO | 프로젝트 리딩, 브랜치 관리(GitHub Flow), PR 관리, 포인트 및 아바타 기능 | [kwonkiyong0059](https://github.com/kwonkiyong0059) |
| 이상억 | 팀장 | GPT API 기반 퀴즈 생성 등 게임 관련 핵심 기능 | [jurio5](https://github.com/jurio5) |
| 노옥선 | 팀원 | 관리자 기능 구현, 발표용 PPT 제작 | [Okman-0920](https://github.com/Okman-0920) |
| 박영준 | 팀원 | 웹소켓 통신, 인프라(AWS, 무중단 CI/CD), 프론트(UI/UX) 작업 | [jurio5](https://github.com/jurio5) |
| 백성현 | 팀원 | 소셜 로그인 및 인증/인가, 사용자 기능 | [bsh52](https://github.com/bsh52) |

---

## 🥇FrontEnd Repository
- https://github.com/jurio5/secret_fe

---

## 📝 회고 및 느낀 점

- 실시간 통신, OAuth2, GPT API, Docker, CI/CD 등 **실무 중심의 기술 스택을 통합 경험**
- 분산 락과 트랜잭션 관리, 성능 안정화 등 **대규모 트래픽을 고려한 설계 적용**
- PR 기반 협업 및 코드 리뷰를 통해 **팀워크와 협업 역량 향상**
