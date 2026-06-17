# Plano Central de Correção — spring-session-lite

Plano-mestre de execução das correções da auditoria. Cada problema tem um plano
próprio em `level_1/`, `level_2/`, `level_3/`. Este documento define **ordem,
dependências, lotes e orquestração com subagentes**.

> Fonte da auditoria: `/home/sid/.claude/plans/realize-uma-auditoria-completa-jiggly-sphinx.md`

---

## 1. Estrutura

```
correction/
├── README.md                 ← este plano central
├── level_1/                  ← Crítico (destrava adoção / segurança grave)
│   ├── 1.1-ddl-tabela-errada.md
│   ├── 1.2-docs-classes-inexistentes.md
│   ├── 1.3-lockout-relogin.md
│   ├── 1.4-csrf-cookie-auth.md
│   └── 1.5-xff-falsificavel.md
├── level_2/                  ← Importante
│   ├── 2.1-nomenclatura.md
│   ├── 2.2-escrita-por-request.md
│   ├── 2.3-logout.md
│   ├── 2.4-roles.md
│   ├── 2.5-scheduling-toggle.md
│   ├── 2.6-revogacao-por-usuario.md
│   ├── 2.7-cobertura-testes.md
│   └── 2.8-cors.md
└── level_3/                  ← Opcional / evolução
    ├── 3.1-session-store.md
    ├── 3.2-sliding-expiration.md
    ├── 3.3-eventos.md
    ├── 3.4-user-record.md
    ├── 3.5-hardening-cookie.md
    ├── 3.6-reduzir-deps.md
    ├── 3.7-readme-publicacao.md
    └── 3.8-hmac-cache.md
```

---

## 2. Grafo de dependências (o que precisa vir antes)

```
2.1 (nomenclatura) ──► deve rodar ANTES de 1.2 (docs) e de qualquer item que
                       toque nomes públicos. Renomeia entidade/repo/classes.

1.1 (DDL) ───────────► depende do nome final da tabela (2.1). Gerar DDL por último
                       dentro do lote estrutural.

2.3 (logout) ────────► usa CookieManager.clear() (já existe). Independe.
2.6 (revogação) ─────► estende repo de 2.3. Fazer 2.3 antes.
2.2 (escrita/req) ───► toca validate(); coordenar com 3.2 (sliding) se ambos.
1.3 (lockout) ───────► toca o filtro; independente, mas altera fluxo de 401.
1.4 (csrf) / 1.5 (xff)► independentes entre si.
2.4 (roles) ─────────► toca SpringSessionLiteUser + filtro + entidade. Coordenar
                       com 3.4 (record) — fazer 3.4 junto se for mexer na classe.
2.7 (testes) ────────► POR ÚLTIMO em cada lote; valida o que foi feito.
1.2 (docs) / 3.7 ────► POR ÚLTIMO no geral; refletem o estado final.
```

**Regra de ouro de paralelismo:** itens que **editam o mesmo arquivo** NÃO podem
rodar em subagentes paralelos sem worktree isolado. Ver matriz de conflito (§4).

---

## 3. Ordem de execução recomendada (em ondas)

### Onda 0 — Coesão estrutural (sequencial, base de tudo)
1. **2.1** nomenclatura (rename global de prefixos + tabela + cookie)
2. **1.1** DDL regenerado a partir da entidade já renomeada

### Onda 1 — Lote crítico funcional (paralelizável; arquivos disjuntos)
3. **1.3** lockout no filtro  ·  **1.5** XFF no IpResolver  (arquivos diferentes → paralelo)

### Onda 2 — Lote segurança
4. **1.4** CSRF (auto-config + docs) · **2.3** logout (service+cookie) · **2.6** revogação (repo+service)
   - 2.3 → 2.6 sequencial (mesmo service/repo). 1.4 paralelo.

### Onda 3 — Performance + extensibilidade
5. **2.2** escrita por request (validate) · **2.4** roles (user+filtro+entidade) · **2.5** scheduling toggle
6. **2.8** CORS (auto-config)

### Onda 4 — Qualidade e fechamento (sequencial, por último)
7. **2.7** testes (cobre tudo acima) 
8. **1.2** docs + **3.7** README/publicação (refletem estado final)

### Nível 3 (oportunístico, sem bloquear release)
3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.8 — agendar após o 1.x estável.

---

## 4. Matriz de conflito de arquivos (para paralelizar com segurança)

| Arquivo | Itens que o tocam |
|---------|-------------------|
| `autoconfigure/SpringSessionLiteAutoConfiguration.java` | 1.4, 2.5, 2.8, 2.4(roles na cadeia?) |
| `security/SpringSessionLiteAuthenticationFilter.java` | 1.3, 2.4 |
| `service/SpringSessionLiteService.java` | 2.2, 2.3, 2.6, 3.2 |
| `service/SpringSessionLiteIpResolver.java` | 1.5 |
| `service/SpringSessionLiteCookieManager.java` | 2.3, 3.5 |
| `domain/SpringLiteSession*.java` | 2.1, 2.4, 2.6 |
| `security/SpringSessionLiteUser.java` | 2.4, 3.4 |
| `config/SpringSessionLiteProperties.java` | 1.4, 1.5, 2.2, 2.5, 2.8, 3.2, 3.5 |
| `docs/*`, `README.md` | 1.2, 3.7 |
| `resources/db/*-schema.sql` | 1.1, 2.1 |

> `SpringSessionLiteProperties.java` é hotspot: muitos itens adicionam propriedades.
> Para paralelizar, ou serializar edições nesse arquivo, ou usar **worktrees
> isolados** por subagente e fazer merge.

---

## 5. Orquestração com subagentes

**Modo recomendado:** executar onda a onda. Dentro de uma onda, despachar um
subagente por item **somente** quando os conjuntos de arquivos forem disjuntos
(ver §4); caso contrário, sequencial ou com `isolation: worktree`.

### Template de prompt para subagente (genérico)
```
Você é um subagente implementando UM item de correção da lib spring-session-lite.
Diretório: /home/sid/www/personal/sidneyroberto9-spring-session-lite

1. Leia o plano do item: correction/level_X/<arquivo>.md
2. Implemente EXATAMENTE o escopo descrito — não toque em arquivos fora da lista.
3. Siga os padrões do projeto (Lombok, construtor injection, ResponseEntity explícito).
4. Rode `./mvnw test` e garanta verde (não quebre os 9 testes existentes).
5. Retorne: arquivos alterados, resumo do diff e resultado dos testes.
NÃO faça commit. NÃO altere nomes públicos além do que o plano manda.
```

### Sugestão de fan-out por onda (pseudo-orquestração)
- **Onda 0:** 1 agente sequencial (2.1 → 1.1) — base, sem paralelismo.
- **Onda 1:** 2 agentes paralelos (1.3, 1.5) — arquivos disjuntos.
- **Onda 2:** 1.4 paralelo; (2.3→2.6) sequencial no mesmo agente.
- **Onda 3:** 2.4, 2.5, 2.8 — 2.5/2.8/1.4 tocam Properties+AutoConfig → serializar ou worktree.
- **Onda 4:** 1 agente sequencial (2.7 testes → 1.2/3.7 docs).

### Gate entre ondas
Após cada onda: `./mvnw test` verde + revisão do diff antes de liberar a próxima.
Recomenda-se 1 subagente revisor (read-only) por onda checando aderência ao plano.

---

## 6. Critérios de pronto (Definition of Done) global
- [ ] `./mvnw clean test` verde (testes atuais + novos de 2.7).
- [ ] App de teste sobe com `ddl-auto=validate` usando o DDL (valida 1.1).
- [ ] Regressão do 1.3: `POST /login` com cookie morto → 200.
- [ ] Snippets de TODAS as docs compilam (valida 1.2).
- [ ] Nenhum nome `m4`/`M4SID`/prefixo misto remanescente (valida 2.1).
- [ ] Sem código morto (`CookieManager.clear` agora referenciado por 2.3).
- [ ] Versão do pom e das docs sincronizadas.

## 7. Versionamento sugerido
- Lote Nível 1 + 2.1 → release **patch/minor** `1.1.0` (correções + rename = breaking se já publicado; se ainda não publicado, manter `1.x`).
- Lote Nível 2 → `1.2.0`.
- Nível 3 → incremental conforme adoção.
