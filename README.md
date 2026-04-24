# MedScope

MVP full stack para busca de evidencias medicas com base em CID, contexto clinico opcional, PubMed e analise local via Ollama.

O sistema:

- recebe um CID e um contexto opcional
- mapeia o CID para uma condicao clinica e uma query em ingles
- consulta artigos recentes no PubMed
- envia titulo e abstract para o Ollama
- devolve um resumo em portugues, classificacao de relevancia e notas de cautela
- nao recomenda tratamento e nao substitui julgamento clinico

## Stack

- Backend: Java 21, Spring Boot, Spring Web, Spring Data JPA, Validation, H2
- Frontend: Vue 3, Vite, Axios
- Banco: H2
- IA local: Ollama via HTTP

## Estrutura

```text
.
|-- backend
|-- frontend
|-- requests.http
`-- README.md
```

## Seed inicial de CIDs

- `F41.1` -> Generalized anxiety disorder
- `M54.5` -> Low back pain
- `E11` -> Type 2 diabetes mellitus
- `F32.9` -> Depressive episode
- `G43.9` -> Migraine

## Requisitos locais

- Java 21
- Maven 3.9+
- Node.js 20+
- Ollama instalado localmente

## 1. Banco H2 para teste

O projeto agora usa H2 em arquivo local para facilitar testes sem PostgreSQL.

Configuracao atual:

```properties
spring.datasource.url=jdbc:h2:file:./data/cliniradar;AUTO_SERVER=TRUE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

O banco sera criado automaticamente ao subir o backend.

Console H2:

```text
http://localhost:8080/h2-console
```

JDBC URL no console:

```text
jdbc:h2:file:./data/cliniradar
```

## 2. Subir Ollama

Instale o Ollama e baixe o modelo configurado:

```bash
ollama pull mistral
ollama run mistral
```

Configuracao padrao do backend:

```properties
ollama.base-url=http://localhost:11434
ollama.model=mistral
```

Se quiser trocar o modelo, ajuste `application.properties`.

## 3. Rodar o backend

Arquivo principal de configuracao:

[application.properties](backend/src/main/resources/application.properties)

Entre na pasta do backend e rode:

```bash
cd backend
mvn spring-boot:run
```

API disponivel em:

```text
http://localhost:8080
```

Console H2:

```text
http://localhost:8080/h2-console
```

### Endpoint principal

`POST /api/search`

Exemplo:

```json
{
  "cid": "F41.1",
  "context": "resistant adults psychotherapy"
}
```

Exemplo de resposta:

```json
{
  "cid": "F41.1",
  "condition": "Generalized anxiety disorder",
  "queryUsed": "generalized anxiety disorder treatment resistant adults psychotherapy",
  "disclaimer": "Conteudo informativo para avaliacao profissional. Nao substitui julgamento clinico.",
  "articles": [
    {
      "pubmedId": "12345678",
      "title": "Example article",
      "publishedAt": "2026-02-11",
      "publicationType": "Randomized Controlled Trial",
      "journal": "Example Journal",
      "url": "https://pubmed.ncbi.nlm.nih.gov/12345678/",
      "summaryPt": "Resumo informativo em portugues.",
      "relevanceLevel": "ALTO",
      "evidenceType": "Ensaio clinico",
      "practicalImpact": "Impacto pratico descrito de forma cautelosa.",
      "warningNote": "Uso apenas informacional para avaliacao profissional."
    }
  ]
}
```

## 4. Rodar o frontend

Opcionalmente copie `frontend/.env.example` para `.env` e altere a URL da API se precisar.

Entre na pasta do frontend e rode:

```bash
cd frontend
npm install
npm run dev
```

App disponivel em:

```text
http://localhost:5173
```

## Observacoes de negocio

- O sistema nao recomenda tratamento
- O sistema nao afirma conduta medica
- Os resumos devem ser lidos como apoio informacional
- Quando a evidencia for fraca ou inconclusiva, isso deve aparecer no resumo e na nota de cautela
- Toda resposta inclui disclaimer para avaliacao profissional

## Como o backend funciona

### Fluxo principal

1. O controller recebe `cid` e `context`
2. O `CidMappingService` resolve a condicao e a query base em ingles
3. O `PubMedClient` busca IDs recentes e depois carrega os detalhes dos artigos
4. O backend trata artigos sem abstract com mensagem explicita de limitacao
5. O `PromptBuilderService` monta um prompt estruturado e cauteloso
6. O `OllamaClient` envia o prompt e espera JSON
7. O `SearchService` persiste request, artigos e summaries e devolve a resposta pronta para a UI

### Organizacao do backend

- `controller`: endpoint REST
- `service`: regras principais da aplicacao
- `repository`: acesso JPA
- `dto`: payloads de entrada e saida
- `entity`: modelos persistidos
- `client`: integracoes externas com PubMed e Ollama
- `exception`: tratamento global de erros

## Pontos de atencao

- O PubMed pode retornar artigos sem abstract; nesses casos o backend informa a limitacao explicitamente
- O Ollama precisa estar respondendo em `http://localhost:11434`
- O modelo escolhido deve conseguir seguir instrucao e retornar JSON valido
- `ddl-auto=update` foi usado para facilitar o MVP

## Melhorias futuras

- cache de resultados por query
- suporte a mais CIDs e importacao de tabela maior
- autenticacao e historico por usuario
- filtros por ano, tipo de estudo e especialidade
- testes automatizados de integracao com mocks de PubMed e Ollama
