# Regras de negócio iniciais

1. Email é obrigatório, válido, limitado a 320 caracteres, aparado e normalizado com locale independente do host.
2. Senha precisa ter 12–72 caracteres; aplica-se BCrypt (work factor 12) antes de persistir.
3. Cadastro público sempre cria `USER`; papel informado pelo cliente não é aceito.
4. Um email corresponde a uma única conta, garantido por constraint no banco.
5. Login falho não revela se email existe.
6. JWT de acesso expira em prazo configurável (900 s por padrão); assinatura é HMAC-SHA256 com segredo Base64 de ao menos 256 bits.
7. Refresh token possui entropia aleatória de 256 bits, expira (604800 s padrão), é guardado somente como hash e só pode ser rotacionado uma vez.
8. Logout revoga refresh token pertencente ao sujeito autenticado; JWT emitido continua válido até expirar.
9. Usuário lê o próprio perfil; ADMIN pode ler perfil de qualquer usuário. Papéis ANALYST/ADMIN são necessários em áreas protegidas.
10. PostgreSQL é registro durável. Redis é reservado a cache/rate-limit/idempotência temporária e não substitui o banco.
11. Timestamps persistidos são `TIMESTAMPTZ`; respostas de erro não incluem senha, tokens ou stack trace.
12. O sistema é educacional: nenhuma transação ou decisão de fraude pode autorizar movimentação de dinheiro real.
