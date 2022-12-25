-- begin GAZPROM_NOMENCLATURE
create table GAZPROM_NOMENCLATURE (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    NOMENCLATURETYPE varchar(255),
    NAME varchar(255),
    FULLNAME text,
    UNIT varchar(255),
    --
    primary key (ID)
)^
-- end GAZPROM_NOMENCLATURE
