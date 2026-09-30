# ADR-005: JWT de acesso e refresh tokens opacos rotativos

- Status: Aceito
- Data: 2026-09-29

## Contexto

A API do portfólio requer cadastro/login e autenticação stateless, mas precisa poder revogar sessões duradouras e separar autorização por função e ownership.

## Decisão

Usar access JWT curto (HS256; duração configurável, padrão 15 minutos) e refresh token aleatório opaco com rotação de uso único. Persistir SHA-256 do refresh token em PostgreSQL. Passwords são hasheadas com BCrypt. Papéis iniciais são USER, ANALYST e ADMIN; cadastro cria somente USER. Spring Security protege por endpoint e method security verifica ownership.

## Consequências

- API valida JWT sem sessão de servidor; erro genérico evita enumeração de contas.
- Logout revoga refresh token, mas não invalida imediatamente access JWT; janela máxima é a expiração curta.
- Segredo HMAC centralizado em variável de ambiente e mínimo 256 bits; rotação de chave ainda é operação futura.
- API de amostra retorna refresh token no JSON; para implantação pública, mover para cookie HttpOnly/Secure/SameSite e definir CSRF/CORS, rate limiting e rotação de chaves.
- Elevação de papéis não está disponível publicamente.

## Alternativas

- Refresh JWT sem estado foi rejeitado por dificultar revogação.
- Sessão server-side foi rejeitada para demonstrar autorização Bearer e simplicidade do serviço stateless.
