# RF001 – Cadastro e autenticação

Use um PostgreSQL descartável para o RF001. O perfil `rf001-local` substitui o perfil
de desenvolvimento padrão e usa SMTP local. Não execute os testes com a base da equipe.


1. Suba um PostgreSQL local com banco `muv` e usuário `muv`, e um capturador SMTP
   (por exemplo, Mailpit) com SMTP na porta 1025 e interface na porta 8025.
2. No terminal do back-end, configure as variáveis abaixo. Gere a chave uma vez por banco
   e preserve-a fora do Git. Trocar a chave sem migração invalida o índice de CPF.

   ```bash
   export SPRING_PROFILES_ACTIVE=rf001-local
   export MUV_LOCAL_DB_PASSWORD='<senha do banco local>'
   export MUV_AUTH_PASSENGER_KEY=$(openssl rand -base64 64 | tr -d '\n')
   cd backend
   bash mvnw spring-boot:run
   ```

   Para outro endereço de banco, use `MUV_LOCAL_DB_URL` e `MUV_LOCAL_DB_USER`.
   Para outro capturador, configure `MUV_LOCAL_SMTP_HOST` e `MUV_LOCAL_SMTP_PORT`.

3. Em outro terminal, execute `cd frontend && npm ci && npm run dev`. Abra a URL
   exibida pelo Vite na porta 5173. O proxy encaminha `/api/auth` ao Spring na porta 8080.
4. Crie uma conta com dados sintéticos, entre, verifique a sessão em `/conta` e saia.
   Em **Esqueceu a senha?**, solicite o link. Abra o capturador SMTP, use o link e confirme
   que a nova senha funciona e que o link só pode ser utilizado uma vez.
5. Para testar, execute `bash mvnw test` no back-end com o mesmo perfil e um banco
   descartável, e `npm run build && npm run lint` no front-end.

## Integração

- `usuario` continua sendo a conta compartilhada. O RF001 só adiciona um registro em
  `passageiro`, sem criar um prestador ou atribuir papel de administração.
- O login usa e-mail, conforme a migração original. CPF, CNPJ, celular como identificador
  e “Manter conectado” estão fora do fluxo. Os termos e a política mostrados no protótipo
  precisam de textos oficiais antes de serem apresentados como links de aceite.
- BCrypt protege as senhas. CPF, nascimento e gênero de novos cadastros ficam cifrados.
  Um índice HMAC permite bloquear CPF duplicado sem gravá-lo em claro. O CPF legado,
  quando presente, continua compatível com a busca de duplicatas. Nome, e-mail e telefone
  permanecem no formato do esquema compartilhado. Proteja o armazenamento e backups.
- O cookie de sessão é HttpOnly e SameSite=Lax. Mutações de autenticação passam por CSRF.
  O módulo de segurança está limitado a `/api/auth/**`. Os fluxos de prestadores e
  motoristas mantêm o comportamento atual e exigem suas próprias regras de autorização.
- Para um ambiente publicado, sirva a SPA e `/api/auth` na mesma origem HTTPS,
  configure `MUV_AUTH_SECURE_COOKIE=true` e `MUV_AUTH_PUBLIC_ORIGIN` com a origem real
  da SPA. Configure `SPRING_MAIL_HOST`, porta, credenciais/TLS e
  `MUV_AUTH_MAIL_FROM` no ambiente. Use limite de requisições de login, cadastro e
  recuperação no proxy. O protótipo local não envia e-mails para a internet.
- A recuperação retorna a mesma resposta para e-mails conhecidos e desconhecidos.
  O banco armazena apenas o hash do token. A emissão invalida links anteriores e
  o consumo é transacional, de uso único e expira em 30 minutos.
