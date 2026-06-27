package com.sleekydz86.catalog.adapter.outbound.persistence.connection;


import com.sleekydz86.catalog.domain.connection.model.ConnectionHealthStatus;
import com.sleekydz86.catalog.domain.connection.model.ConnectionLifecycleStatus;
import com.sleekydz86.catalog.domain.connection.model.ConnectionProfile;
import com.sleekydz86.catalog.domain.connection.model.DatabaseVendor;

final class ConnectionPersistenceMapper {

    private ConnectionPersistenceMapper() {
    }

    static ConnectionProfile toDomain(ConnectionProfileRow row) {
        if (row == null) {
            return null;
        }
        return new ConnectionProfile(
                row.getLnkgId(),
                row.getLnkgNm(),
                DatabaseVendor.valueOf(row.getDbTypeCd()),
                row.getSrvrAddr(),
                row.getSrvrPortNo(),
                row.getDbNm(),
                row.getSchmNm(),
                row.getLnkgExpln(),
                row.getAcntId(),
                row.getEnpswd(),
                Boolean.TRUE.equals(row.getUseYn()),
                ConnectionLifecycleStatus.valueOf(row.getLnkgSttsCd()),
                ConnectionHealthStatus.valueOf(row.getTestRsltCd()),
                row.getLastTestDt(),
                row.getCreatrId(),
                row.getMdfrId()
        );
    }

    static java.util.Map<String, Object> toProcedureParams(ConnectionProfile profile, String op) {
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("op", op);
        params.put("lnkgId", profile.id());
        params.put("lnkgNm", profile.name());
        params.put("dbTypeCd", profile.vendor().name());
        params.put("srvrAddr", profile.host());
        params.put("srvrPortNo", profile.port());
        params.put("dbNm", profile.databaseName());
        params.put("schmNm", profile.schemaName());
        params.put("lnkgExpln", profile.description());
        params.put("acntId", profile.username());
        params.put("enpswd", profile.encryptedPassword());
        params.put("useYn", profile.enabled());
        params.put("lnkgSttsCd", profile.lifecycleStatus().name());
        params.put("testRsltCd", profile.healthStatus().name());
        params.put("lastTestDt", profile.lastTestAt());
        params.put("actorId", profile.modifierId());
        return params;
    }
}
