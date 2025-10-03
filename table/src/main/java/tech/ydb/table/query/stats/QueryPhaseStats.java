package tech.ydb.table.query.stats;

import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

public final class QueryPhaseStats {
    private final long durationUs;
    private final List<TableAccessStats> tableAccess;
    private final long cpuTimeUs;
    private final long affectedShards;
    private final boolean literalPhase;

    public QueryPhaseStats(tech.ydb.proto.YdbQueryStats.QueryPhaseStats protoAutoGenQueryPhaseStats) {
        this(
                protoAutoGenQueryPhaseStats.getDurationUs(),
                protoAutoGenQueryPhaseStats.getTableAccessList().stream().map(TableAccessStats::new).collect(toList()),
                protoAutoGenQueryPhaseStats.getCpuTimeUs(),
                protoAutoGenQueryPhaseStats.getAffectedShards(),
                protoAutoGenQueryPhaseStats.getLiteralPhase()
        );
    }

    public QueryPhaseStats(
            long durationUs,
            List<TableAccessStats> tableAccess,
            long cpuTimeUs,
            long affectedShards,
            boolean literalPhase
    ) {
        this.durationUs = durationUs;
        this.tableAccess = tableAccess;
        this.cpuTimeUs = cpuTimeUs;
        this.affectedShards = affectedShards;
        this.literalPhase = literalPhase;
    }

    public long getDurationUs() {
        return this.durationUs;
    }

    public List<TableAccessStats> getTableAccessList() {
        return this.tableAccess;
    }

    public int getTableAccessCount() {
        return this.tableAccess.size();
    }

    public TableAccessStats getTableAccess(int index) {
        return this.tableAccess.get(index);
    }

    public long getCpuTimeUs() {
        return this.cpuTimeUs;
    }

    public long getAffectedShards() {
        return this.affectedShards;
    }

    public boolean getLiteralPhase() {
        return this.literalPhase;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof QueryPhaseStats)) {
            return super.equals(obj);
        } else {
            QueryPhaseStats other = (QueryPhaseStats) obj;
            return Objects.equals(getDurationUs(), other.getDurationUs()) &&
                    Objects.equals(getTableAccessList(), other.getTableAccessList()) &&
                    Objects.equals(getCpuTimeUs(), other.getCpuTimeUs()) &&
                    Objects.equals(getAffectedShards(), other.getAffectedShards()) &&
                    Objects.equals(getLiteralPhase(), other.getLiteralPhase());
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDurationUs(), getTableAccessList(), getCpuTimeUs(), getAffectedShards(),
                getLiteralPhase());
    }

    @Override
    public String toString() {
        return "QueryPhaseStats{" + "durationUs=" + durationUs + ", tableAccess=" + tableAccess + ", cpuTimeUs=" +
                cpuTimeUs + ", affectedShards=" + affectedShards + ", literalPhase=" + literalPhase + '}';
    }
}
