# Security release checklist

- [ ] `JWT_SECRET` gerado aleatoriamente, com pelo menos 256 bits, via secret manager.
- [ ] `POSTGRES_PASSWORD`, Redis e Kafka sem credenciais de exemplo.
- [ ] HTTPS/TLS habilitado; Kafka com ACLs e TLS.
- [ ] `CORS_ALLOWED_ORIGINS` explícito, sem `*`.
- [ ] Actuator exposto somente em rede administrativa.
- [ ] Logs revisados para não conter tokens, senhas ou payloads sensíveis.
- [ ] Dependency scan sem vulnerabilidades críticas/altas aceitas.
- [ ] Testes de RBAC, ownership, rate limiting e refresh token aprovados.
- [ ] Backup, retenção de auditoria e rotação de chaves definidos.
- [ ] Threat model atualizado e revisão por outro engenheiro registrada.
