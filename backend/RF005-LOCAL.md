# RF005 – Consulta de prestadores local

1. Configure as credenciais de um PostgreSQL local com o esquema MUV:

   ```bash
   export DB_URL='jdbc:postgresql://localhost:5432/muv'
   export DB_USERNAME='muv'
   export DB_PASSWORD='<senha do banco local>'
   ```

   O back-end aplica as migrações Flyway ao iniciar. Os resultados da busca vêm
   dos registros existentes; a consulta não cria nem utiliza dados simulados.

2. Em um terminal, inicie o back-end:

   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```

3. Em outro terminal, inicie o front-end:

   ```bash
   cd frontend
   npm run dev
   ```

   Abra a URL exibida pelo Vite e acesse `/cliente/prestadores`. O proxy local
   encaminha `/api` ao Spring Boot, incluindo os endpoints de autenticação e
   consulta.

4. Pesquise por modalidade e, opcionalmente, por cidade/UF e tipo de prestador.
   A tela informa carregamento, erros e ausência de resultados. Para retornar
   prestadores, o banco precisa conter autorização aprovada e veículo ativo
   compatível com a modalidade.

5. A API também pode ser consultada diretamente:

   ```bash
   curl 'http://localhost:8080/api/prestadores/consulta?modalidade=PESSOA&cidade=Fortaleza&estado=CE&tipo=EMPRESA'
   ```
