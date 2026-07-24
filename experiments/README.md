# Lightsail 원격 매칭 실험 환경

로컬은 실험 제어·부하 생성·결과 저장을 맡고, Lightsail은 실험용 서버 컨테이너를 실행한다.
실험 서버는 Supabase의 별도 실험 프로젝트 DB에만 연결한다.

```text
로컬 Mac (run script / workload / results)
  → Lightsail :18080 (server-experiment)
  → Supabase experiment project (PostgreSQL)
```

## 최초 설정

1. Supabase에 `letsports-experiment` 프로젝트를 만들고 PostgreSQL 연결 정보를 준비한다.
2. Lightsail의 `/home/ubuntu/.env`를 `/home/ubuntu/.env.experiment`로 복사한다.
3. `.env.experiment`의 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`를 실험 Supabase 프로젝트 값으로 바꾼다.
4. 로컬에서 `.env.lightsail.example`을 `.env.lightsail`로 복사하고, `EXPERIMENT_API_BASE_URL`에 Lightsail 도메인(예: `https://api-prod.jikmyeodeum.com`)을 넣는다.
5. `./server/experiments/setup-lightsail.sh`를 실행해 Compose·실험 profile 설정을 원격에 올린다.
6. Lightsail 보안 그룹에서 TCP 8080을 로컬 부하 생성기가 접근할 수 있게 연다.

## 원격 컨테이너

```bash
ssh jikmyeodeum
cd /home/ubuntu
docker compose --env-file .env.experiment -f docker-compose.experiment.yml up -d
docker logs -f jikmyeodeum-server-experiment
```

기존 개발 컨테이너(`jikmyeodeum-server`, 8080) 및 `/home/ubuntu/.env`는 수정하지 않는다.
실험 컨테이너는 8080과 `/home/ubuntu/.env.experiment`만 사용한다. 따라서 기동 전에 기존 개발
컨테이너를 내려 포트를 비워야 한다.

## 현재 범위

이 설정은 원격 실험 서버의 분리 기동까지만 제공한다. S1 데이터 생성, 일반 서비스 워크로드,
정합성 검사, 결과 수집을 단일 명령으로 연결하는 실험 러너는 다음 단계에서 추가한다.
