package com.sleekydz86.catalog.test.support;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public final class H2StoredProcedures {

    private H2StoredProcedures() {
    }

    public static String spLnkgProfile(
            Connection conn,
            String op,
            String lnkgId,
            String lnkgNm,
            String dbTypeCd,
            String srvrAddr,
            Integer srvrPortNo,
            String dbNm,
            String schmNm,
            String lnkgExpln,
            String acntId,
            String enpswd,
            Boolean useYn,
            String lnkgSttsCd,
            String testRsltCd,
            Timestamp lastTestDt,
            String actorId
    ) throws SQLException {
        String actor = blankToDefault(actorId, "system");
        if ("C".equals(op)) {
            if (isBlank(lnkgId)) {
                lnkgId = nextId(conn, "lnkg");
            }
            String sql = """
                    insert into t_lnkg_profile (
                        lnkg_id, lnkg_nm, db_type_cd, srvr_addr, srvr_port_no, db_nm, schm_nm,
                        lnkg_expln, acnt_id, enpswd, use_yn, lnkg_stts_cd, test_rslt_cd, last_test_dt,
                        creatr_id, mdfr_id, crt_dt, mdfcn_dt
                    ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                    """;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                bindLnkg(ps, lnkgId, lnkgNm, dbTypeCd, srvrAddr, srvrPortNo, dbNm, schmNm, lnkgExpln, acntId, enpswd,
                        useYn != null ? useYn : true, blankToDefault(lnkgSttsCd, "ACTIVE"),
                        blankToDefault(testRsltCd, "VALIDATING"), lastTestDt, actor);
                ps.executeUpdate();
            }
        } else if ("U".equals(op)) {
            String sql = """
                    update t_lnkg_profile set
                        lnkg_nm = coalesce(?, lnkg_nm),
                        db_type_cd = coalesce(?, db_type_cd),
                        srvr_addr = coalesce(?, srvr_addr),
                        srvr_port_no = coalesce(?, srvr_port_no),
                        db_nm = coalesce(?, db_nm),
                        schm_nm = coalesce(?, schm_nm),
                        lnkg_expln = coalesce(?, lnkg_expln),
                        acnt_id = coalesce(?, acnt_id),
                        enpswd = coalesce(?, enpswd),
                        use_yn = coalesce(?, use_yn),
                        lnkg_stts_cd = coalesce(?, lnkg_stts_cd),
                        test_rslt_cd = coalesce(?, test_rslt_cd),
                        last_test_dt = coalesce(?, last_test_dt),
                        mdfr_id = ?,
                        mdfcn_dt = current_timestamp
                    where lnkg_id = ?
                    """;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, lnkgNm);
                ps.setString(2, dbTypeCd);
                ps.setString(3, srvrAddr);
                ps.setObject(4, srvrPortNo);
                ps.setString(5, dbNm);
                ps.setString(6, schmNm);
                ps.setString(7, lnkgExpln);
                ps.setString(8, acntId);
                ps.setString(9, enpswd);
                ps.setObject(10, useYn);
                ps.setString(11, lnkgSttsCd);
                ps.setString(12, testRsltCd);
                ps.setTimestamp(13, lastTestDt);
                ps.setString(14, actor);
                ps.setString(15, lnkgId);
                ps.executeUpdate();
            }
        } else if ("D".equals(op)) {
            try (PreparedStatement ps = conn.prepareStatement("""
                    update t_lnkg_profile set
                        use_yn = false,
                        lnkg_stts_cd = 'DELETED',
                        mdfr_id = ?,
                        mdfcn_dt = current_timestamp
                    where lnkg_id = ?
                    """)) {
                ps.setString(1, actor);
                ps.setString(2, lnkgId);
                ps.executeUpdate();
            }
        }
        return lnkgId;
    }

    public static String spMigJob(
            Connection conn,
            String op,
            String migJobId,
            String srcLnkgId,
            String trgtLnkgId,
            String mtdtId,
            String srcSchmNm,
            String trgtSchmNm,
            Integer batchSz,
            Boolean dropExstYn,
            String jobSttsCd,
            Integer totTblCnt,
            Integer succTblCnt,
            Integer failTblCnt,
            Long totRowCnt,
            String errMsgCn,
            Timestamp bgngDt,
            Timestamp endDt,
            String actorId
    ) throws SQLException {
        String actor = blankToDefault(actorId, "system");
        if ("C".equals(op)) {
            if (isBlank(migJobId)) {
                migJobId = nextId(conn, "etl");
            }
            try (PreparedStatement ps = conn.prepareStatement("""
                    insert into t_mig_job (
                        mig_job_id, src_lnkg_id, trgt_lnkg_id, mtdt_id, src_schm_nm, trgt_schm_nm,
                        batch_sz, drop_exst_yn, job_stts_cd, tot_tbl_cnt, succ_tbl_cnt, fail_tbl_cnt, tot_row_cnt,
                        creatr_id, mdfr_id, crt_dt, mdfcn_dt
                    ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0, 0, ?, ?, current_timestamp, current_timestamp)
                    """)) {
                ps.setString(1, migJobId);
                ps.setString(2, srcLnkgId);
                ps.setString(3, trgtLnkgId);
                ps.setString(4, mtdtId);
                ps.setString(5, srcSchmNm);
                ps.setString(6, trgtSchmNm);
                ps.setInt(7, batchSz == null ? 500 : batchSz);
                ps.setBoolean(8, dropExstYn == null || dropExstYn);
                ps.setString(9, blankToDefault(jobSttsCd, "PENDING"));
                ps.setInt(10, totTblCnt == null ? 0 : totTblCnt);
                ps.setString(11, actor);
                ps.setString(12, actor);
                ps.executeUpdate();
            }
        } else if ("U".equals(op)) {
            try (PreparedStatement ps = conn.prepareStatement("""
                    update t_mig_job set
                        job_stts_cd = coalesce(?, job_stts_cd),
                        tot_tbl_cnt = coalesce(?, tot_tbl_cnt),
                        succ_tbl_cnt = coalesce(?, succ_tbl_cnt),
                        fail_tbl_cnt = coalesce(?, fail_tbl_cnt),
                        tot_row_cnt = coalesce(?, tot_row_cnt),
                        err_msg_cn = coalesce(?, err_msg_cn),
                        bgng_dt = coalesce(?, bgng_dt),
                        end_dt = coalesce(?, end_dt),
                        mdfr_id = ?,
                        mdfcn_dt = current_timestamp
                    where mig_job_id = ?
                    """)) {
                ps.setString(1, jobSttsCd);
                ps.setObject(2, totTblCnt);
                ps.setObject(3, succTblCnt);
                ps.setObject(4, failTblCnt);
                ps.setObject(5, totRowCnt);
                ps.setString(6, errMsgCn);
                ps.setTimestamp(7, bgngDt);
                ps.setTimestamp(8, endDt);
                ps.setString(9, actor);
                ps.setString(10, migJobId);
                ps.executeUpdate();
            }
        } else if ("D".equals(op)) {
            try (PreparedStatement ps = conn.prepareStatement("delete from t_mig_job where mig_job_id = ?")) {
                ps.setString(1, migJobId);
                ps.executeUpdate();
            }
        }
        return migJobId;
    }

    public static String spMigJobTbl(
            Connection conn,
            String op,
            String migJobTblId,
            String migJobId,
            String tblNm,
            String tblSttsCd,
            Long rowCnt,
            Integer batchCnt,
            String crtTblDdlCn,
            String errMsgCn,
            Timestamp bgngDt,
            Timestamp endDt,
            Integer sortNo
    ) throws SQLException {
        if ("C".equals(op)) {
            if (isBlank(migJobTblId)) {
                migJobTblId = nextId(conn, "migt");
            }
            try (PreparedStatement ps = conn.prepareStatement("""
                    insert into t_mig_job_tbl (
                        mig_job_tbl_id, mig_job_id, tbl_nm, tbl_stts_cd, row_cnt, batch_cnt,
                        crt_tbl_ddl_cn, err_msg_cn, bgng_dt, end_dt, sort_no, crt_dt, mdfcn_dt
                    ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                    """)) {
                ps.setString(1, migJobTblId);
                ps.setString(2, migJobId);
                ps.setString(3, tblNm);
                ps.setString(4, blankToDefault(tblSttsCd, "PENDING"));
                ps.setObject(5, rowCnt);
                ps.setObject(6, batchCnt);
                ps.setString(7, crtTblDdlCn);
                ps.setString(8, errMsgCn);
                ps.setTimestamp(9, bgngDt);
                ps.setTimestamp(10, endDt);
                ps.setInt(11, sortNo);
                ps.executeUpdate();
            }
        } else if ("U".equals(op)) {
            try (PreparedStatement ps = conn.prepareStatement("""
                    update t_mig_job_tbl set
                        tbl_stts_cd = coalesce(?, tbl_stts_cd),
                        row_cnt = coalesce(?, row_cnt),
                        batch_cnt = coalesce(?, batch_cnt),
                        crt_tbl_ddl_cn = coalesce(?, crt_tbl_ddl_cn),
                        err_msg_cn = coalesce(?, err_msg_cn),
                        bgng_dt = coalesce(?, bgng_dt),
                        end_dt = coalesce(?, end_dt),
                        mdfcn_dt = current_timestamp
                    where mig_job_tbl_id = ?
                    """)) {
                ps.setString(1, tblSttsCd);
                ps.setObject(2, rowCnt);
                ps.setObject(3, batchCnt);
                ps.setString(4, crtTblDdlCn);
                ps.setString(5, errMsgCn);
                ps.setTimestamp(6, bgngDt);
                ps.setTimestamp(7, endDt);
                ps.setString(8, migJobTblId);
                ps.executeUpdate();
            }
        } else if ("D".equals(op)) {
            try (PreparedStatement ps = conn.prepareStatement("delete from t_mig_job_tbl where mig_job_tbl_id = ?")) {
                ps.setString(1, migJobTblId);
                ps.executeUpdate();
            }
        }
        return migJobTblId;
    }

    public static void spMigJobTblResetFailed(Connection conn, String migJobId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                update t_mig_job_tbl set
                    tbl_stts_cd = 'PENDING',
                    row_cnt = null,
                    batch_cnt = null,
                    crt_tbl_ddl_cn = null,
                    err_msg_cn = null,
                    bgng_dt = null,
                    end_dt = null,
                    mdfcn_dt = current_timestamp
                where mig_job_id = ?
                  and tbl_stts_cd = 'FAILED'
                """)) {
            ps.setString(1, migJobId);
            ps.executeUpdate();
        }
    }

    public static String spExtrDmnd(
            Connection conn,
            String op,
            String extrDmndId,
            String dmndSrcCd,
            String extrDmndNm,
            String extrDmndSttsCd,
            Integer datstCnt,
            String actorId
    ) throws SQLException {
        String actor = blankToDefault(actorId, "system");
        if ("C".equals(op)) {
            if (isBlank(extrDmndId)) {
                extrDmndId = nextId(conn, "xdm");
            }
            try (PreparedStatement ps = conn.prepareStatement("""
                    insert into t_extr_dmnd (
                        extr_dmnd_id, dmnd_src_cd, extr_form_id, extr_dmnd_nm, extr_dmnd_stts_cd,
                        otsd_dmnd_id, datst_cnt, creatr_id, mdfr_id, crt_dt, mdfcn_dt
                    ) values (?, ?, null, ?, ?, null, ?, ?, ?, current_timestamp, current_timestamp)
                    """)) {
                ps.setString(1, extrDmndId);
                ps.setString(2, blankToDefault(dmndSrcCd, "EXTERNAL_API"));
                ps.setString(3, blankToDefault(extrDmndNm, extrDmndId));
                ps.setString(4, blankToDefault(extrDmndSttsCd, "READY"));
                ps.setInt(5, datstCnt == null ? 1 : datstCnt);
                ps.setString(6, actor);
                ps.setString(7, actor);
                ps.executeUpdate();
            }
        } else if ("U".equals(op)) {
            try (PreparedStatement ps = conn.prepareStatement("""
                    update t_extr_dmnd set
                        extr_dmnd_stts_cd = coalesce(?, extr_dmnd_stts_cd),
                        mdfr_id = ?,
                        mdfcn_dt = current_timestamp
                    where extr_dmnd_id = ?
                    """)) {
                ps.setString(1, extrDmndSttsCd);
                ps.setString(2, actor);
                ps.setString(3, extrDmndId);
                ps.executeUpdate();
            }
        } else if ("D".equals(op)) {
            try (PreparedStatement ps = conn.prepareStatement("delete from t_extr_dmnd where extr_dmnd_id = ?")) {
                ps.setString(1, extrDmndId);
                ps.executeUpdate();
            }
        }
        return extrDmndId;
    }

    public static String spExtrDatst(
            Connection conn,
            String op,
            String extrDatstId,
            String extrDmndId,
            String extrSpcfId,
            String datstNm,
            String extrDatstSttsCd,
            Integer sortNo,
            String mnfstCn,
            String actorId
    ) throws SQLException {
        String actor = blankToDefault(actorId, "system");
        if ("C".equals(op)) {
            if (isBlank(extrDatstId)) {
                extrDatstId = nextId(conn, "datst");
            }
            try (PreparedStatement ps = conn.prepareStatement("""
                    insert into t_extr_datst (
                        extr_datst_id, extr_dmnd_id, extr_spcf_id, datst_nm, extr_datst_stts_cd,
                        sort_no, mnfst_cn, creatr_id, mdfr_id, crt_dt, mdfcn_dt
                    ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                    """)) {
                ps.setString(1, extrDatstId);
                ps.setString(2, extrDmndId);
                ps.setString(3, extrSpcfId);
                ps.setString(4, blankToDefault(datstNm, extrDatstId));
                ps.setString(5, blankToDefault(extrDatstSttsCd, "READY"));
                ps.setInt(6, sortNo == null ? 1 : sortNo);
                ps.setString(7, mnfstCn);
                ps.setString(8, actor);
                ps.setString(9, actor);
                ps.executeUpdate();
            }
        } else if ("U".equals(op)) {
            try (PreparedStatement ps = conn.prepareStatement("""
                    update t_extr_datst set
                        extr_datst_stts_cd = coalesce(?, extr_datst_stts_cd),
                        mnfst_cn = coalesce(?, mnfst_cn),
                        mdfr_id = ?,
                        mdfcn_dt = current_timestamp
                    where extr_datst_id = ?
                    """)) {
                ps.setString(1, extrDatstSttsCd);
                ps.setString(2, mnfstCn);
                ps.setString(3, actor);
                ps.setString(4, extrDatstId);
                ps.executeUpdate();
            }
        } else if ("D".equals(op)) {
            try (PreparedStatement ps = conn.prepareStatement("delete from t_extr_datst where extr_datst_id = ?")) {
                ps.setString(1, extrDatstId);
                ps.executeUpdate();
            }
        }
        return extrDatstId;
    }

    public static String spExtrExcn(
            Connection conn,
            String op,
            String extrExcnId,
            String extrDatstId,
            String excnTypeCd,
            String excnSttsCd,
            Long rsltNocs,
            String rsltStrgTypeCd,
            String rsltTblNm,
            String rsltFilePath,
            String failCn,
            String excnReqCn,
            String actorId
    ) throws SQLException {
        String actor = blankToDefault(actorId, "system");
        if ("C".equals(op)) {
            if (isBlank(extrExcnId)) {
                extrExcnId = nextId(conn, "xex");
            }
            int excnSn;
            try (PreparedStatement ps = conn.prepareStatement(
                    "select coalesce(max(excn_sn), 0) + 1 from t_extr_excn where extr_datst_id = ?")) {
                ps.setString(1, extrDatstId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    excnSn = rs.getInt(1);
                }
            }
            Timestamp endDt = "SUCCESS".equals(excnSttsCd) || "FAILED".equals(excnSttsCd) || "CANCELLED".equals(excnSttsCd)
                    ? new Timestamp(System.currentTimeMillis()) : null;
            try (PreparedStatement ps = conn.prepareStatement("""
                    insert into t_extr_excn (
                        extr_excn_id, extr_datst_id, excn_sn, excn_type_cd, excn_stts_cd,
                        extr_job_id, excn_bgng_dt, excn_end_dt, rslt_nocs, rslt_strg_type_cd,
                        rslt_tbl_nm, rslt_file_path, fail_cn, checkpoint_state, resumed_from_checkpoint,
                        resume_message, excn_sql_cn, excn_sql_vrbl_cn, excn_req_cn,
                        creatr_id, mdfr_id, crt_dt, mdfcn_dt
                    ) values (?, ?, ?, ?, ?, null, current_timestamp, ?, ?, ?, ?, ?, ?, null, false, null, null, null, ?, ?, ?, current_timestamp, current_timestamp)
                    """)) {
                ps.setString(1, extrExcnId);
                ps.setString(2, extrDatstId);
                ps.setInt(3, excnSn);
                ps.setString(4, excnTypeCd);
                ps.setString(5, blankToDefault(excnSttsCd, "RUNNING"));
                ps.setTimestamp(6, endDt);
                ps.setObject(7, rsltNocs);
                ps.setString(8, rsltStrgTypeCd);
                ps.setString(9, rsltTblNm);
                ps.setString(10, rsltFilePath);
                ps.setString(11, failCn);
                ps.setString(12, excnReqCn);
                ps.setString(13, actor);
                ps.setString(14, actor);
                ps.executeUpdate();
            }
        } else if ("D".equals(op)) {
            try (PreparedStatement ps = conn.prepareStatement("delete from t_extr_excn where extr_excn_id = ?")) {
                ps.setString(1, extrExcnId);
                ps.executeUpdate();
            }
        }
        return extrExcnId;
    }

    private static void bindLnkg(
            PreparedStatement ps,
            String lnkgId,
            String lnkgNm,
            String dbTypeCd,
            String srvrAddr,
            Integer srvrPortNo,
            String dbNm,
            String schmNm,
            String lnkgExpln,
            String acntId,
            String enpswd,
            boolean useYn,
            String lnkgSttsCd,
            String testRsltCd,
            Timestamp lastTestDt,
            String actor
    ) throws SQLException {
        ps.setString(1, lnkgId);
        ps.setString(2, lnkgNm);
        ps.setString(3, dbTypeCd);
        ps.setString(4, srvrAddr);
        ps.setInt(5, srvrPortNo);
        ps.setString(6, dbNm);
        ps.setString(7, schmNm);
        ps.setString(8, lnkgExpln);
        ps.setString(9, acntId);
        ps.setString(10, enpswd);
        ps.setBoolean(11, useYn);
        ps.setString(12, lnkgSttsCd);
        ps.setString(13, testRsltCd);
        ps.setTimestamp(14, lastTestDt);
        ps.setString(15, actor);
        ps.setString(16, actor);
    }

    private static String nextId(Connection conn, String prefix) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("select fn_next_etl_id(?)")) {
            ps.setString(1, prefix);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String blankToDefault(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }
}
