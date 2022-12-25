package com.company.gasprom.service;

import com.company.gasprom.entity.Nomenclature;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Transaction;
import com.haulmont.cuba.core.global.DataManager;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service(NomenclatureService.NAME)
public class NomenclatureServiceBean implements NomenclatureService {
    private final DataManager dataManager;
    @Inject
    private Persistence persistence;


    public NomenclatureServiceBean(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Override
    public Set<String> createAndSaveNomenclaturesFromExcel(List<List<String>> data) {


        Set<String> errors = new HashSet<>();

        data.parallelStream().filter(nomenclatureAttr -> {
            int size = nomenclatureAttr.size();
            if (size != 5) {
                errors.add(nomenclatureAttr.get(0));
                return false;
            } else {
                return true;
            }
        }).forEach(nomenclatureAttr -> {
            Nomenclature newNomenclature;

            try (Transaction transaction = persistence.createTransaction()) {
                EntityManager entityManager = persistence.getEntityManager();
                UUID uuid;
                try {
                    uuid = UUID.fromString(nomenclatureAttr.get(0));
                } catch (IllegalArgumentException e) {
                    errors.add(nomenclatureAttr.get(0));
                    return;
                }
                Nomenclature nomenclature = entityManager.find(Nomenclature.class, uuid);

                if (nomenclature == null) {
                    newNomenclature = dataManager.create(Nomenclature.class);
                    newNomenclature.setUuid(UUID.fromString(nomenclatureAttr.get(0)));
                } else {
                    newNomenclature = nomenclature;
                }
                newNomenclature.setNomenclatureType(nomenclatureAttr.get(1));
                newNomenclature.setName(nomenclatureAttr.get(2));
                newNomenclature.setFullName(nomenclatureAttr.get(3));
                newNomenclature.setUnit(nomenclatureAttr.get(4));

                if (nomenclature == null)
                    entityManager.persist(newNomenclature);

                transaction.commit();
            }
        });

        data.clear();
        return errors;
    }

}