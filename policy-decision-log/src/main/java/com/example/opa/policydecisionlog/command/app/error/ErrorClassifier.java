package com.example.opa.policydecisionlog.command.app.error;

import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.Set;

@Component
public class ErrorClassifier {

    private static final Set<String> RETRYABLE_SQL_STATES = Set.of(
            // 08xxx: Connection exception
            "08000", "08003", "08006", "08001", "08004", "08007", "08P01",
            // 40P01: Deadlock detected
            "40P01",
            // 55P03: Lock not available
            "55P03",
            // 57014: Query canceled (timeout)
            "57014",
            // 53300: Too many connections
            "53300",
            // 57Pxx: Admin/crash shutdown, cannot connect now
            "57P01", "57P02", "57P03"
    );

    public boolean isRetryable(Throwable ex) {
        SQLException sqlException = findSqlException(ex);
        if (sqlException == null) {
            return false;
        }
        return isRetryableSqlState(sqlException.getSQLState());
    }

    private boolean isRetryableSqlState(String sqlState) {
        if (sqlState == null) {
            return false;
        }
        if (RETRYABLE_SQL_STATES.contains(sqlState)) {
            return true;
        }
        return sqlState.startsWith("08");
    }

    private SQLException findSqlException(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof SQLException sqlException) {
                return sqlException;
            }
            current = current.getCause();
        }
        return null;
    }
}
