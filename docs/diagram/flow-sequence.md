# Decision Log Flow - Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant OPA as OPA Server
    participant Ctrl as Controller
    participant KP as Kafka Producer
    participant Main as decision-logs<br/>(Topic)
    participant MC as DecisionLogConsumer
    participant UC as PersistUseCase
    participant EH as ErrorHandler
    participant DB as PostgreSQL<br/>(DB)
    participant Parking as decision-logs-parking<br/>(Topic)
    participant DLQ as decision-logs-dlq<br/>(Topic)
    participant PC as ParkingLotConsumer
    participant PDLQ as parking-dlq<br/>(Topic)

    %% === Ingestion ===
    rect rgb(220, 240, 220)
        Note over OPA,KP: 1. Ingestion
        OPA->>+Ctrl: POST /logs (gzip)
        Ctrl->>Ctrl: decompress & parse
        Ctrl->>KP: publish(records)
        KP->>Main: kafka.send
        Ctrl-->>-OPA: 204 NoContent
    end

    %% === Happy Case ===
    rect rgb(220, 230, 250)
        Note over Main,DB: 2. Happy Case
        Main->>+MC: poll(batch)
        MC->>MC: parse → Command
        MC->>+UC: execute(commands)
        UC->>+DB: batchUpdate
        DB-->>-UC: success
        UC-->>-MC: done
        MC->>MC: ack
    end

    %% === Retryable Error ===
    rect rgb(255, 240, 220)
        Note over MC,Parking: 3. Retryable Error (DB 연결 실패 등)
        MC->>+UC: execute(commands)
        UC->>+DB: batchUpdate
        DB-->>-UC: SQLException (08xxx)
        UC->>+EH: handle(commands, error)
        EH->>EH: isRetryable? → Yes
        EH->>Parking: publish(commands)
        EH-->>-UC: done
        UC-->>-MC: done
        MC->>MC: ack
    end

    %% === Non-Retryable Error ===
    rect rgb(255, 230, 230)
        Note over MC,DLQ: 4. Non-Retryable Error (데이터 에러)
        MC->>+UC: execute(commands)
        UC->>+DB: batchUpdate
        DB-->>-UC: DataException
        UC->>+EH: handle(commands, error)
        EH->>EH: isRetryable? → No
        EH->>EH: bisect → 실패 레코드 식별
        EH->>DLQ: toDlq(failedRecord)
        EH->>DB: save(successRecords)
        EH-->>-UC: done
        UC-->>-MC: done
        MC->>MC: ack
    end

    %% === Parking Recovery ===
    rect rgb(240, 230, 250)
        Note over Parking,PDLQ: 5. Parking Lot Recovery
        Parking->>+PC: poll(record)
        PC->>PC: check not-before
        alt not-before 미도달
            PC->>PC: nack(waitMs)
        else not-before 도달
            alt attempt < maxRetry
                PC->>+UC: executeRecovery(command)
                UC->>+DB: batchUpdate
                alt 성공
                    DB-->>UC: success
                    UC-->>PC: done
                else retryable 에러
                    DB-->>UC: error
                    UC-->>PC: error
                    PC->>Parking: retry(attempt+1)
                else non-retryable 에러
                    DB-->>-UC: error
                    UC-->>-PC: error
                    PC->>DLQ: toDlq
                end
            else attempt >= maxRetry
                PC->>PDLQ: publish
            end
        end
        PC->>-PC: ack
    end
```

## 흐름 설명

### 1. Ingestion (수집)
- OPA Server가 gzip 압축된 Decision Log를 HTTP POST로 전송
- Controller가 압축 해제 후 Kafka로 발행
- 비동기 처리로 빠른 응답 (204 No Content)

### 2. Happy Case (정상 처리)
- Kafka Consumer가 배치 단위로 메시지 소비
- UseCase를 통해 DB에 배치 저장
- 성공 시 offset commit (ack)

### 3. Retryable Error (재시도 가능 에러)
- DB 연결 실패, 타임아웃, 데드락 등
- ErrorHandler가 Parking Lot 토픽으로 발행
- 지수 백오프로 재시도 예정

### 4. Non-Retryable Error (재시도 불가 에러)
- 파싱 에러, 데이터 무결성 에러 등
- Bisect 알고리즘으로 실패 레코드만 식별
- 실패 레코드는 DLQ로, 정상 레코드는 DB에 저장

### 5. Parking Lot Recovery (복구)
- not-before 시간까지 대기 후 재시도
- 최대 재시도 횟수 초과 시 Parking DLQ로 이동
- 성공 시 복구 완료, 실패 시 다음 attempt로 재발행
