package com.company.gasprom.web.screens.nomenclature;

import com.company.gasprom.entity.Nomenclature;
import com.company.gasprom.service.NomenclatureService;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.FileUploadField;
import com.haulmont.cuba.gui.executors.BackgroundTask;
import com.haulmont.cuba.gui.executors.BackgroundTaskHandler;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import com.haulmont.cuba.gui.executors.TaskLifeCycle;
import com.haulmont.cuba.gui.screen.*;
import org.apache.poi.ooxml.util.SAXHelper;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;


@UiController("gazprom_Nomenclature.browse")
@UiDescriptor("nomenclature-browse.xml")
@LookupComponent("nomenclaturesTable")
@LoadDataBeforeShow
public class NomenclatureBrowse extends StandardLookup<Nomenclature> {
    @Inject
    private FileUploadField uploadFile;
    @Inject
    private NomenclatureService nomenclatureService;
    @Inject
    private Notifications notifications;
    @Inject
    private Messages messages;
    @Inject
    private BackgroundWorker backgroundWorker;

    private final List<List<String>> readyData = new ArrayList<>();
    @Inject
    private Dialogs dialogs;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        uploadFile.setContextHelpIconClickHandler(contextHelpIconClickEvent ->
                dialogs.createMessageDialog()
                        .withCaption("Help")
                        .withMessage(messages.getMainMessage("importExcelHelpText"))
                        .withType(Dialogs.MessageType.CONFIRMATION)
                        .show()
        );
    }



    @Subscribe("uploadFile")
    public void onUploadFileFileUploadSucceed(FileUploadField.FileUploadSucceedEvent event) throws Exception {

        boolean isFileNameCorrect = checkFileName(uploadFile.getFileName());
        if (!isFileNameCorrect) {
            showIncorrectFileExtensionNotification();
            return;
        }
        SaxEventUserModel saxEventUserModel = new SaxEventUserModel();
        try {
            saxEventUserModel.processSheets(uploadFile.getFileContent());
        } catch (SAXException e) {
            showIncorrectHeaderNotification();
            return;
        }
        if (readyData.size() == 0) {
            showEmptyDataRowsNotification();
            return;

        }
        Set<String> errCountWhileSavingData = null;
        BackgroundTaskHandler<Void> taskHandler = backgroundWorker.handle(new ImportTask(errCountWhileSavingData));
        taskHandler.execute();


    }

    private void showIsSavedNotification() {
        notifications.create()
                .withCaption(messages.getMainMessage("isSavedCaption"))
                .withType(Notifications.NotificationType.HUMANIZED)
                .show();
    }

    private void showIsSavedWithWarningsNotification(Set<String> ids) {
        StringBuilder resultDescription = new StringBuilder(messages.getMainMessage("isSavedWithWarningsDescription")).append("\n");
        for (String id : ids) {
            resultDescription.append(id).append("\n");
        }
        notifications.create()
                .withCaption(messages.getMainMessage("isSavedWithWarningsCaption"))
                .withDescription(resultDescription.toString())
                .withType(Notifications.NotificationType.WARNING)
                .show();
    }

    private void showEmptyDataRowsNotification() {
        notifications.create()
                .withCaption(messages.getMainMessage("emptyDataRowsCaption"))
                .withDescription(messages.getMainMessage("emptyDataRowsDescription"))
                .withType(Notifications.NotificationType.ERROR)
                .show();
    }

    private void showIncorrectHeaderNotification() {
        notifications.create()
                .withCaption(messages.getMainMessage("incorrectHeaderCaption"))
                .withDescription(messages.getMainMessage("incorrectHeaderDescription"))
                .withType(Notifications.NotificationType.ERROR)
                .show();
    }

    private void showIncorrectFileExtensionNotification() {
        notifications.create()
                .withCaption(messages.getMainMessage("incorrectFileExtensionCaption"))
                .withDescription(messages.getMainMessage("incorrectFileExtensionDescription"))
                .withType(Notifications.NotificationType.ERROR)
                .show();
    }

    private boolean checkFileName(String fileName) {
        return fileName.endsWith(".xlsx");
    }


    public class SaxEventUserModel {
        public void processSheets(InputStream filename) throws Exception {
            readyData.clear();
            try (OPCPackage pkg = OPCPackage.open(filename);) {
                XSSFReader r = new XSSFReader(pkg);
                SharedStringsTable sst = r.getSharedStringsTable();
                XMLReader parser = fetchSheetParser(sst);
                Iterator<InputStream> sheets = r.getSheetsData();
                try (InputStream sheet = sheets.next()) {
                    InputSource sheetSource = new InputSource(sheet);
                    parser.parse(sheetSource);
                }
                sst.close();
                pkg.clearRelationships();
            }

        }

        public XMLReader fetchSheetParser(SharedStringsTable sst) throws SAXException, ParserConfigurationException {
            XMLReader sheetParser = SAXHelper.newXMLReader();
            ContentHandler handler = new SheetHandler(sst);
            sheetParser.setContentHandler(handler);
            return sheetParser;
        }

        private class SheetHandler extends DefaultHandler {
            private SharedStringsTable sst;
            private String lastContents;
            private Integer rowInd = 0;
            private boolean nextIsString;
            private final List<String> excelHeader = new ArrayList<>();

            private boolean isHeaderCorrect = false;


            private final List<String> correctHeader = Arrays.asList("Идентификатор"
                    , "Группа 1 уровня"
                    , "Наименование (без бренда,без модели,без наименования производителя)"
                    , "Наименование полное (обязательно с моделью,брендом)"
                    , "Единица измерения");

            private SheetHandler(SharedStringsTable sst) {
                this.sst = sst;
            }


            @Override
            public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {

                if (name.equals("c")) {
                    rowInd = Integer.parseInt(attributes.getValue("r").substring(1));
                    String cellType = attributes.getValue("t");
                    if (cellType != null && cellType.equals("s")) {
                        nextIsString = true;
                    } else {
                        nextIsString = false;
                    }
                }

                lastContents = "";
            }

            @Override
            public void endElement(String uri, String localName, String name) throws SAXException {

                if (!isHeaderCorrect && rowInd > 1) {
                    isHeaderCorrect = excelHeader.equals(correctHeader);
                }

                if (nextIsString) {
                    int idx = Integer.parseInt(lastContents);
                    lastContents = sst.getItemAt(idx).getString();
                    nextIsString = false;

                }

                if (name.equals("v")) {
                    if (rowInd <= 1) {
                        excelHeader.add(lastContents);
                        return;
                    }

                    if (!isHeaderCorrect)
                        throw new SAXParseException("Incorrect header", null);

                    if (readyData.size() == rowInd - 2)
                        readyData.add(new ArrayList<>());
                    readyData.get(rowInd - 2).add(lastContents);
                }

            }

            @Override
            public void characters(char[] ch, int start, int length) {
                lastContents += new String(ch, start, length);
            }
        }
    }

    private class ImportTask extends BackgroundTask<Integer, Void> {

        private Set<String> errCountWhileSavingData;

        protected ImportTask(Set<String> errCountWhileSavingData) {
            super(10, TimeUnit.MINUTES);
            this.errCountWhileSavingData = errCountWhileSavingData;
        }

        @Override
        public Void run(TaskLifeCycle<Integer> taskLifeCycle) throws Exception {
            errCountWhileSavingData = nomenclatureService.createAndSaveNomenclaturesFromExcel(readyData);
            return null;
        }

        @Override
        public void done(Void result) {
            super.done(result);
            if (errCountWhileSavingData.size() > 0) {
                showIsSavedWithWarningsNotification(errCountWhileSavingData);
            } else {
                showIsSavedNotification();
            }
        }
    }
}