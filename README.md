# API Inteligente de Orçamento com Spring Boot e Spring AI

Projeto do **Desafio de Projeto de Spring AI** da [DIO](https://www.dio.me/), feito a partir do projeto base do expert ([dio-spring-boot-learning-track / 05-spring-ai](https://github.com/digitalinnovationone/dio-spring-boot-learning-track/tree/main/05-spring-ai)).

## O que o projeto faz

É uma API de controle de gastos que entende **comandos de voz**. A pessoa envia um áudio como *"gastei 50 reais no mercado"* e a API:

1. transforma o áudio em texto (transcrição com o modelo `whisper-1`);
2. envia o texto para um modelo de linguagem (`gpt-4o-mini`) através do `ChatClient`;
3. a IA entende a intenção e **chama uma função real da aplicação** (Tool Calling): registrar um gasto, listar gastos ou calcular o total de uma categoria;
4. o resultado é salvo ou consultado no banco MySQL;
5. a resposta final da IA é convertida em **áudio MP3** (`gpt-4o-mini-tts`) e devolvida para quem enviou o comando.

Também existem endpoints REST comuns (sem IA) para criar e consultar transações.

## Melhoria implementada

Escolhi uma evolução pequena, mas completa e testada, em três partes:

### 1. Correção da conversão de centavos para reais
O projeto guarda os valores em **centavos** (`5000` = R$ 50,00), mas a resposta exibia o número sem converter: um gasto de R$ 50,00 aparecia como `5000.0`. Agora a conversão é feita em `TransactionOutput.centsToReais()` com `BigDecimal.valueOf(centavos, 2)`, que desloca a vírgula sem erro de arredondamento.

### 2. Validações antes de salvar uma transação
A regra fica no **domínio** (`Transaction`), então vale tanto para o endpoint REST quanto para as ferramentas chamadas pela IA:
- descrição não pode ser vazia;
- valor tem que ser maior que zero;
- categoria é obrigatória.

Se algo estiver errado, é lançada a `InvalidTransactionException`:
- no **REST**, o `ApiExceptionHandler` devolve **HTTP 400** com uma mensagem clara (formato `ProblemDetail`), em vez de um erro 500 genérico;
- na **IA**, o Spring AI devolve a mensagem de erro para o modelo (comportamento padrão, `spring.ai.tools.throw-exception-on-error=false`), e o prompt de sistema orienta a IA a explicar o problema e pedir o dado que falta.

### 3. Nova ferramenta: total gasto por categoria
Novo caso de uso `SummarizeTransactionsByCategoryUseCase`, que soma os gastos de uma categoria e conta quantas transações existem. Ele é usado de duas formas, sem duplicar regra:
- como **ferramenta da IA** (`@Tool summarize-transactions-by-category`): agora dá para perguntar por voz *"quanto eu gastei no mercado?"*;
- como **endpoint REST**: `GET /transactions/{categoria}/summary`.

O prompt de sistema (`prompts/system-message.st`) também foi melhorado: explica à IA que os valores vão em centavos, quando usar a nova ferramenta e que a resposta deve ser curta e em português, porque vira áudio.

### Testes
Foram criados **17 testes automatizados** que rodam **sem chave da OpenAI e sem banco de dados**, usando um repositório falso em memória (`InMemoryTransactionRepository`) e mocks do Mockito:

| Classe de teste | O que verifica |
|---|---|
| `TransactionTest` | criação válida e as três validações |
| `PersistTransactionUseCaseTest` | salva e devolve o valor em reais; não salva valor zero |
| `SummarizeTransactionsByCategoryUseCaseTest` | soma só a categoria pedida; categoria vazia dá zero |
| `TransactionOutputTest` | conversão de centavos para reais |
| `TransactionControllerTest` | endpoints REST: criação, erro 400 e resumo |

Os testes do projeto original que chamam a OpenAI continuam lá e só rodam quando a variável `OPENAI_API_KEY` está definida.

## Tecnologias usadas

- Java 25
- Spring Boot 4.0.5 (Web, Data JPA, Docker Compose)
- Spring AI 2.0.0-M4 com OpenAI (`ChatClient`, Tool Calling, `TranscriptionModel`, `TextToSpeechModel`)
- MySQL 9.6 (via Docker Compose)
- Lombok
- JUnit 5, AssertJ, Mockito e MockMvc
- Gradle

## Arquitetura

O projeto segue as camadas do DDD usadas na trilha:

- `domain` – modelo (`Transaction`, `Category`), regras de validação e contrato do repositório;
- `application` – casos de uso, usados **tanto pelo REST quanto pela IA** (é aqui que ficam os `@Tool`);
- `infrastructure` – controller HTTP, tratamento de erros e persistência JPA.

A IA nunca acessa o banco direto: ela só pode agir através dos casos de uso, que aplicam as regras do domínio.

## Como executar

Pré-requisitos: **Java 25**, **Docker** (para o MySQL) e uma **chave de API da OpenAI** com créditos.

1. Defina a chave da OpenAI:

   Linux/macOS:
   ```bash
   export OPENAI_API_KEY="sua_chave_aqui"
   ```
   Windows (PowerShell):
   ```powershell
   $env:OPENAI_API_KEY="sua_chave_aqui"
   ```

2. Suba a aplicação (o Spring Boot inicia o MySQL do `compose.yml` automaticamente):
   ```bash
   ./gradlew bootRun
   ```
   No Windows: `gradlew.bat bootRun`

A API fica disponível em `http://localhost:8080`.

## Como testar

### Testes automatizados (não precisam de chave nem de banco)
```bash
./gradlew test
```

### Fluxo principal: comando de voz
Envie um áudio (há exemplos em `src/test/resources/audio`) e salve a resposta em MP3:
```bash
curl -X POST http://localhost:8080/transactions/ai \
  -F "file=@src/test/resources/audio/recording-1.m4a" \
  --output resposta.mp3
```
Depois, grave um áudio perguntando *"quanto eu gastei no mercado?"* e envie da mesma forma: a IA vai usar a nova ferramenta de resumo.

### Endpoints REST
Criar um gasto (valor em **centavos**):
```bash
curl -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{"description": "Mercado", "category": "GROCERIES", "amount": 5000}'
```
Resposta: `{"id":"...","category":"GROCERIES","description":"Mercado","amount":50.0}`

Listar gastos de uma categoria:
```bash
curl http://localhost:8080/transactions/GROCERIES
```

Total por categoria (**novo**):
```bash
curl http://localhost:8080/transactions/GROCERIES/summary
```
Resposta: `{"category":"GROCERIES","transactionCount":1,"total":50.0}`

Validação (**novo**) – valor zero é recusado com HTTP 400:
```bash
curl -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{"description": "Mercado", "category": "GROCERIES", "amount": 0}'
```
Resposta: `{"title":"Transação inválida","status":400,"detail":"O valor da transação deve ser maior que zero.", ...}`

Categorias disponíveis: `GROCERIES` (mercado), `PHARMA` (farmácia), `AUTO` (carro).

## O que eu aprendi

- Como o Spring AI conecta um modelo de linguagem a uma aplicação Java real pelo `ChatClient`.
- Que o Tool Calling deixa a IA **decidir qual função chamar**, mas quem executa e valida é o meu código: por isso as regras ficam no domínio e não no prompt.
- Que um mesmo caso de uso pode servir a REST e à IA, sem duplicar lógica.
- Como transformar áudio em texto e texto em áudio com o Spring AI.
- Que dá para testar a maior parte da aplicação sem gastar créditos da OpenAI, usando um repositório em memória e mocks.
- Que guardar dinheiro em centavos evita erro de arredondamento, mas exige cuidado na hora de exibir o valor.

## Créditos

Projeto base criado pelo expert **Poiani** na trilha de Spring Boot da **DIO** ([repositório original](https://github.com/digitalinnovationone/dio-spring-boot-learning-track)).
