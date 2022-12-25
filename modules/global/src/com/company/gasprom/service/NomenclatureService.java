package com.company.gasprom.service;

import java.util.List;
import java.util.Set;

public interface NomenclatureService {
    String NAME = "gasprom_NomenclatureService";

    Set<String> createAndSaveNomenclaturesFromExcel(List<List<String>> lists);

}