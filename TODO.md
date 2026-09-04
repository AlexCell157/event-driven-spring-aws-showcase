# Persönliche Lern-Roadmap & zukünftige TODOs

Ziel: kontinuierliche Weiterentwicklung im Bereich Backend- & Cloud-Engineering.

Ausgangslage — bereits vorhanden: **Docker, Java, Spring, Spring Boot, MySQL, Maven, Jenkins, Nexus**.
Durch dieses Projekt zusätzlich angerissen: **AWS (S3, DynamoDB), Kafka, Terraform, Kubernetes, LocalStack**.

## Schwerpunkte

Themen, die ich mittelfristig vertiefen möchte:

1. **Cloud-Kompetenz** — AWS, Kubernetes und Infrastructure as Code praktisch beherrschen.
2. **System Design & Distributed Systems** — Architekturen entwerfen, bewerten und erklären können.
3. **Datenstrukturen & Algorithmen** — Grundlagen regelmäßig trainieren und frisch halten.
4. **Moderne Delivery-Praktiken** — GitOps, Observability, "you build it, you run it".
5. **Sichtbare Ergebnisse** — Portfolio-Projekte, Open-Source-Beiträge, dokumentierte Learnings.

---

## Priorität 1 — Direkt auf diesem Projekt aufbauen

- [ ] **AWS vertiefen**: über S3/DynamoDB hinaus SQS, SNS, Lambda, ECS/EKS, IAM (Policies, Roles, IRSA) praktisch einsetzen.
- [ ] **AWS-Zertifizierung**: *AWS Certified Solutions Architect – Associate* (international das anerkannteste Cloud-Zertifikat).
- [ ] **Kubernetes produktionsnah**: Helm-Chart für dieses Projekt schreiben, Liveness/Readiness-Probes, HPA, Ingress; danach optional *CKAD*-Zertifizierung.
- [ ] **Terraform ausbauen**: Module, Remote State (S3 + Locking), Workspaces; optional *HashiCorp Terraform Associate*.
- [ ] **Kafka vertiefen**: Schema Registry (Avro/Protobuf), Kafka Streams, Dead Letter Topics, Exactly-Once-Semantik — direkt im `OrderConsumer` ausprobierbar.
- [ ] **Observability ergänzen**: Micrometer + Prometheus + Grafana ins Projekt einbauen, verteiltes Tracing mit OpenTelemetry.

## Priorität 2 — Grundlagen & Kommunikation

- [ ] **Datenstrukturen & Algorithmen**: regelmäßig Übungsaufgaben lösen (z. B. LeetCode, Ziel: ~150 Aufgaben, Schwerpunkt Medium; Muster wie Two Pointers, Sliding Window, BFS/DFS, Heaps).
- [ ] **System Design**: "Designing Data-Intensive Applications" (Kleppmann) lesen; Übungsdesigns wie URL-Shortener, Rate Limiter, Notification System — dieses Event-Driven-Projekt ist eine gute Referenz.
- [ ] **Über eigene Arbeit sprechen**: Projekterfahrungen strukturiert aufbereiten (Situation, Aufgabe, Vorgehen, Ergebnis), um sie klar präsentieren zu können.
- [ ] **Englisch**: technisches Englisch aktiv üben — Design-Diskussionen, Code-Reviews, Mock-Gespräche.
- [ ] **Profil pflegen**: Lebenslauf ergebnisorientiert mit Metriken formulieren ("reduced X by Y%"), LinkedIn-Profil auf Englisch aktuell halten.

## Priorität 3 — Tech-Stack verbreitern

- [ ] **PostgreSQL**: neben MySQL die international am weitesten verbreitete Open-Source-Datenbank; Indexing, Query-Tuning, Transaktionen/Isolation-Levels verstehen.
- [ ] **Moderne CI/CD**: GitHub Actions (löst Jenkins vielerorts ab) und GitOps mit ArgoCD; eine Pipeline für dieses Projekt bauen (Build → Test → Docker Image → Deploy).
- [ ] **Zweite Programmiersprache**: Python (am universellsten, auch für AI) oder Go (Cloud-Native-Standard, z. B. Kubernetes-Ökosystem).
- [ ] **AI/LLM-Integration**: Spring AI ausprobieren, Grundlagen zu Embeddings, RAG und Vektor-Datenbanken — aktuell eines der gefragtesten Themen.
- [ ] **Security**: OAuth2/OIDC mit Spring Security, OWASP Top 10, Secrets-Management (AWS Secrets Manager / Vault).
- [ ] **Testing-Tiefe**: Testcontainers (echte Kafka/LocalStack-Integrationstests statt Mocks), Contract Testing (Pact), Last-Tests (Gatling/k6).

## Priorität 4 — Sichtbarkeit & Netzwerk

- [ ] **Open-Source-Beiträge**: kleine PRs an Projekten, die ich nutze (Spring, Testcontainers, LocalStack).
- [ ] **Portfolio ausbauen**: dieses Projekt mit CI/CD, Monitoring und Live-Demo (z. B. auf einem günstigen EKS/Fly.io-Setup) abrunden; englische READMEs.
- [ ] **Technisches Schreiben**: Blogposts über Learnings (z. B. "Idempotent Kafka Consumers with DynamoDB") auf dev.to oder Medium.
- [ ] **Netzwerk**: englischsprachige Communities (Reddit r/ExperiencedDevs, Spring/Kafka-Slack/Discord), Meetups, ggf. Konferenz-Talks.

---

## Vorschlag für die Reihenfolge (grob 12–18 Monate)

| Phase | Fokus |
| :--- | :--- |
| Monate 1–4 | AWS SAA-Zertifizierung + Observability/CI-CD in dieses Projekt einbauen |
| Monate 5–8 | Kubernetes/Helm vertiefen, Kafka-Themen (Schema Registry, DLT), PostgreSQL |
| Monate 9–12 | Algorithmen-Routine + System Design + englische Mock-Gespräche |
| ab Monat 12 | Portfolio, Open Source & Blog kontinuierlich weiterführen |
