package com.company.gasprom.web.screens.nomenclature;

import com.haulmont.cuba.gui.screen.*;
import com.company.gasprom.entity.Nomenclature;

@UiController("gazprom_Nomenclature.edit")
@UiDescriptor("nomenclature-edit.xml")
@EditedEntityContainer("nomenclatureDc")
@LoadDataBeforeShow
public class NomenclatureEdit extends StandardEditor<Nomenclature> {
}