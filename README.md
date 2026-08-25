# bank-demo
- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- IBM MQ queue manager: `QM1`, channel `DEV.APP.SVRCONN`, port `1414`
- Point-to-point application queue: `TRANSFER.COMPLETED`

## Main API flow

Create an account:

```bash
curl -i -X POST http://localhost:8080/api/v1/accounts -H "Content-Type: application/json" -d "{\"ownerName\":\"Alice\",\"currency\":\"THB\",\"initialBalance\":1000}"
```

Deposit, transfer, and inspect balances/statement:

```bash
curl -X POST http://localhost:8080/api/v1/accounts/1/deposit -H "Content-Type: application/json" -d "{\"amount\":500}"
curl -X POST http://localhost:8080/api/v1/transfers -H "Content-Type: application/json" -H "Idempotency-Key: demo-transfer-001" -d "{\"fromAccountId\":1,\"toAccountId\":2,\"amount\":100,\"currency\":\"THB\"}"
curl http://localhost:8080/api/v1/accounts/1/balance
curl "http://localhost:8080/api/v1/accounts/1/transactions?page=0&size=20"
```

Other endpoints are documented interactively in Swagger UI: account lookup, status update, withdraw, and transfer lookup.
