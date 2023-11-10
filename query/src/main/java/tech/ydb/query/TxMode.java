package tech.ydb.query;

import tech.ydb.proto.query.YdbQuery;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class TxMode implements QuerySession.Tx {
    private final YdbQuery.TransactionSettings txMode;
    private final boolean commitTx;

    private TxMode(YdbQuery.TransactionSettings txMode, boolean commitTx) {
        this.txMode = txMode;
        this.commitTx = commitTx;
    }

    @Override
    public YdbQuery.TransactionControl toTxControlPb() {
        return YdbQuery.TransactionControl.newBuilder()
                .setBeginTx(txMode)
                .setCommitTx(commitTx)
                .build();
    }

    public YdbQuery.TransactionSettings toTxSettingsPb() {
        return txMode;
    }

    public boolean isCommitTx() {
        return commitTx;
    }

    public TxMode setCommitTx(boolean commitTx) {
        return new TxMode(txMode, commitTx);
    }

    public TxMode withCommitTx() {
        return setCommitTx(true);
    }

    public TxMode withoutCommitTx() {
        return setCommitTx(false);
    }

    public static TxMode serializableRw() {
        return TxSerializableRw.COMMIT_ON;
    }

    public static TxMode staleRo() {
        return TxStaleRo.COMMIT_ON;
    }

    public static TxMode snapshotRo() {
        return TxSnapshotRo.COMMIT_ON;
    }

    public static TxOnlineRo onlineRo() {
        return new TxOnlineRo(true, false);
    }

    private static final class TxSerializableRw extends TxMode {
        private static final YdbQuery.TransactionSettings MODE = YdbQuery.TransactionSettings.newBuilder()
                .setSerializableReadWrite(YdbQuery.SerializableModeSettings.getDefaultInstance())
                .build();

        private static final TxSerializableRw COMMIT_ON = new TxSerializableRw(true);
        private static final TxSerializableRw COMMIT_OFF = new TxSerializableRw(false);

        private TxSerializableRw(boolean commitTx) {
            super(MODE, commitTx);
        }

        @Override
        public TxMode setCommitTx(boolean commitTx) {
            return commitTx ? COMMIT_ON : COMMIT_OFF;
        }
    }

    private static final class TxStaleRo extends TxMode {
        private static final YdbQuery.TransactionSettings MODE = YdbQuery.TransactionSettings.newBuilder()
                .setStaleReadOnly(YdbQuery.StaleModeSettings.getDefaultInstance())
                .build();

        private static final TxStaleRo COMMIT_ON = new TxStaleRo(true);
        private static final TxStaleRo COMMIT_OFF = new TxStaleRo(false);

        private TxStaleRo(boolean commitTx) {
            super(MODE, commitTx);
        }

        @Override
        public TxMode setCommitTx(boolean commitTx) {
            return commitTx ? COMMIT_ON : COMMIT_OFF;
        }
    }

    private static final class TxSnapshotRo extends TxMode {
        private static final YdbQuery.TransactionSettings MODE = YdbQuery.TransactionSettings.newBuilder()
                .setSnapshotReadOnly(YdbQuery.SnapshotModeSettings.getDefaultInstance())
                .build();

        private static final TxSnapshotRo COMMIT_ON = new TxSnapshotRo(true);
        private static final TxSnapshotRo COMMIT_OFF = new TxSnapshotRo(false);

        private TxSnapshotRo(boolean commitTx) {
            super(MODE, commitTx);
        }

        @Override
        public TxMode setCommitTx(boolean commitTx) {
            return commitTx ? COMMIT_ON : COMMIT_OFF;
        }
    }

    public static final class TxOnlineRo extends TxMode {
        TxOnlineRo(boolean commitTx, boolean allowInconsistentReads) {
            super(YdbQuery.TransactionSettings.newBuilder()
                .setOnlineReadOnly(YdbQuery.OnlineModeSettings.newBuilder()
                        .setAllowInconsistentReads(allowInconsistentReads)
                        .build())
                .build(), commitTx);
        }

        public boolean isAllowInconsistentReads() {
            return toTxSettingsPb().getOnlineReadOnly().getAllowInconsistentReads();
        }

        public TxOnlineRo setAllowInconsistentReads(boolean allow) {
            return new TxOnlineRo(isCommitTx(), allow);
        }
    }
}