CREATE TABLE IF NOT EXISTS job_definition (
                                              id        VARCHAR(40) PRIMARY KEY,
    name      VARCHAR(255) NOT NULL,
    grp       VARCHAR(255) NOT NULL,
    job_type  VARCHAR(255),
    payload   TEXT
    );
CREATE INDEX IF NOT EXISTS idx_job_definition_name_grp ON job_definition(name, grp);

CREATE TABLE IF NOT EXISTS job_execution (
                                             id           BIGSERIAL PRIMARY KEY,
                                             job_id       VARCHAR(40) NOT NULL,
    started_at   TIMESTAMPTZ,
    finished_at  TIMESTAMPTZ,
    outcome      VARCHAR(32),
    message      TEXT
    );
CREATE INDEX IF NOT EXISTS idx_job_execution_job_id_started ON job_execution(job_id, started_at DESC);
