package studdybuddy.admin;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.sql.*;
import java.util.*;

/** Checks even CASCADE foreign keys, including legacy tables not mapped as JPA entities. */
@Component
public class DeletionGuard {
    private final JdbcTemplate jdbc;
    public DeletionGuard(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public void requireUnreferenced(String table, Long id) {
        Boolean referenced=jdbc.execute((ConnectionCallback<Boolean>) connection -> {
            var metadata=connection.getMetaData();
            String actual=metadata.storesUpperCaseIdentifiers() ? table.toUpperCase(Locale.ROOT) : table;
            var references=new ArrayList<String[]>();
            try (var keys=metadata.getExportedKeys(connection.getCatalog(), connection.getSchema(), actual)) {
                while(keys.next()) references.add(new String[]{keys.getString("FKTABLE_NAME"), keys.getString("FKCOLUMN_NAME")});
            }
            String quote=metadata.getIdentifierQuoteString().trim();
            for(var reference:references) {
                String sql="SELECT 1 FROM " + quote + reference[0].replace(quote, quote+quote) + quote
                    + " WHERE " + quote + reference[1].replace(quote, quote+quote) + quote + " = ?";
                try(var statement=connection.prepareStatement(sql)) {
                    statement.setLong(1,id); statement.setMaxRows(1);
                    try(var rows=statement.executeQuery()) { if(rows.next()) return true; }
                }
            }
            return false;
        });
        if(Boolean.TRUE.equals(referenced)) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Cannot delete: linked records exist. Keep this record or disable the account instead.");
    }
}
