# API de autenticação (v1)

Base URL: `/api/v1`. JSON UTF-8. Senha tem 12–72 caracteres; o valor persistido é BCrypt, nunca texto aberto.

## Cadastro

`POST /auth/register` (público), `201 Created`.

```json
{"email":"ana@example.com","password":"correct-horse-battery"}
```

Retorna `{ "id": "UUID", "email": "ana@example.com", "role": "USER", "createdAt": "UTC timestamp" }`. Email é aparado e normalizado para minúsculas. Email repetido: `409`.

## Login

`POST /auth/login` (público), `200 OK`.

```json
{"email":"ana@example.com","password":"correct-horse-battery"}
```

Retorna `accessToken` JWT Bearer e `refreshToken` opaco. O JWT inclui `sub` (UUID do usuário), `email`, `roles`, `iat` e `exp`; assinado em HS256. Access token padrão: 900 segundos. Credenciais inválidas: `401` com mensagem genérica.

## Renovação (rotação)

`POST /auth/refresh` (público), corpo `{ "refreshToken": "..." }`. O token em texto aberto não é persistido: somente SHA-256 é armazenado. Token válido é revogado e substituído por um novo par. Reuso, expiração ou token inválido: `401`.

## Logout

`POST /auth/logout` requer `Authorization: Bearer <accessToken>` e corpo `{ "refreshToken": "..." }`. A revogação é limitada ao proprietário do token. JWT de acesso existente permanece válido até expirar; tokens de acesso são curtos (15 min padrão).

## Autorização e erros

- `GET /users/{userId}/profile`: dono ou ADMIN; outro usuário recebe `403`.
- `/analyst/**`: ANALYST ou ADMIN.
- `/admin/**`: somente ADMIN.
- Requisição sem autenticação/token inválido: `401`.
- Usuário autenticado sem permissão: `403`.
- Formato de erro JSON estável: `timestamp`, `status`, `error`, `message` (e `path` para erros da API).

Papel elevado nunca pode ser escolhido no cadastro. Operação administrativa para provisionar ANALYST/ADMIN ainda não é implementada.
