package be.neutrinet.ispng.util;

import com.j256.ormlite.db.PostgresDatabaseType;
import com.j256.ormlite.field.FieldType;

import java.util.List;

/**
 * Created by wannes on 6/30/15.
 * <p>
 * Apparently the built-in PostgreSQL class has either not been tested or is abandoned
 * This class implements proper SEQUENCE creation and handling
 */
public class PostgreSQLType extends PostgresDatabaseType {
    @Override
    public void appendSelectNextValFromSequence(StringBuilder sb, String sequenceName) {
        sb.append("SELECT NEXTVAL(");
        sb.append('\'').append('\"').append(sequenceName).append('\"').append('\'');
        sb.append(')');
    }

    @Override
    protected void configureGeneratedIdSequence(StringBuilder sb, FieldType fieldType, List<String> statementsBefore,
                                                List<String> additionalArgs, List<String> queriesAfter) {
        String sequenceName = fieldType.getGeneratedIdSequence();
        // added existence check
        // !! this check will only detect if there is a something with the given name present,
        // even though the object in question might not be a SEQUENCE
        StringBuilder seqSb = new StringBuilder();
        seqSb.append("do $$\nBEGIN\n");
        seqSb.append("IF NOT EXISTS (SELECT 0 FROM pg_class where relname = '");
        seqSb.append(sequenceName);
        seqSb.append("' )\n" +
                "THEN\n" +
                "CREATE SEQUENCE ");
        appendEscapedEntityName(seqSb, sequenceName);
        seqSb.append(";\nEND IF;\nEND\n$$");

        statementsBefore.add(seqSb.toString());

        sb.append("DEFAULT NEXTVAL(");
        // postgres needed this special escaping for NEXTVAL('"sequence-name"')
        sb.append('\'').append('\"').append(sequenceName).append('\"').append('\'');
        sb.append(") ");
        // could also be the type serial for auto-generated sequences
        // 8.2 also have the returning insert statement

        configureId(sb, fieldType, statementsBefore, additionalArgs, queriesAfter);
    }

}
