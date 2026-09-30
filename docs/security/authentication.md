# Segurança: autenticação inicial

- Não guardar senha ou refresh token em texto aberto; verificar implementação/hashes no banco de teste.
- Secreto JWT precisa ser Base64 válido com no mínimo 32 bytes; `.env` é ignorado por Git.
- Utilizar exclusivamente credenciais fictícias no desenvolvimento local; não reutilizar senhas pessoais.
- Emitir tokens sem gravar valor completo em logs. Erros de login são genéricos.
- Refresh tokens têm uso único, expiração e revogação em PostgreSQL.
- Segredo de desenvolvimento do `.env.example` é placeholder e deve ser trocado.
- JWT válido não é revogado por logout; limite a janela com access token curto.
- Antes de implantação: cookies seguros, HTTPS, CORS/CSRF explícitos, rate limiting, política de lockout, key rotation, gestão centralizada de segredos, auditoria e revisão de ameaças.
