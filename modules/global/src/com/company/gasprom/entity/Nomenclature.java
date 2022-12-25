package com.company.gasprom.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;


@Table(name = "GAZPROM_NOMENCLATURE")
@Entity(name = "gazprom_Nomenclature")
@NamePattern("%s|fullName")
public class Nomenclature extends StandardEntity {
    private static final long serialVersionUID = 8471712155033791510L;

    @Column
    protected String nomenclatureType;

    @Column
    protected String name;

    @Lob
    @Column
    protected String fullName;

    @Column
    protected String unit;

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getNomenclatureType() {
        return nomenclatureType;
    }

    public void setNomenclatureType(String nomenclatureType) {
        this.nomenclatureType = nomenclatureType;
    }


}
