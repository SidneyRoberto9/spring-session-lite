# Migração — 1.0.x → 2.0.0

A versão **2.0.0** contém mudanças **breaking** em relação a `1.0.0`/`1.0.1` (publicadas no
Maven Central). Este guia cobre o que muda e como migrar com segurança.

> Resumo: rename de classes/tabela/cookie, `SpringSessionLiteUser` virou `record`, e vários
> recursos novos (logout, roles, store, CORS/CSRF, sliding, eventos). A maioria dos recursos é
> aditiva; as quebras estão em **nomes** (classes, tabela, cookie) e no **principal**.

---

## 1. Banco de dados — rename da tabela e índices (obrigatório)

A tabela passou de `spring_lite_sessions` para `spring_session_lite_sessions`, os índices foram
renomeados e há uma nova coluna `roles`.

### MySQL / MariaDB
```sql
RENAME TABLE spring_lite_sessions TO spring_session_lite_sessions;

ALTER TABLE spring_session_lite_sessions ADD COLUMN roles VARCHAR(255) NULL;

ALTER TABLE spring_session_lite_sessions
    RENAME INDEX idx_spring_lite_sessions_session_id TO idx_spring_session_lite_sessions_session_id,
    RENAME INDEX idx_spring_lite_sessions_expires_at TO idx_spring_session_lite_sessions_expires_at;

-- índice novo em user_id (logoutAll / revogação por usuário)
CREATE INDEX idx_spring_session_lite_sessions_user_id ON spring_session_lite_sessions (user_id);
```

### PostgreSQL
```sql
ALTER TABLE spring_lite_sessions RENAME TO spring_session_lite_sessions;
ALTER TABLE spring_session_lite_sessions ADD COLUMN roles VARCHAR(255);

ALTER INDEX idx_spring_lite_sessions_session_id RENAME TO idx_spring_session_lite_sessions_session_id;
ALTER INDEX idx_spring_lite_sessions_expires_at RENAME TO idx_spring_session_lite_sessions_expires_at;

CREATE INDEX idx_spring_session_lite_sessions_user_id ON spring_session_lite_sessions (user_id);
```

> Com `ddl-auto=update`, o Hibernate cria a tabela/coluna nova, mas **não** migra dados da
> tabela antiga nem remove a velha. Para preservar sessões ativas, rode o `RENAME` acima.
> O DDL completo do zero está em
> [`src/main/resources/db/spring-session-lite-schema.sql`](src/main/resources/db/spring-session-lite-schema.sql).

---

## 2. Cookie — `M4SID` → `SLSID`

O nome padrão do cookie mudou. Sessões com o cookie antigo deixam de ser reconhecidas e os
usuários precisam refazer login.

- **Para evitar logout em massa**, mantenha o nome antigo via configuração:
  ```properties
  spring-session-lite.cookie-name=M4SID
  ```
- Caso aceite o logout único, nada a fazer — os usuários reautenticam.

---

## 3. Rename de classes/anotações (atualize imports)

| 1.0.x | 2.0.0 |
|-------|-------|
| `SpringLiteSession` | `SpringSessionLiteSession` |
| `SpringLiteSessionRepository` | `SpringSessionLiteSessionRepository` |

Os demais nomes (`SpringSessionLiteService`, `SpringSessionLiteUser`,
`@SpringSessionLiteCurrentSession`, `SpringSessionLiteUserService`,
`SpringSessionLiteAuthenticationFilter`) **não mudaram** em relação a `1.0.1`. Se você importava
a entidade/repositório diretamente, ajuste os imports.

---

## 4. `SpringSessionLiteUser` virou `record`

Os acessores mudaram de getters Lombok para componentes de record:

| 1.0.x | 2.0.0 |
|-------|-------|
| `user.getUserId()` | `user.userId()` |
| `user.getEmail()` | `user.email()` |
| `user.getSessionId()` | `user.sessionId()` |
| — | `user.roles()` (novo) |

A serialização JSON (chaves `userId`, `email`, `sessionId`, `roles`) é equivalente — clientes
HTTP não são afetados; apenas código Java que chamava os getters.

---

## 5. Recursos novos (aditivos, sem quebra)

- **Logout/revogação:** `sessionService.logout(req, res)`, `logout(sessionId)`, `logoutAll(userId)`.
- **Roles:** `login(userId, email, roles, req, res)` → authorities `ROLE_*`. A sobrecarga sem
  roles continua válida.
- **Store plugável:** interface `SpringSessionLiteSessionStore` (default JPA); forneça seu bean
  para trocar o backend.
- **Eventos:** `SpringSessionLiteSessionCreatedEvent` / `...DestroyedEvent` via `@EventListener`.
- **Segurança:** `csrf-enabled`, `cors-*`, `trusted-proxy-count` (XFF não-falsificável),
  validador de startup (avisa salt default / `SameSite=None` sem CSRF), `cookie-prefix`.
- **Performance:** `update-last-accessed` + `last-accessed-throttle` (fim do write-por-request),
  `sliding-expiration`.
- **Operacional:** `cleanup-enabled` para desligar só a limpeza.
- `session-id-length` default subiu de **16** para **21** (sessões existentes seguem válidas).

Veja todas as propriedades em
[`docs/02-configuracao-application-properties.md`](docs/02-configuracao-application-properties.md).

---

## 6. Mudança de comportamento — 401 do filtro

Em `1.0.x`, um cookie inválido/expirado fazia o filtro responder **401 e encerrar** a requisição
— bloqueando inclusive rotas `permit-all` (lock-out de re-login). Em `2.0.0` o filtro limpa o
cookie morto e segue anônimo; a autorização decide o 401. Rotas protegidas continuam retornando
401; rotas abertas (como `/login`) passam a funcionar mesmo com cookie morto.

---

## 7. Checklist de migração

- [ ] Rodar o SQL de rename de tabela/índices + coluna `roles` (seção 1).
- [ ] Decidir cookie: aceitar re-login ou fixar `cookie-name=M4SID` (seção 2).
- [ ] Atualizar imports de `SpringLiteSession*` (seção 3).
- [ ] Trocar getters de `SpringSessionLiteUser` por acessores de record (seção 4).
- [ ] Revisar `ip-hash-salt` (aviso de startup se default).
- [ ] `./mvnw clean test` / subir a app e validar login + endpoint protegido.
